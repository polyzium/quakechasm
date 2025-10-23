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

package com.github.polyzium.quakechasm.events.listeners;

import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.*;
import org.bukkit.util.Vector;
import com.github.polyzium.quakechasm.QuakePlugin;
import com.github.polyzium.quakechasm.QuakeUserState;
import com.github.polyzium.quakechasm.game.combat.DamageCause;
import com.github.polyzium.quakechasm.game.movement.StrafeJumpHandler;
import com.github.polyzium.quakechasm.matchmaking.MatchmakingManager;

import static com.github.polyzium.quakechasm.game.combat.WeaponUtil.damageCustom;

public class MiscListener implements Listener {
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player joinedPlayer = event.getPlayer();
        for (QuakeUserState userState : QuakePlugin.INSTANCE.userStates.values()) {
            if (userState.currentMatch != null)
                userState.getPlayer().unlistPlayer(joinedPlayer);
        }

        QuakePlugin.INSTANCE.initPlayer(joinedPlayer);

        joinedPlayer.teleport(QuakePlugin.LOBBY);
    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        QuakeUserState userState = QuakePlugin.INSTANCE.userStates.get(player);
        if (userState.currentMatch != null) userState.currentMatch.leave(player);
        if (userState.mmState.currentPendingMatch != null) userState.mmState.currentPendingMatch.cancel();
        if (MatchmakingManager.INSTANCE.findPendingParty(player) != null) MatchmakingManager.INSTANCE.stopSearching(player);
        userState.mmState.currentParty.removePlayer(player);

        QuakePlugin.INSTANCE.userStates.remove(player);

        player.teleport(QuakePlugin.LOBBY);
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        // Set fall damage to always deal 2 hearts no matter what (repro of -10 HP on fall in Quake)
        if (!(event.getEntityType() == EntityType.PLAYER && event.getCause() == EntityDamageEvent.DamageCause.FALL)) return;
        if (event.getEntity().getFallDistance() < 10)
            event.setCancelled(true);
        event.setDamage(2);
    }

    // Strafejumping
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        
        // Apply strafe acceleration when airborne and moving
        Vector velocity = event.getTo().toVector().subtract(event.getFrom().toVector());

        StrafeJumpHandler.applyStrafeAcceleration(player, velocity);
    }

    // Telefrag: kill any entity at the teleport destination
    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player teleportingPlayer = event.getPlayer();

        for (Entity entity : event.getTo().getWorld().getNearbyEntities(event.getTo(), 1.5, 2.0, 1.5)) {
            if (entity.equals(teleportingPlayer)) continue;
            if (!(entity instanceof LivingEntity victim)) continue;

            damageCustom(victim, 1000, teleportingPlayer, DamageCause.TELEFRAG);
        }
    }

}
