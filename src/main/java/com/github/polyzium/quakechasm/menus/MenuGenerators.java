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

package com.github.polyzium.quakechasm.menus;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import com.github.polyzium.quakechasm.QuakePlugin;
import com.github.polyzium.quakechasm.matchmaking.MatchmakingManager;
import com.github.polyzium.quakechasm.misc.MiscUtil;
import com.github.polyzium.quakechasm.misc.TranslationManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public abstract class MenuGenerators {
    public static Menu mainMenu(Locale locale) {
        Menu menu = new Menu(Bukkit.createInventory(null, 9, TranslationManager.t("menu.main", locale)));

        ItemStack partyOption = new ItemStack(Material.OAK_BOAT);
        MiscUtil.setNameForItemStack(partyOption, TranslationManager.t("party.title", locale).decoration(TextDecoration.ITALIC, false));
        ItemStack matchmakingOption = new ItemStack(Material.CROSSBOW);
        MiscUtil.setNameForItemStack(matchmakingOption, TranslationManager.t("menu.matchmaking.title", locale).decoration(TextDecoration.ITALIC, false));

        menu.setItem(3, partyOption, Menu.Handler.fromLMB(player -> {
            MenuManager.INSTANCE.showMenu(MenuGenerators.partyMenu(player), player);
        }));
        menu.setItem(4, matchmakingOption, Menu.Handler.fromLMB(player -> {
            if (QuakePlugin.INSTANCE.userStates.get(player).mmState.currentParty.leader == player)
                MenuManager.INSTANCE.showMenu(new MatchmakingMenu(player), player);
            else
                player.sendMessage(TranslationManager.t("error.party.matchmaking.leaderOnly", locale));
        }));

        return menu;
    }

    public static Menu partyMenu(Player viewer) {
        Menu menu = new Menu(Bukkit.createInventory(null, 27, TranslationManager.t("menu.party", viewer)));
        MatchmakingManager.Party party = QuakePlugin.INSTANCE.userStates.get(viewer)
                .mmState.currentParty;

        for (int index = 0; index < party.size(); index++) {
            if (index == 25) break;

            Player partyMember = party.getPlayers().get(index);

            Menu.Handler handler = new Menu.Handler();
            if (viewer == party.leader && !(partyMember == viewer)) {
                handler.put(ClickType.DOUBLE_CLICK, player -> {
                    player.performCommand("quake party kick " + partyMember.getName());
                    MenuManager.INSTANCE.showMenu(partyMenu(player), player);
                });
            }

            ItemStack playerOption = new ItemStack(Material.PLAYER_HEAD);
            MiscUtil.setNameForItemStack(playerOption, partyMember.displayName().decoration(TextDecoration.ITALIC, false));
            ArrayList<Component> playerLore = new ArrayList<>(2);
            if (partyMember == party.leader)
                playerLore.add(TranslationManager.t("menu.leader", viewer).decoration(TextDecoration.ITALIC, false).color(TextColor.color(0x55FF55)));
            if (viewer == party.leader && !(partyMember == viewer))
                playerLore.add(TranslationManager.t("menu.kickPlayer", viewer).decoration(TextDecoration.ITALIC, false).color(TextColor.color(0xAA0000)));

            playerOption.lore(playerLore);
            menu.setItem(index, playerOption, handler);
        }

        ItemStack leaveOption = new ItemStack(Material.ACACIA_DOOR);
        MiscUtil.setNameForItemStack(leaveOption, TranslationManager.t("menu.leaveParty", viewer).decoration(TextDecoration.ITALIC, false));
        if (party.size() == 1)
            leaveOption.lore(List.of(TranslationManager.t("menu.disabled.partySingle", viewer).decoration(TextDecoration.ITALIC, false).color(TextColor.color(0xFF5555))));
        menu.setItem(26, leaveOption, Menu.Handler.fromLMB(player -> {
            if (party.size() != 1) {
                player.performCommand("quake party leave");
                player.closeInventory();
            }
        }));

        ItemStack backOption = new ItemStack(Material.ARROW);
        MiscUtil.setNameForItemStack(backOption, TranslationManager.t("menu.back", viewer).decoration(TextDecoration.ITALIC, false));
        menu.setItem(25, backOption, Menu.Handler.fromLMB(player -> {
            MenuManager.INSTANCE.showMenu(MenuGenerators.mainMenu(viewer.locale()), viewer);
        }));

        return menu;
    }
}
