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

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class Menu {
    public static class Handler extends EnumMap<ClickType, Consumer<Player>> {
        public Handler() {
            super(ClickType.class);
        }

        public static Handler fromLMB(Consumer<Player> leftClickCallback) {
            Handler handler = new Handler();
            handler.put(ClickType.LEFT, leftClickCallback);
            return handler;
        }
    }

    private Inventory inv;
    private HashMap<Integer, Handler> handlers;

    public Menu(Inventory inv) {
        this.inv = inv;
        this.handlers = new HashMap<>();
    }

    public Inventory getInventory() {
        return this.inv;
    }

    public void setItem(int index, ItemStack itemStack, Handler handler) {
        this.inv.setItem(index, itemStack);
        this.handlers.put(index, handler);
    }

    public void removeItem(int index) {
        this.inv.clear(index);
        this.handlers.remove(index);
    }

    public Handler getHandler(int index) {
        return this.handlers.get(index);
    }
}
