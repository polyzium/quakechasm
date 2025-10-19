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

import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import ru.darkchronics.quake.QuakePlugin;
import ru.darkchronics.quake.game.combat.WeaponUserState;
import ru.darkchronics.quake.game.entities.QEntityUtil;
import ru.darkchronics.quake.hud.Hud;
import ru.darkchronics.quake.misc.TranslationManager;

public class AmmoSpawner extends Spawner {
    public static final int[] AMOUNTS = {
            50, // bullets
            10, // shells
            5, // rockets
            60, // battery
            10, // slugs
            30, // cells
            1 // bfg charge
    };
    public static final String[] NAMES = {
            "PICKUP_AMMO_BULLETS",
            "PICKUP_AMMO_SHELLS",
            "PICKUP_AMMO_ROCKETS",
            "PICKUP_AMMO_BATTERY",
            "PICKUP_AMMO_SLUGS",
            "PICKUP_AMMO_CELLS",
            "PICKUP_AMMO_BFG",
    };
    public static final String[] ALIASES = {
            "bullets",
            "shells",
            "rockets",
            "battery",
            "slugs",
            "cells",
            "bfg"
    };
    private ItemStack itemForRespawn;
    private BukkitTask respawnTask;
    private int ammoType;
    public AmmoSpawner(int ammoType, World world, Location location) {
        super(new ItemStack(Material.GUNPOWDER), world, location);

        ItemStack displayItem = new ItemStack(Material.GUNPOWDER);
        ItemMeta displayItemMeta = displayItem.getItemMeta();
        displayItemMeta.setCustomModelData(ammoType);
        displayItem.setItemMeta(displayItemMeta);

        this.ammoType = ammoType;
        PersistentDataContainer displayData = display.getPersistentDataContainer();
        NamespacedKey ammoTypeKey = new NamespacedKey(QuakePlugin.INSTANCE, "ammoType");
        displayData.set(ammoTypeKey, PersistentDataType.INTEGER, this.ammoType);

        super.display.setItemStack(displayItem);
        QEntityUtil.setEntityType(super.display, "ammo_spawner");

        QuakePlugin.INSTANCE.triggers.add(this);
    }

    public AmmoSpawner(ItemDisplay display) {
        super(display);

        PersistentDataContainer displayData = display.getPersistentDataContainer();
        NamespacedKey ammoTypeKey = new NamespacedKey(QuakePlugin.INSTANCE, "ammoType");
        this.ammoType = displayData.get(ammoTypeKey, PersistentDataType.INTEGER);

        QuakePlugin.INSTANCE.triggers.add(this);
    }

    @Override
    public void onPickup(Player player) {
        WeaponUserState weaponState = QuakePlugin.INSTANCE.userStates.get(player).weaponState;
        if (this.display.getItemStack().isEmpty() || weaponState.ammo[ammoType] >= 200) return;

        this.itemForRespawn = this.display.getItemStack();
        weaponState.ammo[ammoType] += AMOUNTS[ammoType];
        if (weaponState.ammo[ammoType] > 200)
            weaponState.ammo[ammoType] = 200;

        this.display.setItemStack(new ItemStack(Material.AIR)); // Make invisible
        player.getWorld().playSound(player, "quake.items.ammo.pickup", 0.5f, 1f);
        Hud.pickupMessage(player, Component.text(TranslationManager.t(NAMES[ammoType], player)));

        // Respawn in 40 seconds
        this.respawnTask = new BukkitRunnable() {
            public void run() {
                respawn();
            }
        }.runTaskLater(QuakePlugin.INSTANCE, 20*40);
    }

    @Override
    public void respawn() {
        if (!super.display.getItemStack().isEmpty()) return;

        display.setItemStack(this.itemForRespawn);
        display.getWorld().spawnParticle(Particle.INSTANT_EFFECT, display.getLocation(), 16, 0.5, 0.5, 0.5);
        display.getWorld().playSound(display, "quake.items.respawn", 0.5f, 1f);

        if (this.respawnTask != null)
            this.respawnTask.cancel();
    }
}
