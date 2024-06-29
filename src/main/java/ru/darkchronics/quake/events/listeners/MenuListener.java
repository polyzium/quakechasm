package ru.darkchronics.quake.events.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryView;
import ru.darkchronics.quake.menus.Menu;
import ru.darkchronics.quake.menus.MenuManager;

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
