package ru.darkchronics.quake.events;

import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import ru.darkchronics.quake.QuakePlugin;
import ru.darkchronics.quake.game.entities.ItemSpawner;

public class MoveListener implements Listener {
    private QuakePlugin plugin;

    public MoveListener(QuakePlugin plugin) {
        this.plugin = plugin;
    }

    public static double floorNoSign(double value) {
        return (long)value;
    }

    public static double addAbs(double value, double rhs) {
        if (value > 0) {
            return Math.abs(value) + rhs;
        } else {
            return -(Math.abs(value) + rhs);
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        for (int i = 0; i < this.plugin.itemSpawners.size(); i++) {
            ItemSpawner spawner = this.plugin.itemSpawners.get(i);
            Player player = event.getPlayer();
            Location ploc = player.getLocation();
            Location sloc = spawner.display.getLocation();
            ploc.set(
                    addAbs(floorNoSign(ploc.x()), 0.5),
                    addAbs(floorNoSign(ploc.y()), 1),
                    addAbs(floorNoSign(ploc.z()), 0.5)
            );

            if (
                    ploc.x() == sloc.x() &&
                            ploc.y() == sloc.y() &&
                            ploc.z() == sloc.z()
            ) {
                spawner.onPickup(player);
            }
        }
    }
}
