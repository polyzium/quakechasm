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

import com.github.polyzium.quakechasm.game.entities.triggers.Jumppad;
import com.github.polyzium.quakechasm.misc.MiscUtil;
import com.github.polyzium.quakechasm.misc.TranslationManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.title.Title;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.time.Duration;
import java.util.ArrayList;

import static com.github.polyzium.quakechasm.events.listeners.MapperToolListener.TOOL_UPDATE_TICKS;
import static com.github.polyzium.quakechasm.game.combat.WeaponUtil.spawnParticlesLine;

public class JumppadTool {
    public static Vector calculateLaunchVector(Location jumppadLoc, Location landingLoc, double powerMultiplier) {
        Vector displacement = landingLoc.toVector().subtract(jumppadLoc.toVector());

        Vector horizontalDir = new Vector(displacement.getX(), 0, displacement.getZ());
        double horizontalDist = horizontalDir.length();
        
        if (horizontalDist < 0.1) {
            return new Vector(0, Math.max(0.5, Math.abs(displacement.getY()) * 0.5) * powerMultiplier, 0);
        }
        
        horizontalDir.normalize();

        Vector horizontalVec = horizontalDir.clone().multiply(powerMultiplier);

        double minY = -1.0 * powerMultiplier;
        double maxY = 3.0 * powerMultiplier;
        double yStep = 0.01 * powerMultiplier;
        int maxIterations = (int) Math.ceil((maxY - minY) / yStep);

        final double MARGIN_OF_ERROR = 1.0;

        for (int i = 0; i <= maxIterations; i++) {
            double yComponent = minY + (i * yStep);

            Vector launchVec = horizontalVec.clone();
            launchVec.setY(yComponent);

            ArrayList<Vector> trajectory = MiscUtil.calculateTrajectory(jumppadLoc, launchVec);
            if (trajectory.isEmpty()) continue;
            if (trajectory.size() < 32)
                visualizeTrajectory(jumppadLoc, launchVec, Color.fromRGB(0xFF0000), false, TOOL_UPDATE_TICKS);

            Vector landPos = trajectory.get(trajectory.size() - 1);
            double error = landPos.distance(landingLoc.toVector());
            
            if (error <= MARGIN_OF_ERROR) {
                return launchVec;
            }
        }

        return null;
    }

    public static void visualizeTrajectory(Location jumppadLoc, Vector launchVec, Color color, int durationTicks) {
        ArrayList<Vector> trajectory = MiscUtil.calculateTrajectory(jumppadLoc, launchVec);
        
        Location prevLoc = null;
        for (int i = 0; i < trajectory.size(); i++) {
            Vector vector = trajectory.get(i);
            Location loc = vector.toLocation(jumppadLoc.getWorld());
            
            if (prevLoc == null) {
                jumppadLoc.getWorld().spawnParticle(
                    Particle.TRAIL, 
                    loc, 
                    1, 0, 0, 0, 0, 
                    new Particle.Trail(loc, color, durationTicks)
                );
            } else {
                spawnParticlesLine(prevLoc, loc, 8, particleLocation -> 
                    loc.getWorld().spawnParticle(
                        Particle.TRAIL, 
                        particleLocation, 
                        1, 0, 0, 0, 0, 
                        new Particle.Trail(particleLocation, color, durationTicks),
                        true
                    )
                );
            }
            prevLoc = loc;
        }
    }

    public static void visualizeTrajectory(Location jumppadLoc, Vector launchVec, Color color, boolean precise, int durationTicks) {
        ArrayList<Vector> trajectory = MiscUtil.calculateTrajectory(jumppadLoc, launchVec);

        Location prevLoc = null;
        for (int i = 0; i < trajectory.size(); i++) {
            Vector vector = trajectory.get(i);
            Location loc = vector.toLocation(jumppadLoc.getWorld());

            if (prevLoc == null) {
                jumppadLoc.getWorld().spawnParticle(
                        Particle.TRAIL,
                        loc,
                        1, 0, 0, 0, 0,
                        new Particle.Trail(loc, color, durationTicks)
                );
            } else {
                if (precise)
                    spawnParticlesLine(prevLoc, loc, 8, particleLocation ->
                            loc.getWorld().spawnParticle(
                                    Particle.TRAIL,
                                    particleLocation,
                                    1, 0, 0, 0, 0,
                                    new Particle.Trail(particleLocation, color, durationTicks),
                                    true
                            )
                    );
                else
                    loc.getWorld().spawnParticle(
                            Particle.TRAIL,
                            loc,
                            1, 0, 0, 0, 0,
                            new Particle.Trail(loc, color, durationTicks),
                            true
                    );
            }
            prevLoc = loc;
        }
    }

    public static void displayJumppadInfo(Player player, Jumppad jumppad) {
        Location loc = jumppad.getLocation();
        Vector launchVec = jumppad.getLaunchVec();

        ArrayList<Vector> trajectory = MiscUtil.calculateTrajectory(loc, launchVec);
        Vector landingPos = trajectory.isEmpty() ? loc.toVector() : trajectory.get(trajectory.size() - 1);
        
        player.showTitle(Title.title(
            Component.empty(),
            TranslationManager.t("mapper.tool.jumppad.subtitle.landing", player,
                Placeholder.unparsed("location", String.format("%.1f %.1f %.1f",
                    landingPos.getX(), landingPos.getY(), landingPos.getZ()))),
            Title.Times.times(Duration.ZERO, Duration.ofMillis(500), Duration.ofMillis(200))));
    }

    public static void displayPowerMultiplier(Player player, double powerMultiplier) {
        player.showTitle(Title.title(
            Component.empty(),
            TranslationManager.t("mapper.tool.jumppad.subtitle.power", player,
                Placeholder.unparsed("power", String.format("%.1f", powerMultiplier))),
            Title.Times.times(Duration.ZERO, Duration.ofMillis(500), Duration.ofMillis(200))));
    }
}