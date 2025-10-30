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

package com.github.polyzium.quakechasm.game.mapper;

import com.github.polyzium.quakechasm.QuakePlugin;
import com.github.polyzium.quakechasm.game.combat.WeaponUtil;
import com.github.polyzium.quakechasm.game.entities.QEntityUtil;
import com.github.polyzium.quakechasm.game.entities.Trigger;
import com.github.polyzium.quakechasm.misc.TranslationManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.title.Title;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.time.Duration;

import static com.github.polyzium.quakechasm.events.listeners.MapperToolListener.TOOL_UPDATE_TICKS;

public class EntityTool {
    public static Trigger getTargetEntity(Player player) {
        Location eyeLocation = player.getEyeLocation();
        org.bukkit.util.Vector direction = eyeLocation.getDirection();
        double maxDistance = 10.0;
        
        Trigger closestTrigger = null;
        double closestDistance = maxDistance;

        for (Trigger trigger : QuakePlugin.INSTANCE.triggers) {
            BoundingBox offsetBox = trigger.getOffsetBoundingBox();
            if (offsetBox == null) continue;

            Location entityLoc = trigger.getLocation();
            org.bukkit.util.Vector entityPos = entityLoc.toVector();

            BoundingBox worldBox = offsetBox.clone().shift(entityPos);
            
            org.bukkit.util.RayTraceResult rayTrace = worldBox.rayTrace(
                eyeLocation.toVector(),
                direction,
                maxDistance
            );
            
            if (rayTrace != null) {
                double distance = eyeLocation.toVector().distance(rayTrace.getHitPosition());
                if (distance < closestDistance) {
                    closestDistance = distance;
                    closestTrigger = trigger;
                }
            }
        }
        
        return closestTrigger;
    }

    public static void displayEntityInfo(Player player, Trigger trigger) {
        if (trigger == null) {
            player.showTitle(Title.title(
                Component.empty(),
                Component.text(TranslationManager.tLegacy("mapper.tool.entity.subtitle.noEntity", player)).color(NamedTextColor.GRAY),
                Title.Times.times(Duration.ZERO, Duration.ofMillis(500), Duration.ofMillis(200))
            ));
            return;
        }
        
        Entity entity = trigger.getEntity();
        String entityType = QEntityUtil.getEntityType(entity);
        Location loc = trigger.getLocation();
        
        Component subtitle = TranslationManager.t("mapper.tool.entity.subtitle.entityInfo", player,
            Placeholder.unparsed("entity_type", entityType != null ? entityType : "unknown"),
            Placeholder.unparsed("location", String.format("%.1f %.1f %.1f", loc.x(), loc.y(), loc.z())));
        
        player.showTitle(Title.title(
            Component.empty(),
            subtitle,
            Title.Times.times(Duration.ZERO, Duration.ofMillis(500), Duration.ofMillis(200))
        ));
    }

    public static void removeEntity(Player player, Trigger trigger) {
        if (trigger == null) {
            player.sendMessage(TranslationManager.t("mapper.tool.entity.message.noEntity", player)
                .color(NamedTextColor.RED));
            return;
        }
        
        String entityType = QEntityUtil.getEntityType(trigger.getEntity());
        Location loc = trigger.getLocation();
        
        trigger.remove();
        QuakePlugin.INSTANCE.triggers.remove(trigger);
        
        player.sendMessage(TranslationManager.t("mapper.tool.entity.message.removed", player,
            Placeholder.unparsed("entity_type", entityType != null ? entityType : "entity"),
            Placeholder.unparsed("location", String.format("%.1f %.1f %.1f", loc.x(), loc.y(), loc.z())))
            .color(NamedTextColor.GREEN));
    }

    public static void startMovingEntity(Player player, Trigger trigger) {
        if (trigger == null) {
            player.sendMessage(TranslationManager.t("mapper.tool.entity.message.noEntityMove", player)
                .color(NamedTextColor.RED));
            return;
        }
        
        String entityType = QEntityUtil.getEntityType(trigger.getEntity());
        player.sendMessage(TranslationManager.t("mapper.tool.entity.message.startedMoving", player,
            Placeholder.unparsed("entity_type", entityType != null ? entityType : "entity"))
            .color(NamedTextColor.GREEN));
    }

