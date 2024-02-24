package ru.darkchronics.quake.misc;

import org.bukkit.*;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Arrays;

public abstract class MiscUtil {
    public static final double GRAVITY = 0.08;
    public static final double AIR_DRAG = 0.91;

    public static void teleEffect(Location targetLoc) {
        World world = targetLoc.getWorld();

        world.spawnParticle(Particle.REDSTONE, targetLoc, 32, 0.25, 1, 0.25, 1, new Particle.DustOptions(Color.fromRGB(0xFF00FF), 1));
        world.spawnParticle(Particle.SPELL_INSTANT, targetLoc, 32, 0.25, 1, 0.25, 1);
    }

    public static ArrayList<Vector> calculateTrajectory(Location startLoc, Vector initialVelocity) {
        ArrayList<Vector> trajectory = new ArrayList<>(32);
        Vector currentVelocity = initialVelocity.clone();
        Vector currentPos = startLoc.toVector();

        trajectory.add(currentPos.clone());

        for (int i = 0; i < 127; i++) {
            if (i > 2) {
                currentVelocity.setX(currentVelocity.getX() * AIR_DRAG);
                currentVelocity.setZ(currentVelocity.getZ() * AIR_DRAG);
                currentVelocity.setY(currentVelocity.getY() - GRAVITY);
            }
            currentPos.add(currentVelocity);

            trajectory.add(currentPos.clone());

            if (startLoc.getWorld().getBlockAt(currentPos.toLocation(startLoc.getWorld())).getType() != Material.AIR)
                break;
        }

        return trajectory;
    }

    public static String[] getEnumNames(Class<? extends Enum<?>> e) {
        return Arrays.stream(e.getEnumConstants()).map(Enum::name).toArray(String[]::new);
    }

    public static String[] getEnumNamesLowercase(Class<? extends Enum<?>> e) {
        return Arrays.stream(e.getEnumConstants()).map(Enum::name).map(String::toLowerCase).toArray(String[]::new);
    }
}