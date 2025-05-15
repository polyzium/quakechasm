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

package ru.darkchronics.quake.game.entities.triggers;

import org.apache.commons.lang3.SerializationUtils;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Marker;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3d;
import ru.darkchronics.quake.QuakePlugin;
import ru.darkchronics.quake.game.entities.QEntityUtil;
import ru.darkchronics.quake.game.entities.Trigger;
import ru.darkchronics.quake.misc.MiscUtil;
import ru.darkchronics.quake.misc.ParticleUtil;

public class Portal implements Trigger {
    static BoundingBox boundingBox = new BoundingBox(-0.5, 0, -0.5, 0.5, 2, 0.5);
    Marker marker;
    Location targetLoc;
    BukkitRunnable particleEmitter;

    public Portal(Location loc, Location targetLoc) {
        this.marker = (Marker) loc.getWorld().spawnEntity(loc, EntityType.MARKER);
        this.targetLoc = targetLoc;

        QEntityUtil.setEntityType(marker, "portal");

        byte[] serializedTargetPos = SerializationUtils.serialize(this.targetLoc.toVector().toVector3d());
        byte[] serializedTargetDir = SerializationUtils.serialize(this.targetLoc.getDirection().toVector3d());
        PersistentDataContainer pdc = this.marker.getPersistentDataContainer();
        pdc.set(new NamespacedKey("darkchronics-quake", "target_pos"), PersistentDataType.BYTE_ARRAY, serializedTargetPos);
        pdc.set(new NamespacedKey("darkchronics-quake", "target_dir"), PersistentDataType.BYTE_ARRAY, serializedTargetDir);

        this.particleEmitter = this.newParticleEmitter();
        this.particleEmitter.runTaskTimer(QuakePlugin.INSTANCE, 0, 1);
        QuakePlugin.INSTANCE.triggers.add(this);
    }

    public Portal(Marker marker) {
        this.marker = marker;

        PersistentDataContainer pdc = this.marker.getPersistentDataContainer();
        byte[] targetPosData = pdc.get(new NamespacedKey("darkchronics-quake", "target_pos"), PersistentDataType.BYTE_ARRAY);
        byte[] targetDirData = pdc.get(new NamespacedKey("darkchronics-quake", "target_dir"), PersistentDataType.BYTE_ARRAY);
        Vector targetPos = Vector.fromJOML((Vector3d) SerializationUtils.deserialize(targetPosData));
        Vector targetDir = Vector.fromJOML((Vector3d) SerializationUtils.deserialize(targetDirData));
        this.targetLoc = targetPos.toLocation(marker.getWorld()).setDirection(targetDir);

        this.particleEmitter = this.newParticleEmitter();
        this.particleEmitter.runTaskTimer(QuakePlugin.INSTANCE, 0, 1);

        QuakePlugin.INSTANCE.triggers.add(this);
    }

    private BukkitRunnable newParticleEmitter() {
        return new BukkitRunnable() {
            int ticks;

            @Override
            public void run() {
                if (marker.isDead()) {
                    this.cancel();
                    return;
                }

                Location loc = marker.getLocation();
                loc.add(0, 1,0);
                loc.getWorld().spawnParticle(Particle.PORTAL, loc, 2, 0.25, 0.5, 0.25, 0.25);

                Location loc2 = marker.getLocation();
                loc2.add(0, (double) ticks /4, 0);
                ParticleUtil.drawParticlesCircle(Particle.ELECTRIC_SPARK, loc2, 0.75, 8);

                if (ticks >= 8) {
                    ticks = 0;
                }

                ticks++;
            }
        };
    }

    @Override
    public void onTrigger(Entity entity) {
        entity.teleport(this.targetLoc);

        MiscUtil.teleEffect(this.marker.getLocation(), true);
        MiscUtil.teleEffect(targetLoc, false);
    }

    @Override
    public void onUnload() {
        this.particleEmitter.cancel();
    }

    @Override
    public Location getLocation() {
        return marker.getLocation();
    }

    @Override
    public Entity getEntity() {
        return marker;
    }

    @Override
    public BoundingBox getOffsetBoundingBox() {
        return boundingBox;
    }

    @Override
    public void remove() {
        this.onUnload();
        marker.remove();
    }
}
