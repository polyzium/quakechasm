package ru.darkchronics.quake.misc;

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
            world.spawnParticle(Particle.REDSTONE, particleLocation, 1, 0, 0, 0, 0, redstoneOptions, true);
        }
    }
}
