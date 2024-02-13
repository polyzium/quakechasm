package ru.darkchronics.quake.game.entities;

import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.function.Predicate;

public abstract class QEntityUtil {
    public static String getEntityType(Entity entity) {
        PersistentDataContainer pdc = entity.getPersistentDataContainer();
        return pdc.get(new NamespacedKey("darkchronics-quake", "entity_type"), PersistentDataType.STRING);
    }

    public static void setEntityType(Entity entity, String type) {
        PersistentDataContainer pdc = entity.getPersistentDataContainer();
        pdc.set(new NamespacedKey("darkchronics-quake", "entity_type"), PersistentDataType.STRING, type);
    }

    public static Entity nearestEntity(Location loc, double radius, Predicate<Entity> predicate) {
        Entity nearestEntity = null;
        for (Entity nearbyEntity : loc.getNearbyEntities(radius, radius, radius)) {
            if (!predicate.test(nearbyEntity)) continue;

            if (nearestEntity == null || nearbyEntity.getLocation().distance(loc) < nearestEntity.getLocation().distance(loc))
                nearestEntity = nearbyEntity;
        }

        return nearestEntity;
    }
}
