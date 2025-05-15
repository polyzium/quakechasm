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

package ru.darkchronics.quake.menus;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import ru.darkchronics.quake.QuakePlugin;
import ru.darkchronics.quake.matchmaking.MatchmakingManager;
import ru.darkchronics.quake.matchmaking.map.QMap;
import ru.darkchronics.quake.matchmaking.matches.MatchMode;
import ru.darkchronics.quake.misc.MiscUtil;
import ru.darkchronics.quake.misc.TranslationManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MatchmakingMenu extends Menu {
    public ArrayList<String> selectedMaps = new ArrayList<>();
    public MatchMode selectedMode = MatchMode.FFA;
    public Locale locale;

    public MatchmakingMenu(Player player) {
        super(Bukkit.createInventory(null, 27, Component.text(TranslationManager.t("MENU_MATCHMAKING", player))));

        this.locale = player.locale();

        MatchmakingManager.PendingParty pendingParty = MatchmakingManager.INSTANCE.findPendingParty(player);
        if (pendingParty != null) {
            this.selectedMaps = new ArrayList<>(pendingParty.getSelectedMaps());
            this.selectedMode = pendingParty.getMatchMode();
        }

        this.drawMaps();
        this.drawGamemodes();
        this.drawMM(player);

        ItemStack backOption = new ItemStack(Material.ARROW);
        MiscUtil.setNameForItemStack(backOption, Component.text(TranslationManager.t("MENU_BACK", player)).decoration(TextDecoration.ITALIC, false));
        this.setItem(25, backOption, Menu.Handler.fromLMB(player1 -> {
            MenuManager.INSTANCE.showMenu(MenuGenerators.mainMenu(player1.locale()), player1);
        }));
    }

    public void drawMM(Player player) {
        MatchmakingManager.PendingParty pendingParty = MatchmakingManager.INSTANCE.findPendingParty(player);
        boolean isSearching = pendingParty != null;
        ItemStack mmOption;
        if (!isSearching) {
            mmOption = new ItemStack(Material.EMERALD);
            MiscUtil.setNameForItemStack(mmOption, Component.text(TranslationManager.t("MENU_MATCHMAKING_SEARCH", player)).decoration(TextDecoration.ITALIC, false));
            if (this.selectedMaps.isEmpty())
                mmOption.lore(List.of(Component.text(TranslationManager.t("MENU_DISABLED_NOMAPS", player)).decoration(TextDecoration.ITALIC, false).color(TextColor.color(0xFF5555))));

            this.setItem(26, mmOption, Menu.Handler.fromLMB(player1 -> {
                if (this.selectedMaps.isEmpty()) return;

                player1.performCommand("quake matchmaking search " + this.selectedMode.toString().toLowerCase() + " " + String.join(" ", selectedMaps));
                this.drawMM(player);
            }));
        } else {
            mmOption = new ItemStack(Material.BARRIER);
            MiscUtil.setNameForItemStack(mmOption, Component.text(TranslationManager.t("MENU_MATCHMAKING_CANCELSEARCH", player)).decoration(TextDecoration.ITALIC, false));
            this.setItem(26, mmOption, Menu.Handler.fromLMB(player1 -> {
                player1.performCommand("quake matchmaking cancel");
                this.drawMM(player);
            }));
        }
    }

    public void drawGamemodes() {
        int index = 18;
        for (MatchMode mode : MatchMode.values()) {
            ItemStack modeOption;

            if (this.selectedMode == mode) {
                modeOption = new ItemStack(Material.GOLDEN_SWORD);
                modeOption.lore(List.of(Component.text(TranslationManager.t("MENU_SELECTED", this.locale)).decoration(TextDecoration.ITALIC, false).color(TextColor.color(0xFFFF55))));
            } else
                modeOption = new ItemStack(Material.IRON_SWORD);

            MiscUtil.setNameForItemStack(modeOption, Component.text(TranslationManager.t(mode.getDisplayName(), this.locale)).decoration(TextDecoration.ITALIC, false));
            this.setItem(index, modeOption, Menu.Handler.fromLMB(player -> {
                boolean isSearching = MatchmakingManager.INSTANCE.findPendingParty(player) != null;
                if (isSearching) return;

                this.selectedMode = mode;

                this.selectedMaps.clear();

                this.drawGamemodes();
                this.drawMaps();
            }));

            index++;
        }
    }

    public void drawMaps() {
        for (int i = 0; i < QuakePlugin.INSTANCE.maps.size(); i++) {
            QMap map = QuakePlugin.INSTANCE.maps.get(i);

            if (!map.recommendedModes.contains(this.selectedMode)) {
                this.removeItem(i);
                continue;
            }

            ItemStack mapOption;
            ArrayList<Component> mapLore = new ArrayList<>(2);
            mapLore.add(Component.text(map.neededPlayers+TranslationManager.t("MENU_MATCHMAKING_CRITERIA_PLAYERSREQUIREMENT", this.locale)).decoration(TextDecoration.ITALIC, false).color(TextColor.color(0x55FFFF)));
            if (selectedMaps.contains(map.name)) {
                mapOption = new ItemStack(Material.GLOW_ITEM_FRAME);
                mapLore.add(Component.text(TranslationManager.t("MENU_SELECTED", this.locale)).decoration(TextDecoration.ITALIC, false).color(TextColor.color(0xFFFF55)));
            } else
                mapOption = new ItemStack(Material.ITEM_FRAME);

            mapOption.lore(mapLore);

            MiscUtil.setNameForItemStack(mapOption, Component.text(map.displayName).decoration(TextDecoration.ITALIC, false));
            this.setItem(i, mapOption, Menu.Handler.fromLMB(player -> {
                boolean isSearching = MatchmakingManager.INSTANCE.findPendingParty(player) != null;
                if (isSearching) return;
                if (!selectedMaps.contains(map.name)) {
                    this.selectedMaps.clear();
                    this.selectedMaps.add(map.name);
                } else
                    this.selectedMaps.remove(map.name);

                this.drawMaps();
                this.drawMM(player);
            }));
        }
    }
}
