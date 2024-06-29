package ru.darkchronics.quake.menus;

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
