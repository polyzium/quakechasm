package ru.darkchronics.quake.events;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import ru.darkchronics.quake.game.weapons.WeaponsHandler;

import java.util.function.Consumer;

public class WeaponsListener implements Listener {
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getPlayer().getInventory().getItemInMainHand().getType() != Material.CARROT_ON_A_STICK) return;
        // TODO switch statement for item type
        // for now carrot on a stick (heavy machinegun) is the only weapon

        Consumer<Player> hmgClass = WeaponsHandler::fireHMG;

        hmgClass.accept(event.getPlayer());
    }
}
