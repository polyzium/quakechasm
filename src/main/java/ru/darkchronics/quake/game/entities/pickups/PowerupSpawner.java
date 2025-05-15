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

package ru.darkchronics.quake.game.entities.pickups;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import ru.darkchronics.quake.QuakePlugin;
import ru.darkchronics.quake.QuakeUserState;
import ru.darkchronics.quake.game.combat.powerup.Powerup;
import ru.darkchronics.quake.game.combat.powerup.PowerupType;
import ru.darkchronics.quake.game.entities.QEntityUtil;
import ru.darkchronics.quake.hud.Hud;
import ru.darkchronics.quake.matchmaking.matches.Match;
import ru.darkchronics.quake.misc.TranslationManager;

public class PowerupSpawner extends Spawner {
    private static final int RESPAWN_TIME = 20*60;
    private ItemStack itemForRespawn;
    private BukkitTask respawnTask;
    private PowerupType type;
    private boolean isDrop;
    private int duration;
    private Match belongingMatch;
    public PowerupSpawner(PowerupType type, World world, Location location, boolean isDrop, int duration) {
        super(new ItemStack(Material.TOTEM_OF_UNDYING), world, location);

        ItemStack powerupItem = this.display.getItemStack();
        ItemMeta itemMeta = powerupItem.getItemMeta();
        itemMeta.setCustomModelData(type.ordinal());
        powerupItem.setItemMeta(itemMeta);
        this.display.setItemStack(powerupItem);

        this.type = type;
        this.isDrop = isDrop;
        this.duration = duration;

        PersistentDataContainer displayData = display.getPersistentDataContainer();
        NamespacedKey typeKey = new NamespacedKey(QuakePlugin.INSTANCE, "type");
        displayData.set(typeKey, PersistentDataType.STRING, this.type.toString());

        if (!isDrop)
            QEntityUtil.setEntityType(super.display, "powerup_spawner");
    }

    public PowerupSpawner(ItemDisplay display) {
        super(display);

        PersistentDataContainer displayData = display.getPersistentDataContainer();
        NamespacedKey typeKey = new NamespacedKey(QuakePlugin.INSTANCE, "type");
        this.type = PowerupType.valueOf(displayData.get(typeKey, PersistentDataType.STRING));
        this.isDrop = false;
        this.duration = 30;

        ItemStack powerupItem = this.display.getItemStack();
        ItemMeta itemMeta = powerupItem.getItemMeta();
        itemMeta.setCustomModelData(type.ordinal());
        powerupItem.setItemMeta(itemMeta);
        this.display.setItemStack(powerupItem);

        QuakePlugin.INSTANCE.triggers.add(this);
    }

    public static void doPowerup(Player player, PowerupType type, int time) {
        boolean found = false;
        Powerup powerup2;

        QuakeUserState state = QuakePlugin.INSTANCE.userStates.get(player);
        for (Powerup powerup : state.activePowerups) {
            if (powerup.getType() == type) {
                powerup.extendDuration(time);

                found = true;
                break;
            }
        }
        if (!found) {
            powerup2 = new Powerup(player, type, time);
            state.activePowerups.add(powerup2);
        }
    }

    @Override
    public void onPickup(Player player) {
        QuakeUserState userState = QuakePlugin.INSTANCE.userStates.get(player);

        ItemStack item = super.display.getItemStack();
        if (item.isEmpty()) return;
        this.itemForRespawn = item;

        doPowerup(player, this.type, this.duration);

        this.despawn(userState.currentMatch);
        if (belongingMatch != null) {
            for (Player mPlayer : belongingMatch.getPlayers()) {
                mPlayer.playSound(Sound.sound(Key.key(Powerup.SOUNDS.get(type)), Sound.Source.NEUTRAL, 0.5f, 1f), mPlayer);
            }
        } else
            player.getWorld().playSound(player, Powerup.SOUNDS.get(type), 0.5f, 1f);

        Hud.pickupMessage(player, Component.text(TranslationManager.t(Powerup.NAMES.get(type), player)));

        if (!this.isDrop)
            // Respawn in 1 minute
            this.respawnTask = new BukkitRunnable() {
            public void run() {
                respawn();
            }
        }.runTaskLater(QuakePlugin.INSTANCE, RESPAWN_TIME);
        else
            // Despawn
            this.display.remove();
    }

    public void respawn() {
        if (!super.display.getItemStack().isEmpty() || this.isDrop) return;

        display.setItemStack(this.itemForRespawn);
        display.getWorld().spawnParticle(Particle.SPELL_INSTANT, display.getLocation(), 16, 0.5, 0.5, 0.5);
        if (belongingMatch != null) {
            for (Player mPlayer : belongingMatch.getPlayers()) {
                mPlayer.playSound(Sound.sound(Key.key("quake.items.powerups.respawn"), Sound.Source.NEUTRAL, 0.5f, 1f), mPlayer);
            }
        } else
            display.getWorld().playSound(display, "quake.items.powerups.respawn", 0.5f, 1f);

        if (!this.isDrop && this.respawnTask != null)
            this.respawnTask.cancel();
    }

    public void despawn(Match match) {
        ItemStack item = super.display.getItemStack();
        if (item.isEmpty()) return;
        this.itemForRespawn = item;

        this.belongingMatch = match;

        this.display.setItemStack(new ItemStack(Material.AIR)); // Make invisible
        this.respawnTask = new BukkitRunnable() {
            public void run() {
                respawn();
            }
        }.runTaskLater(QuakePlugin.INSTANCE, RESPAWN_TIME);
    }

    public void matchCleanup() {
        this.belongingMatch = null;
        if (this.isDrop) {
            QuakePlugin.INSTANCE.triggers.remove(this);
            this.remove();
        } else {
            this.respawn();
        }
    }
}
