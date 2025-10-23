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

package com.github.polyzium.quakechasm.game.entities;

import com.github.polyzium.quakechasm.QuakePlugin;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Set;
import java.util.function.Predicate;

public abstract class QEntityUtil {
    public static String getEntityType(Entity entity) {
        PersistentDataContainer pdc = entity.getPersistentDataContainer();
        String oldType = pdc.get(new NamespacedKey("darkchronics-quake", "entity_type"), PersistentDataType.STRING);
        if (oldType != null) {

            Set<NamespacedKey> keys = pdc.getKeys();

            QuakePlugin.INSTANCE.getLogger().warning(String.format("Migrating DarkChronics-Quake entity %s to Quakechasm entity", oldType));
            String oldNamespace = "darkchronics-quake";
            String newNamespace = "quakechasm";

            for (NamespacedKey key : keys) {
                if (key.getNamespace().equalsIgnoreCase(oldNamespace)) {
                    // Transfer data under this key to the new namespace
                    NamespacedKey newKey = new NamespacedKey(newNamespace, key.getKey());
                    
                    // Try different data types to handle all possible stored values
                    try {
                        // Try STRING first (most common)
                        String stringValue = pdc.get(key, PersistentDataType.STRING);
                        if (stringValue != null) {
                            pdc.set(newKey, PersistentDataType.STRING, stringValue);
                            pdc.remove(key);
                            continue;
                        }
                    } catch (IllegalArgumentException ignored) {}
                    
                    try {
                        // Try INTEGER
                        Integer intValue = pdc.get(key, PersistentDataType.INTEGER);
                        if (intValue != null) {
                            pdc.set(newKey, PersistentDataType.INTEGER, intValue);
                            pdc.remove(key);
                            continue;
                        }
                    } catch (IllegalArgumentException ignored) {}
                    
                    try {
                        // Try BYTE_ARRAY
                        byte[] byteArrayValue = pdc.get(key, PersistentDataType.BYTE_ARRAY);
                        if (byteArrayValue != null) {
                            pdc.set(newKey, PersistentDataType.BYTE_ARRAY, byteArrayValue);
                            pdc.remove(key);
                            continue;
                        }
                    } catch (IllegalArgumentException ignored) {}
                    
                    // If we get here, we couldn't migrate this key
                    QuakePlugin.INSTANCE.getLogger().warning(String.format("Could not migrate key %s - unknown data type", key));
                }
            }
        }

        return pdc.get(new NamespacedKey(QuakePlugin.INSTANCE, "entity_type"), PersistentDataType.STRING);
    }

    public static void setEntityType(Entity entity, String type) {
        PersistentDataContainer pdc = entity.getPersistentDataContainer();
        pdc.set(new NamespacedKey(QuakePlugin.INSTANCE, "entity_type"), PersistentDataType.STRING, type);
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
