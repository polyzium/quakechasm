package ru.darkchronics.quake.menus;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;

import java.util.HashMap;
import java.util.Optional;

public class MenuManager {
    public static MenuManager INSTANCE;
    private HashMap<Player, Menu> menus = new HashMap<>();

    public MenuManager() {
        INSTANCE = this;
//        this.menus = new HashMap<>();
    }

//    public void put(Player player, Menu menu) {
//        menus.put(player, menu);
//    }
//
//    public Menu get(Player player) {
//        return menus.get(player);
//    }
//
//    public void remove(Player player) {
//        menus.remove(player);
//    }

//    public Menu getMenuFromView(InventoryView view) {
//        Optional<Menu> foundMenu = menus.values().stream()
//                .filter(menu -> menu.getInventory() == view.getTopInventory())
//                .findFirst();
//
//        return foundMenu.orElse(null);
//    }

    public Menu getMenuFromInventory(Inventory inventory) {
        Optional<Menu> foundMenu = menus.values().stream()
                .filter(menu -> menu.getInventory() == inventory)
                .findFirst();

        return foundMenu.orElse(null);
    }

    public void showMenu(Menu menu, Player viewer) {
        this.menus.put(viewer, menu);
        viewer.openInventory(menu.getInventory());
    }
}
