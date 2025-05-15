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

package ru.darkchronics.quake.misc;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import ru.darkchronics.quake.misc.adapters.LocationAdapter;
import ru.darkchronics.quake.misc.adapters.WorldAdapter;

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