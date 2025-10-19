/*
 * Quakechasm, a Quake minigame plugin for Minecraft servers running PaperMC
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

package com.github.polyzium.quakechasm.hud;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.entity.Player;
import com.github.polyzium.quakechasm.QuakeUserState;
import com.github.polyzium.quakechasm.game.combat.WeaponType;
import com.github.polyzium.quakechasm.game.combat.WeaponUtil;

import java.time.Duration;
import java.util.ArrayList;

public class Hud {
    public PowerupBoard powerupBoard;
    private int ticksPassed = 0;
    private boolean lowHealthFlashing;

    public Hud(QuakeUserState state) {
        this.powerupBoard = new PowerupBoard(state);
    }

    public static void pickupMessage(Player player, Component text) {
        player.showTitle(
                Title.title(Component.empty(),
                        text,
                        Title.Times.times(Duration.ZERO, Duration.ZERO, Duration.ofSeconds(1))
                )
        );
    }

    public void draw(QuakeUserState state) {
        Player player = state.getPlayer();

        // Low health flashing logic
        if (ticksPassed >= 4) {
            lowHealthFlashing = !lowHealthFlashing;
            ticksPassed = 0;
        }
        ticksPassed++;

        // Status bar (ammo, health, armor)
        ArrayList<Component> components = new ArrayList<>(6);
        // Ammo
        TextColor ammoColor = TextColor.color(0xffaf00);
        int heldWeapon = WeaponUtil.getHoldingWeaponIndex(player);
        if (
                heldWeapon != -1 &&
                !(heldWeapon == WeaponType.MACHINEGUN || heldWeapon == WeaponType.LIGHTNING_GUN || heldWeapon == WeaponType.PLASMA_GUN) &&
                        state.weaponState.cooldowns[heldWeapon] != 0
        ) {
            float cooldownFactor = ((float) state.weaponState.cooldowns[heldWeapon] / WeaponUtil.PERIODS[heldWeapon]*0.75f)+0.25f;
            ammoColor = TextColor.color(cooldownFactor*0.5f, cooldownFactor*0.5f, cooldownFactor*0.5f);
        }
        if (heldWeapon != -1)
            components.add(
                    Component.text((char) (Icons.WEAPONS_OFFSET+ heldWeapon)).append(
                        Component.text(String.format("%-3d", state.weaponState.ammo[heldWeapon])).color(TextColor.color(ammoColor))
                    )
            );
        else
            components.add(Component.text("    "));

        // Health
        TextColor healthColor = TextColor.color(0xffaf00);
        double health = player.getHealth();
        if (health > 20)
            healthColor = TextColor.color(0xffffff);
        else if (health <= 5 && lowHealthFlashing)
            healthColor = TextColor.color(0xff3f3f);
        components.add(
                Component.text(Icons.HEALTH).append(
                        Component.text(String.format("%-3d", (int) Math.round(health *5) )).color(healthColor)
                )
        );
        // Armor
        components.add(
                Component.text(Icons.ARMOR).append(
                        Component.text(String.format("%-3d", state.armor)).color(TextColor.color(0xffaf00))
                )
        );

        // Send to client
        player.sendActionBar(
            Component.join(JoinConfiguration.separator(Component.text(" ")), components).font(Key.key("hud"))
        );

        // Call others (powerups, etc)
        this.powerupBoard.draw();
    }
}
