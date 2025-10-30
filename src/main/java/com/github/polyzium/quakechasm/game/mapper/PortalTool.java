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

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import com.github.polyzium.quakechasm.QuakePlugin;
import com.github.polyzium.quakechasm.game.entities.triggers.Portal;
import com.github.polyzium.quakechasm.misc.TranslationManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.Arrays;

public class PortalTool {
    
    public enum PortalToolState {
        IDLE,
        PLACING_PORTAL,
        SETTING_TARGET,
        EDITING_TARGET
    }
    
    public static class PortalToolData {
        public PortalToolState state = PortalToolState.IDLE;
        public Location portalLocation = null;
        public Portal editingPortal = null;
        
        public void reset() {
            state = PortalToolState.IDLE;
            portalLocation = null;
            editingPortal = null;
        }
    }
    
    public static ItemStack createPortalTool() {
        ItemStack tool = new ItemStack(Material.ENDER_PEARL);
        ItemMeta meta = tool.getItemMeta();
        
        meta.displayName(TranslationManager.t("mapper.tool.portal.name", TranslationManager.FALLBACK)
            .color(NamedTextColor.LIGHT_PURPLE)
            .decoration(TextDecoration.ITALIC, false));
        
        meta.lore(Arrays.asList(
            TranslationManager.t("mapper.tool.portal.lore.pointAtPortal", TranslationManager.FALLBACK)
                .color(NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false),
            TranslationManager.t("mapper.tool.portal.lore.leftClickEdit", TranslationManager.FALLBACK)
                .color(NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false),
            TranslationManager.t("mapper.tool.portal.lore.pointAtBlock", TranslationManager.FALLBACK)
                .color(NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false),
            TranslationManager.t("mapper.tool.portal.lore.rightClickPlace", TranslationManager.FALLBACK)
                .color(NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false),
            TranslationManager.t("mapper.tool.portal.lore.rightClickTarget", TranslationManager.FALLBACK)
                .color(NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false),
            TranslationManager.t("mapper.tool.portal.lore.leftClickCancel", TranslationManager.FALLBACK)
                .color(NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false)
        ));
        
        meta.getPersistentDataContainer().set(
            new NamespacedKey(QuakePlugin.INSTANCE, "portal_tool"),
            PersistentDataType.BOOLEAN,
            true
        );
        
        tool.setItemMeta(meta);
        return tool;
    }
    
    public static boolean isPortalTool(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        
        return item.getItemMeta().getPersistentDataContainer().has(
            new NamespacedKey(QuakePlugin.INSTANCE, "portal_tool"),
            PersistentDataType.BOOLEAN
        );
    }
}