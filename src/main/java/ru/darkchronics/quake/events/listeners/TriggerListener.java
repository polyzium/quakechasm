package ru.darkchronics.quake.events.listeners;

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import ru.darkchronics.quake.QuakePlugin;
import ru.darkchronics.quake.game.entities.*;
import ru.darkchronics.quake.game.entities.pickups.Spawner;

import java.util.ArrayList;

public class TriggerListener implements Listener {
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        for (int i = 0; i < QuakePlugin.INSTANCE.triggers.size(); i++) {
            Trigger trigger = QuakePlugin.INSTANCE.triggers.get(i);

            Location triggerLoc = trigger.getLocation();
            if (trigger.isDead()) {
                Bukkit.getLogger().warning(String.format("Removing dead trigger %s at %.1f %.1f %.1f", trigger.getClass().getSimpleName(), triggerLoc.x(), triggerLoc.y(), triggerLoc.z()));
                trigger.remove();
                QuakePlugin.INSTANCE.triggers.remove(trigger);
                continue;
            }

            Player player = event.getPlayer();
            World world = player.getLocation().getWorld();
            if (!world.getName().equals(trigger.getEntity().getWorld().getName())) return;
            BoundingBox bb = trigger.getOffsetBoundingBox();
            Vector triggerMin = triggerLoc.toVector().add(bb.getMin());
            Vector triggerMax = triggerLoc.toVector().add(bb.getMax());
            BoundingBox absoluteBb = BoundingBox.of(triggerMin, triggerMax);

            if (absoluteBb.overlaps(player.getBoundingBox()) && !player.isDead() && player.getGameMode() != GameMode.SPECTATOR) {
                trigger.onTrigger(player);
            }
        }
    }

    @EventHandler
    public void onEntityAdd(EntityAddToWorldEvent event) {
        Entity entity = event.getEntity();
        if (QEntityUtil.getEntityType(event.getEntity()) == null) return;

        QuakePlugin.INSTANCE.loadTrigger(entity);
    }

    @EventHandler
    public void onEntityRemove(EntityRemoveFromWorldEvent event) {
        Trigger toRemove = null;

        for (int i = 0; i < QuakePlugin.INSTANCE.triggers.size(); i++) {
            Trigger trigger = QuakePlugin.INSTANCE.triggers.get(i);
            if (event.getEntity() != trigger.getEntity()) continue;

            if (trigger instanceof Spawner spawner) spawner.respawn();

            toRemove = trigger;
            break;
        }

        if (toRemove != null) {
            toRemove.onUnload();
            QuakePlugin.INSTANCE.triggers.remove(toRemove);
        }

    }
}
