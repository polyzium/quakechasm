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
import org.bukkit.Color;
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

import java.util.Random;

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
        pdc.set(new NamespacedKey(QuakePlugin.INSTANCE, "launch_vec"), PersistentDataType.BYTE_ARRAY, serializedLaunchVec);

        this.particleEmitter = this.newParticleEmitter();
        this.particleEmitter.runTaskTimer(QuakePlugin.INSTANCE, 0, 1);
        QuakePlugin.INSTANCE.triggers.add(this);
    }

    public Jumppad(Marker marker) {
        this.marker = marker;

        PersistentDataContainer pdc = this.marker.getPersistentDataContainer();
        byte[] launchVecData = pdc.get(new NamespacedKey(QuakePlugin.INSTANCE, "launch_vec"), PersistentDataType.BYTE_ARRAY);
        this.launchVec = Vector.fromJOML((Vector3d) SerializationUtils.deserialize(launchVecData));

        this.particleEmitter = this.newParticleEmitter();
        this.particleEmitter.runTaskTimer(QuakePlugin.INSTANCE, 0, 15);

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

                Location center = marker.getLocation();
                center.setY(center.getY() + 0.05);
                Color trailColor = Color.fromRGB(255, 200, 0);
                int durationTicks = 15;

                for (int i = 0; i < 32; i++) {
                    double angle = (i * Math.PI / 16);

                    double destX = Math.cos(angle) * 0.5;
                    double destZ = Math.sin(angle) * 0.5;

                    Location targetLocation = center.clone().add(destX, 0, destZ);

                    Particle.Trail trailData = new Particle.Trail(targetLocation, trailColor, durationTicks);

                    center.getWorld().spawnParticle(Particle.TRAIL, center, 0, trailData);
                }
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
        if (entity instanceof Player player)
            player.setSprinting(false);

        Location iloc = entity.getLocation();
        iloc.setY(iloc.y()+0.1);
        this.triggered = true;

        Location center = marker.getLocation();
        center.setY(center.getY() + 0.05);
        Color trailColor = Color.fromRGB(0xFFFFFF);

        Random randomGenerator = new Random();
        for (int i = 0; i < 64; i++) {
            double angle = (i * Math.PI / 32);

            int random = randomGenerator.nextInt((10 - 5) + 1) + 5;
            double destX = Math.cos(angle) * 1.5 * ((double) random / 10);
            double destZ = Math.sin(angle) * 1.5 * ((double) random / 10);

            Location targetLocation = iloc.clone().add(destX, 0, destZ);

            Particle.Trail trailData = new Particle.Trail(targetLocation, trailColor, random);

            center.getWorld().spawnParticle(Particle.TRAIL, iloc, 0, trailData);
        }

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
