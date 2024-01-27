package ru.darkchronics.quake.events;

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import ru.darkchronics.quake.QuakePlugin;
import ru.darkchronics.quake.game.entities.*;
import ru.darkchronics.quake.game.entities.pickups.ItemSpawner;

public class TriggerListener implements Listener {
    private final QuakePlugin plugin;

    public TriggerListener(QuakePlugin plugin) {
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
        for (Trigger trigger : this.plugin.triggers) {
            if (trigger.isDead()) {
                Location loc = trigger.getLocation();
                Bukkit.getLogger().warning(String.format("Removing dead trigger %s at %.1f %.1f %.1f", trigger.getClass().getName(), loc.x(), loc.y(), loc.z()));
                trigger.remove();
                this.plugin.triggers.remove(trigger);
                continue;
            }
            Player player = event.getPlayer();
            Location ploc = player.getLocation();
            Location sloc = trigger.getLocation();
            ploc.set(
                    addAbs(floorNoSign(ploc.x()), 0.5),
                    Math.floor(ploc.y()),
                    addAbs(floorNoSign(ploc.z()), 0.5)
            );

            // Special cases
            switch (QEntityUtil.getEntityType(trigger.getEntity())) {
                case "item_spawner":
                case "health_spawner":
                    ploc.setY(ploc.y() + 1);
                    break;
                case "jumppad":
//                    ploc.setY(ploc.y() - 1);
                    break;
            }

            if (
                    ploc.x() == sloc.x() &&
                            ploc.y() == sloc.y() &&
                            ploc.z() == sloc.z()
            ) {
                trigger.onTrigger(player);
            }
        }
    }

    private void loadItemSpawner(Entity entity) {
        ItemDisplay display = (ItemDisplay) entity;

        PersistentDataContainer displayData = display.getPersistentDataContainer();
        NamespacedKey spawnerItemKey = new NamespacedKey(this.plugin, "spawner_item");
        byte[] itemData = displayData.get(spawnerItemKey, PersistentDataType.BYTE_ARRAY);
        if (itemData == null) return;

        plugin.triggers.add(new ItemSpawner(display, this.plugin));
    }

    @EventHandler
    public void onEntityAdd(EntityAddToWorldEvent event) {
        Entity entity = event.getEntity();
        if (QEntityUtil.getEntityType(event.getEntity()) == null) return;

        plugin.loadTrigger(entity);
    }

    @EventHandler
    public void onEntityRemove(EntityRemoveFromWorldEvent event) {
        for (int i = 0; i < this.plugin.triggers.size(); i++) {
            Trigger trigger = this.plugin.triggers.get(i);
            if (event.getEntity() != trigger.getEntity()) continue;

            this.plugin.triggers.remove(i);
            return;
        }
    }
}
