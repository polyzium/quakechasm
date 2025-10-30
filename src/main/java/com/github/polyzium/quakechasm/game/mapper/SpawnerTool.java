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

package com.github.polyzium.quakechasm.game.mapper;

import com.github.polyzium.quakechasm.game.combat.powerup.Powerup;
import com.github.polyzium.quakechasm.misc.TranslationManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import com.github.polyzium.quakechasm.QuakePlugin;
import com.github.polyzium.quakechasm.game.combat.powerup.PowerupType;
import com.github.polyzium.quakechasm.game.entities.pickups.*;
import com.github.polyzium.quakechasm.matchmaking.Team;

import static com.github.polyzium.quakechasm.game.entities.pickups.WeaponSpawner.NAMES;

public class SpawnerTool {

    public static ItemStack createRedFlagTool() {
        ItemStack item = new ItemStack(Material.RED_BANNER);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(TranslationManager.t("mapper.tool.spawner.name.redFlag", TranslationManager.FALLBACK)
                .color(NamedTextColor.RED)
                .decoration(TextDecoration.ITALIC, false));
        
        NamespacedKey key = new NamespacedKey(QuakePlugin.INSTANCE, "spawner_tool");
        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, "ctf_flag:red");
        
        item.setItemMeta(meta);
        return item;
    }
    
    public static ItemStack createBlueFlagTool() {
        ItemStack item = new ItemStack(Material.BLUE_BANNER);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(TranslationManager.t("mapper.tool.spawner.name.blueFlag", TranslationManager.FALLBACK)
                .color(NamedTextColor.BLUE)
                .decoration(TextDecoration.ITALIC, false));
        
        NamespacedKey key = new NamespacedKey(QuakePlugin.INSTANCE, "spawner_tool");
        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, "ctf_flag:blue");
        
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack createWeaponSpawnerTool(int weaponIndex) {
        ItemStack item = WeaponSpawner.getWeapon(weaponIndex).clone();
        ItemStack toolItem = new ItemStack(Material.AMETHYST_SHARD, item.getAmount());
        ItemMeta itemMeta = item.getItemMeta();
        toolItem.setItemMeta(itemMeta);

        ItemMeta toolMeta = toolItem.getItemMeta();
        Component name = TranslationManager.t(NAMES[weaponIndex], TranslationManager.FALLBACK);

        toolMeta.displayName(TranslationManager.t("mapper.tool.spawner.name.weapon", TranslationManager.FALLBACK,
                net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.component("weapon", name))
                .color(NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false));
        
        NamespacedKey key = new NamespacedKey(QuakePlugin.INSTANCE, "spawner_tool");
        toolMeta.getPersistentDataContainer().set(key, PersistentDataType.STRING, "weapon:" + weaponIndex);
        
        toolItem.setItemMeta(toolMeta);
        return toolItem;
    }

    public static ItemStack createAmmoSpawnerTool(int ammoType) {
        ItemStack item = new ItemStack(Material.GUNPOWDER);
        ItemMeta meta = item.getItemMeta();
        meta.setCustomModelData(ammoType);
        
        meta.displayName(TranslationManager.t("mapper.tool.spawner.name.ammo", TranslationManager.FALLBACK,
                net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed("ammo",
                    TranslationManager.tLegacy(AmmoSpawner.NAMES[ammoType], TranslationManager.FALLBACK)))
                .color(NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
        
        NamespacedKey key = new NamespacedKey(QuakePlugin.INSTANCE, "spawner_tool");
        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, "ammo:" + ammoType);
        
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack createHealthSpawnerTool(int health) {
        Material material = switch (health) {
            case 1 -> Material.CARROT;
            case 5 -> Material.BAKED_POTATO;
            case 10 -> Material.COOKED_BEEF;
            case 20 -> Material.GOLDEN_APPLE;
            default -> Material.PORKCHOP;
        };
        
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        String healthKey = switch (health) {
            case 1 -> "mapper.tool.spawner.name.health.small";
            case 5 -> "mapper.tool.spawner.name.health.medium";
            case 10 -> "mapper.tool.spawner.name.health.large";
            case 20 -> "mapper.tool.spawner.name.health.mega";
            default -> "mapper.tool.spawner.name.health.generic";
        };
        
        meta.displayName(TranslationManager.t(healthKey, TranslationManager.FALLBACK)
                .color(NamedTextColor.GREEN)
                .decoration(TextDecoration.ITALIC, false));
        
        NamespacedKey key = new NamespacedKey(QuakePlugin.INSTANCE, "spawner_tool");
        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, "health:" + health);
        
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack createArmorSpawnerTool(int armor) {
        Material material = switch (armor) {
            case 5 -> Material.IRON_INGOT;
            case 50 -> Material.GOLDEN_CHESTPLATE;
            case 100 -> Material.NETHERITE_CHESTPLATE;
            default -> Material.IRON_INGOT;
        };
        
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        String armorKey = switch (armor) {
            case 5 -> "mapper.tool.spawner.name.armor.shard";
            case 50 -> "mapper.tool.spawner.name.armor.light";
            case 100 -> "mapper.tool.spawner.name.armor.heavy";
            default -> "mapper.tool.spawner.name.armor.generic";
        };
        
        meta.displayName(TranslationManager.t(armorKey, TranslationManager.FALLBACK)
                .color(NamedTextColor.AQUA)
                .decoration(TextDecoration.ITALIC, false));
        
        NamespacedKey key = new NamespacedKey(QuakePlugin.INSTANCE, "spawner_tool");
        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, "armor:" + armor);
        
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack createPowerupSpawnerTool(PowerupType type) {
        ItemStack item = new ItemStack(Material.TOTEM_OF_UNDYING);
        ItemMeta meta = item.getItemMeta();
        meta.setCustomModelData(type.ordinal());
        
        meta.displayName(TranslationManager.t("mapper.tool.spawner.name.powerup", TranslationManager.FALLBACK,
                net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed("powerup",
                    TranslationManager.tLegacy(Powerup.NAMES.get(type), TranslationManager.FALLBACK)))
                .color(NamedTextColor.LIGHT_PURPLE)
                .decoration(TextDecoration.ITALIC, false));
        
        NamespacedKey key = new NamespacedKey(QuakePlugin.INSTANCE, "spawner_tool");
        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, "powerup:" + type.name());
        
        item.setItemMeta(meta);
        return item;
    }

    public static boolean isSpawnerTool(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        
        ItemMeta meta = item.getItemMeta();
        NamespacedKey key = new NamespacedKey(QuakePlugin.INSTANCE, "spawner_tool");
        return meta.getPersistentDataContainer().has(key, PersistentDataType.STRING);
    }

    public static String getSpawnerToolType(ItemStack item) {
        if (!isSpawnerTool(item)) return null;
        
        ItemMeta meta = item.getItemMeta();
        NamespacedKey key = new NamespacedKey(QuakePlugin.INSTANCE, "spawner_tool");
        return meta.getPersistentDataContainer().get(key, PersistentDataType.STRING);
    }

    public static void placeSpawner(String toolType, Location location) {
        if (toolType == null) return;
        
        String[] parts = toolType.split(":", 2);
        if (parts.length < 2) return;
        
        String category = parts[0];
        String value = parts[1];
        
        switch (category) {
            case "ctf_flag":
                Team team = value.equals("red") ? Team.RED : Team.BLUE;
                new CTFFlag(team, false, null, location);
                break;
                
            case "weapon":
                int weaponIndex = Integer.parseInt(value);
                new WeaponSpawner(weaponIndex, location.getWorld(), location);
                break;
                
            case "ammo":
                int ammoType = Integer.parseInt(value);
                new AmmoSpawner(ammoType, location.getWorld(), location);
                break;
                
            case "health":
                int health = Integer.parseInt(value);
                new HealthSpawner(health, location.getWorld(), location);
                break;
                
            case "armor":
                int armor = Integer.parseInt(value);
                new ArmorSpawner(armor, location.getWorld(), location);
                break;
                
            case "powerup":
                PowerupType type = PowerupType.valueOf(value);
                new PowerupSpawner(type, location.getWorld(), location, false, 30);
                break;
        }
    }
}