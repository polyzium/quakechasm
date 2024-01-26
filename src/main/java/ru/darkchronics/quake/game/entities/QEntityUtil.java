package ru.darkchronics.quake.game.entities;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Projectile;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public abstract class QEntityUtil {
    public static String getEntityType(Entity entity) {
        PersistentDataContainer pdc = entity.getPersistentDataContainer();
        return pdc.get(new NamespacedKey("darkchronics-quake", "entity_type"), PersistentDataType.STRING);
    }

    public static void setEntityType(Entity entity, String type) {
        PersistentDataContainer pdc = entity.getPersistentDataContainer();
        pdc.set(new NamespacedKey("darkchronics-quake", "entity_type"), PersistentDataType.STRING, type);
    }
}
