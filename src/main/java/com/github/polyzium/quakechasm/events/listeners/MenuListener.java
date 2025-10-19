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

package com.github.polyzium.quakechasm.events.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryView;
import com.github.polyzium.quakechasm.menus.Menu;
import com.github.polyzium.quakechasm.menus.MenuManager;

import java.util.function.Consumer;

public class MenuListener implements Listener {
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        InventoryView view = event.getView();
        Menu menu = MenuManager.INSTANCE.getMenuFromInventory(view.getTopInventory());
        if (menu == null) return;

        event.setCancelled(true);
        if (event.getClickedInventory() != menu.getInventory()) return;

        Menu.Handler handler = menu.getHandler(event.getSlot());
        if (handler == null) return;
        Consumer<Player> callback = handler.get(event.getClick());
        if (callback == null) return;

        if (event.getWhoClicked() instanceof Player player)
            callback.accept(player);
        else
            event.getWhoClicked().sendMessage("getWhoClicked is not a player");
    }
}
