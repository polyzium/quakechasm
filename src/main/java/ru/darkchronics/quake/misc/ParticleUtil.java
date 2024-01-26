package ru.darkchronics.quake.misc;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;

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
}
