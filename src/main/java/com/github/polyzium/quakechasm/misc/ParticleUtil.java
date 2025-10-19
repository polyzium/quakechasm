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

package com.github.polyzium.quakechasm.misc;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.util.Vector;

public abstract class ParticleUtil {
    public static void drawParticlesCircle(Particle particle, Location loc, double radius, int amount) {
        double centerX = loc.x();
        double centerZ = loc.z();

        double angleIncrement = 2 * Math.PI / amount;
        double currentAngle = 0;

        World world = loc.getWorld();

        for (int i = 0; i < amount; i++) {
            double x = centerX + (radius * Math.cos(currentAngle));
            double z = centerZ + (radius * Math.sin(currentAngle));

            Location particleLocation = new Location(world, x, loc.y(), z);
            world.spawnParticle(particle, particleLocation, 1, 0, 0, 0, 0);

            currentAngle += angleIncrement;
        }
    }

    public static void drawRedstoneLine(Location startLocation, Location endLocation, Particle.DustOptions redstoneOptions) {
        World world = startLocation.getWorld();
        double density = 4;

        Vector direction = endLocation.toVector().subtract(startLocation.toVector()).normalize();
        double distance = startLocation.distance(endLocation);
        int particleCount = (int) (distance * density);

        for (int i = 0; i < particleCount; i++) {
            double ratio = (double) i / particleCount;
            double x = startLocation.getX() + ratio * (endLocation.getX() - startLocation.getX());
            double y = startLocation.getY() + ratio * (endLocation.getY() - startLocation.getY());
            double z = startLocation.getZ() + ratio * (endLocation.getZ() - startLocation.getZ());

            Location particleLocation = new Location(world, x, y, z);
            world.spawnParticle(Particle.DUST, particleLocation, 1, 0, 0, 0, 0, redstoneOptions, true);
        }
    }
}
