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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import com.github.polyzium.quakechasm.misc.adapters.LocationAdapter;
import com.github.polyzium.quakechasm.misc.adapters.WorldAdapter;

import java.util.ArrayList;
import java.util.Arrays;

public abstract class MiscUtil {
    public static final double GRAVITY = 0.08;
    public static final double AIR_DRAG = 0.91;

    public static void teleEffect(Location targetLoc, boolean teleOut) {
        World world = targetLoc.getWorld();

        if (!teleOut)
            targetLoc.getWorld().playSound(targetLoc, "quake.world.tele_in", 1, 1);
        else
            targetLoc.getWorld().playSound(targetLoc, "quake.world.tele_out", 1, 1);

        world.spawnParticle(Particle.DUST, targetLoc, 32, 0.25, 1, 0.25, 1, new Particle.DustOptions(Color.fromRGB(0xFF00FF), 1));
        world.spawnParticle(Particle.INSTANT_EFFECT, targetLoc, 32, 0.25, 1, 0.25, 1);
    }

    public static ArrayList<Vector> calculateTrajectory(Location startLoc, Vector initialVelocity) {
        ArrayList<Vector> trajectory = new ArrayList<>(128);
        Vector pos = startLoc.toVector();
        World world = startLoc.getWorld();

        Vector velocity = initialVelocity.clone();
        // Tick 0: Apply ground friction to X/Z if starting on ground
        velocity.setX(velocity.getX() * 0.6);
        velocity.setZ(velocity.getZ() * 0.6);

        trajectory.add(pos.clone());

        for (int i = 0; i < 127; i++) {
            Vector nextPos;
            if (i == 3) {
                velocity = initialVelocity.clone();
            } else {
                velocity.setY(velocity.getY() - GRAVITY);
                velocity.setX(velocity.getX() * AIR_DRAG);
                velocity.setZ(velocity.getZ() * AIR_DRAG);
            }
            nextPos = pos.clone().add(velocity);

            if (raytraceHitsBlock(world, pos, nextPos)) break;

            pos.copy(nextPos);
            trajectory.add(pos.clone());

            if (world.getBlockAt(pos.toLocation(world)).getType() != Material.AIR) break;
        }

        return trajectory;
    }

    private static boolean raytraceHitsBlock(World world, Vector start, Vector end) {
        Vector direction = end.clone().subtract(start);
        double distance = direction.length();
        
        if (distance == 0) {
            return false;
        }
        
        direction.normalize();
        
        // Step through the ray in small increments (0.1 blocks)
        double step = 0.1;
        int steps = (int) Math.ceil(distance / step);
        
        for (int i = 0; i <= steps; i++) {
            Vector checkPos = start.clone().add(direction.clone().multiply(i * step));
            Location checkLoc = checkPos.toLocation(world);

            if (!world.getBlockAt(checkLoc).isPassable()) {
                return true;
            }
        }
        
        return false;
    }

    public static String[] getEnumNames(Class<? extends Enum<?>> e) {
        return Arrays.stream(e.getEnumConstants()).map(Enum::name).toArray(String[]::new);
    }

    public static String[] getEnumNamesLowercase(Class<? extends Enum<?>> e) {
        return Arrays.stream(e.getEnumConstants()).map(Enum::name).map(String::toLowerCase).toArray(String[]::new);
    }

    public static Gson getEnhancedGson() {
        GsonBuilder builder = new GsonBuilder();
        builder.setPrettyPrinting();
        builder.registerTypeHierarchyAdapter(World.class, new WorldAdapter());
        builder.registerTypeAdapter(Location.class, new LocationAdapter());

        return builder.create();
    }

    public static boolean chunkIntersectsBoundingBox(Chunk chunk, BoundingBox boundingBox) {
        int chunkMinX = chunk.getX() << 4;
        int chunkMinZ = chunk.getZ() << 4;
        int chunkMaxX = chunkMinX + 15;
        int chunkMaxZ = chunkMinZ + 15;

        BoundingBox chunkBB = new BoundingBox(chunkMinX, chunk.getWorld().getMinHeight(), chunkMinZ, chunkMaxX, chunk.getWorld().getMaxHeight(), chunkMaxZ);

        return boundingBox.overlaps(chunkBB);
    }

    public static void setNameForItemStack(ItemStack itemStack, Component name) {
        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.displayName(name);
        itemStack.setItemMeta(itemMeta);
    }
}