    public static void stopMovingEntity(Player player, Trigger trigger, Location newLocation) {
        if (trigger == null) {
            player.sendMessage(TranslationManager.t("mapper.tool.entity.message.noEntityMoving", player)
                .color(NamedTextColor.RED));
            return;
        }
        
        Entity entity = trigger.getEntity();
        entity.teleport(newLocation);
        
        String entityType = QEntityUtil.getEntityType(entity);
        player.sendMessage(TranslationManager.t("mapper.tool.entity.message.placed", player,
            Placeholder.unparsed("entity_type", entityType != null ? entityType : "entity"),
            Placeholder.unparsed("location", String.format("%.1f %.1f %.1f", newLocation.x(), newLocation.y(), newLocation.z())))
            .color(NamedTextColor.GREEN));
    }

    public static void drawBoundingBox(World world, BoundingBox box, Color color, int duration, double density) {
        Vector min = box.getMin();
        Vector max = box.getMax();

        drawLine(world, min.getX(), min.getY(), min.getZ(), max.getX(), min.getY(), min.getZ(), color, duration, density);
        drawLine(world, max.getX(), min.getY(), min.getZ(), max.getX(), min.getY(), max.getZ(), color, duration, density);
        drawLine(world, max.getX(), min.getY(), max.getZ(), min.getX(), min.getY(), max.getZ(), color, duration, density);
        drawLine(world, min.getX(), min.getY(), max.getZ(), min.getX(), min.getY(), min.getZ(), color, duration, density);

        drawLine(world, min.getX(), max.getY(), min.getZ(), max.getX(), max.getY(), min.getZ(), color, duration, density);
        drawLine(world, max.getX(), max.getY(), min.getZ(), max.getX(), max.getY(), max.getZ(), color, duration, density);
        drawLine(world, max.getX(), max.getY(), max.getZ(), min.getX(), max.getY(), max.getZ(), color, duration, density);
        drawLine(world, min.getX(), max.getY(), max.getZ(), min.getX(), max.getY(), min.getZ(), color, duration, density);

        drawLine(world, min.getX(), min.getY(), min.getZ(), min.getX(), max.getY(), min.getZ(), color, duration, density);
        drawLine(world, max.getX(), min.getY(), min.getZ(), max.getX(), max.getY(), min.getZ(), color, duration, density);
        drawLine(world, max.getX(), min.getY(), max.getZ(), max.getX(), max.getY(), max.getZ(), color, duration, density);
        drawLine(world, min.getX(), min.getY(), max.getZ(), min.getX(), max.getY(), max.getZ(), color, duration, density);
    }

    public static void drawLine(World world, double x1, double y1, double z1, double x2, double y2, double z2, Color color, int duration, double density) {
        Location start = new Location(world, x1, y1, z1);
        Location end = new Location(world, x2, y2, z2);
        
        WeaponUtil.spawnParticlesLine(start, end, density, loc -> {
            world.spawnParticle(
                Particle.TRAIL,
                loc,
                1,
                0, 0, 0,
                0,
                new Particle.Trail(loc, color, duration),
                true
            );
        });
    }

    public static void visualizeTargetEntity(Player player, Trigger trigger) {
        visualizeEntityWithColor(player, trigger, Color.fromRGB(0xFF0000));
    }

    public static void visualizeEntityWithColor(Player player, Trigger trigger, Color color) {
        if (trigger == null) return;
        
        BoundingBox offsetBox = trigger.getOffsetBoundingBox();
        if (offsetBox == null) return;
        
        Location entityLoc = trigger.getLocation();
        Vector entityPos = entityLoc.toVector();
        BoundingBox worldBox = offsetBox.clone().shift(entityPos);

        drawBoundingBox(player.getWorld(), worldBox, color, TOOL_UPDATE_TICKS, 16);
    }
}