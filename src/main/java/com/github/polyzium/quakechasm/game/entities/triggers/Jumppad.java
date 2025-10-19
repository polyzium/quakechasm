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

package com.github.polyzium.quakechasm.game.entities.triggers;

import org.apache.commons.lang3.SerializationUtils;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Marker;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.joml.Vector3d;
import com.github.polyzium.quakechasm.QuakePlugin;
import com.github.polyzium.quakechasm.game.entities.QEntityUtil;
import com.github.polyzium.quakechasm.game.entities.Trigger;
import com.github.polyzium.quakechasm.misc.ParticleUtil;

public class Jumppad implements Trigger {
    static BoundingBox boundingBox = new BoundingBox(-0.5, 0, -0.5, 0.5, 1, 0.5);
    private Vector launchVec;
    private Marker marker;
    private boolean triggered;
    private BukkitRunnable particleEmitter;
    public Jumppad(Location loc, Vector launchVec) {
        this.marker = (Marker) loc.getWorld().spawnEntity(loc, EntityType.MARKER);
        this.launchVec = launchVec;

        QEntityUtil.setEntityType(marker, "jumppad");

        byte[] serializedLaunchVec = SerializationUtils.serialize(this.launchVec.toVector3d());
        PersistentDataContainer pdc = this.marker.getPersistentDataContainer();
        pdc.set(new NamespacedKey("darkchronics-quake", "launch_vec"), PersistentDataType.BYTE_ARRAY, serializedLaunchVec);

        this.particleEmitter = this.newParticleEmitter();
        this.particleEmitter.runTaskTimer(QuakePlugin.INSTANCE, 0, 1);
        QuakePlugin.INSTANCE.triggers.add(this);
    }

    public Jumppad(Marker marker) {
        this.marker = marker;

        PersistentDataContainer pdc = this.marker.getPersistentDataContainer();
        byte[] launchVecData = pdc.get(new NamespacedKey("darkchronics-quake", "launch_vec"), PersistentDataType.BYTE_ARRAY);
        this.launchVec = Vector.fromJOML((Vector3d) SerializationUtils.deserialize(launchVecData));

        this.particleEmitter = this.newParticleEmitter();
        this.particleEmitter.runTaskTimer(QuakePlugin.INSTANCE, 0, 1);

        QuakePlugin.INSTANCE.triggers.add(this);
    }

    private BukkitRunnable newParticleEmitter() {
        return new BukkitRunnable() {
            @Override
            public void run() {
                if (marker.isDead()) {
                    this.cancel();
                    return;
                }

                Location loc2 = marker.getLocation();
                loc2.getWorld().spawnParticle(Particle.INSTANT_EFFECT, loc2, 1, 0.25, 0, 0.25, 0);
            }
        };
    }

    @Override
    public void onTrigger(Entity entity) {
        if (this.triggered || entity.isSneaking()) return;

        // Fix for the overshoot problem
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks == 2) cancel();
//                Vector v = launchVec.clone();
//                entity.setVelocity(v.multiply(0.9));
                entity.setVelocity(launchVec);
                ticks++;
            }
        }.runTaskTimer(QuakePlugin.INSTANCE, 0, 1);

        this.marker.getWorld().playSound(this.marker.getLocation(), "quake.world.jumppad", 1, 1);
        Location iloc = entity.getLocation();
        iloc.setY(iloc.y()+0.1);
        this.triggered = true;

        new BukkitRunnable() {
            int iter = 0;
            Location loc = iloc;
            @Override
            public void run() {
                if (iter >= 4)
                    this.cancel();

                ParticleUtil.drawParticlesCircle(Particle.ELECTRIC_SPARK, loc, (double) iter /2, 32);
                iter++;
            }
        }.runTaskTimer(QuakePlugin.INSTANCE, 0, 1);

        new BukkitRunnable() {
            @Override
            public void run() {
                triggered = false;
            }
        }.runTaskLater(QuakePlugin.INSTANCE, 10);
    }

    public void onUnload() {
        this.particleEmitter.cancel();
    }

    public static void testJump(Player player, Vector launchVec, QuakePlugin plugin) {
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks == 2) cancel();
//                Vector v = launchVec.clone();
//                entity.setVelocity(v.multiply(0.9));
                player.setVelocity(launchVec);
                ticks++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    @Override
    public Location getLocation() {
        return this.marker.getLocation();
    }

    @Override
    public Entity getEntity() {
        return this.marker;
    }

    public Vector getLaunchVec() {
        return launchVec;
    }

    @Override
    public BoundingBox getOffsetBoundingBox() {
        return boundingBox;
    }

    @Override
    public void remove() {
        this.onUnload();
        this.marker.remove();
    }
}
