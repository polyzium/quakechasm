/*
 * DarkChronics-Quake, a Quake minigame plugin for Minecraft servers running PaperMC
 * 
 * Copyright (C) 2024-present Polyzium
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package ru.darkchronics.quake.game.combat.powerup;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.SoundCategory;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.intellij.lang.annotations.Subst;
import org.jetbrains.annotations.NotNull;
import ru.darkchronics.quake.QuakePlugin;
import ru.darkchronics.quake.QuakeUserState;
import ru.darkchronics.quake.game.entities.pickups.PowerupSpawner;

import java.util.ArrayList;
import java.util.EnumMap;

public class Powerup {
    public static EnumMap<PowerupType, String> NAMES = new EnumMap<>(PowerupType.class);
    static {
        NAMES.put(PowerupType.QUAD_DAMAGE, "PICKUP_POWERUP_QUAD");
        NAMES.put(PowerupType.REGENERATION, "PICKUP_POWERUP_REGENERATION");
        NAMES.put(PowerupType.PROTECTION, "PICKUP_POWERUP_PROTECTION");
    }

    public static EnumMap<PowerupType, String> SOUNDS = new EnumMap<>(PowerupType.class);
    static {
        SOUNDS.put(PowerupType.QUAD_DAMAGE, "quake.items.powerups.quad_damage.pickup");
        SOUNDS.put(PowerupType.REGENERATION, "quake.items.powerups.regeneration.pickup");
        SOUNDS.put(PowerupType.PROTECTION, "quake.items.powerups.protection.pickup");
    }

    private int time;
    private PowerupType type;
    public BukkitTask timer;
    public Powerup(Player player, PowerupType type, int time) {
        this.type = type;
        this.time = time;

        QuakeUserState state = QuakePlugin.INSTANCE.userStates.get(player);
        Powerup self = this;
        this.timer = new BukkitRunnable() {
            @Override
            public void run() {
                self.time -= 1;
                if (self.time < 0) {
                    state.activePowerups.remove(self);

                    if (type == PowerupType.REGENERATION)
                        state.startHealthDecreaser();

                    state.hud.powerupBoard.update();
                    cancel();
                    return;
                }
                if (self.time < 5 && self.time >= 0)
                    player.playSound(player, "quake.items.powerups.wearoff", SoundCategory.NEUTRAL, 0.5f, 1);

                // Regenerate health
                if (type == PowerupType.REGENERATION && player.getHealth() < 40) {
                    float newHealth = Math.round(player.getHealth()*5)/5f;
                    double maxHealth = 0;
                    if (newHealth < 20) {
                        newHealth += 3;
                        if (newHealth > 20)
                            maxHealth = newHealth;
                        else
                            maxHealth = 20;
                    } else if (newHealth < 40) {
                        newHealth += 1;
                        maxHealth = newHealth;
                    }

                    if (newHealth > 40) {
                        newHealth = 40;
                        maxHealth = 40;
                    }

                    player.getWorld().playSound(player, "quake.items.powerups.regeneration.heal", 0.5f, 1f);
                    player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(maxHealth);
                    player.setHealth(newHealth);
                }

                state.hud.powerupBoard.update();
            }
        }.runTaskTimer(QuakePlugin.INSTANCE, 0, 20);
    }

    public PowerupType getType() {
        return type;
    }

    public int getTime() {
        return time;
    }

    public void extendDuration(int time) {
        this.time += time;
    }

    public static boolean hasPowerup(Player player, PowerupType type) {
        QuakeUserState state = QuakePlugin.INSTANCE.userStates.get(player);
        for (Powerup powerup : state.activePowerups)
            if (powerup.getType() == type) return true;

        return false;
    }

    public static void dropPowerups(Player player) {
        QuakeUserState state = QuakePlugin.INSTANCE.userStates.get(player);

        for (int i = 0; i < state.activePowerups.size(); i++) {
            Powerup powerup = state.activePowerups.get(i);
//        for (Powerup powerup : state.activePowerups) {
            Location playerLocation = player.getLocation().clone();
            playerLocation.setY(playerLocation.getY()+1);

            if (playerLocation.y() > -64)
                new PowerupSpawner(powerup.type, player.getWorld(), playerLocation, true, powerup.time);

            powerup.timer.cancel();
            state.hud.powerupBoard.update();
            state.activePowerups.remove(powerup);
        }
    }
}
