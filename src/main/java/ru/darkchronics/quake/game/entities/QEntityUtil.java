/*
 * DarkChronics-Quake, a Quake minigame plugin for Minecraft servers running PaperMC
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

package ru.darkchronics.quake.game.entities;

import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
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
