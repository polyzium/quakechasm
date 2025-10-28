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

package com.github.polyzium.quakechasm.game.movement;

import com.github.polyzium.quakechasm.QuakeUserState;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;

import java.time.Duration;

import static com.github.polyzium.quakechasm.misc.MiscUtil.AIR_DRAG;
import static com.github.polyzium.quakechasm.misc.MiscUtil.GRAVITY;

// How to use: press sprint, hold down W and then look left or right
public class StrafeJumpHandler {
    private static final int TICK_INTERVAL = 5; // Apply velocity every 5 ticks to reduce ACK delays
    private static final double AIR_ACCELERATION = 0.4*TICK_INTERVAL;
    private static final double MAX_SPEED_MULTIPLIER = 1.8*TICK_INTERVAL;
    private static final double ANGLE_THRESHOLD = 0.975;

    public static void applyStrafeAcceleration(Player player, QuakeUserState userState, Vector velocity) {
        if (player.isOnGround() || player.isFlying() || player.isSneaking() || !player.isSprinting()) {
            return;
        }

        Vector horizontalVel = velocity.clone().setY(0);

        if (horizontalVel.lengthSquared() < 0.01) {
            return;
        }

        Vector lookDir = player.getEyeLocation().getDirection();

        double alignment = lookDir.dot(horizontalVel.normalize());

        // TODO possibly ping adaptive strafejumping algorithm
        if (Math.abs(alignment) < ANGLE_THRESHOLD && userState.strafeJumpTicks % TICK_INTERVAL == 0) {
            double baseSpeed = player.getWalkSpeed() * TICK_INTERVAL;
            double currentSpeed = horizontalVel.length();
            double maxSpeed = baseSpeed * MAX_SPEED_MULTIPLIER;

            if (currentSpeed >= maxSpeed) {
                return;
            }

            double accelFactor = (ANGLE_THRESHOLD - Math.abs(alignment)) / ANGLE_THRESHOLD;

            lookDir.setY(0);
            lookDir.normalize();
            Vector acceleration = lookDir.multiply(AIR_ACCELERATION * accelFactor);

            velocity.add(acceleration);

            Vector newHorizontalVel = velocity.clone().setY(0);
            if (newHorizontalVel.length() > maxSpeed) {
                newHorizontalVel.normalize().multiply(maxSpeed);
                velocity.setX(newHorizontalVel.getX());
                velocity.setZ(newHorizontalVel.getZ());
            }

            velocity.setY(velocity.getY() - GRAVITY);
            velocity.setX(velocity.getX() * 0.95);
            velocity.setZ(velocity.getZ() * 0.95);

            player.setVelocity(velocity);
        }
    }
}