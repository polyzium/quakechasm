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
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import ru.darkchronics.quake.QuakePlugin;
import ru.darkchronics.quake.QuakeUserState;
import ru.darkchronics.quake.game.entities.QEntityUtil;
import ru.darkchronics.quake.hud.Hud;
import ru.darkchronics.quake.misc.TranslationManager;

import java.util.HashMap;
import java.util.Map;

public class ArmorSpawner extends Spawner {
    public static final Map<Integer, String> NAMES = new HashMap<>();
    static {
        NAMES.put(5, "PICKUP_ARMOR_SHARD");
        NAMES.put(50, "PICKUP_ARMOR_LIGHT");
        NAMES.put(100, "PICKUP_ARMOR_HEAVY");
    }
    private ItemStack itemForRespawn;
    private BukkitTask respawnTask;
    private int armor;
    public ArmorSpawner(int armor, World world, Location location) {
        super(new ItemStack(Material.PORKCHOP), world, location);

        ItemStack item = null;
        switch (armor) {
            case 5:
                item = new ItemStack(Material.IRON_INGOT);
                break;
            case 50:
                item = new ItemStack(Material.GOLDEN_CHESTPLATE);
                break;
            case 100:
                item = new ItemStack(Material.NETHERITE_CHESTPLATE);
                break;
        }

        this.armor = armor;
        PersistentDataContainer displayData = display.getPersistentDataContainer();
        NamespacedKey armorKey = new NamespacedKey(QuakePlugin.INSTANCE, "armor");
        displayData.set(armorKey, PersistentDataType.INTEGER, this.armor);

        super.display.setItemStack(item);
        QEntityUtil.setEntityType(super.display, "armor_spawner");

        QuakePlugin.INSTANCE.triggers.add(this);
    }

    public ArmorSpawner(ItemDisplay display) {
        super(display);

        PersistentDataContainer displayData = display.getPersistentDataContainer();
        NamespacedKey armorKey = new NamespacedKey(QuakePlugin.INSTANCE, "armor");
        this.armor = displayData.get(armorKey, PersistentDataType.INTEGER);

        QuakePlugin.INSTANCE.triggers.add(this);
    }

    @Override
    public void onPickup(Player player) {
        QuakeUserState userState = QuakePlugin.INSTANCE.userStates.get(player);

        if (
                this.display.getItemStack().isEmpty() ||
                        userState.armor >= 200
        )
            return;

        this.itemForRespawn = this.display.getItemStack();

        userState.armor += this.armor;
        if (userState.armor > 200) userState.armor = 200;
        if (userState.armor > 100)
            userState.startArmorDecreaser();

        this.display.setItemStack(new ItemStack(Material.AIR)); // Make invisible
        if (this.armor == 5) {
            player.getWorld().playSound(player, "quake.items.armor_shard.pickup", 0.5f, 1f);
        } else {
            player.getWorld().playSound(player, "quake.items.armor.pickup", 0.5f, 1f);
        }
        Hud.pickupMessage(player, Component.text(TranslationManager.t(NAMES.get(this.armor), player)));

        // Respawn in 25 seconds
        this.respawnTask = new BukkitRunnable() {
            public void run() {
                respawn();
            }
        }.runTaskLater(QuakePlugin.INSTANCE, 20*25);
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
