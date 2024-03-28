package ru.darkchronics.quake.events.listeners;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.*;
import ru.darkchronics.quake.QuakePlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3d;
import ru.darkchronics.quake.QuakePlugin;
import ru.darkchronics.quake.QuakeUserState;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import static ru.darkchronics.quake.misc.MiscUtil.AIR_DRAG;
import static ru.darkchronics.quake.misc.MiscUtil.GRAVITY;

public class MiscListener implements Listener {
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player joinedPlayer = event.getPlayer();
        for (QuakeUserState userState : QuakePlugin.INSTANCE.userStates.values()) {
            if (userState.currentMatch != null)
                userState.getPlayer().unlistPlayer(joinedPlayer);
        }

        QuakePlugin.INSTANCE.initPlayer(joinedPlayer);
    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        QuakeUserState userState = QuakePlugin.INSTANCE.userStates.get(player);
        if (userState.currentMatch != null) {
            userState.currentMatch.leave(player);
        }

        QuakePlugin.INSTANCE.userStates.remove(player);
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        // Set fall damage to always deal 2 hearts no matter what (repro of -10 HP on fall in Quake)
        if (!(event.getEntityType() == EntityType.PLAYER && event.getCause() == EntityDamageEvent.DamageCause.FALL)) return;
        if (event.getEntity().getFallDistance() < 10)
            event.setCancelled(true);
        event.setDamage(2);
    }

    // Bunnyhop/strafejumping (EXPERIMENTAL)
//    @EventHandler
//    public void onPlayerMove(PlayerMoveEvent event) {
//        if (!event.hasChangedOrientation()) return;
//
//        Player player = event.getPlayer();
//        Vector fromDir = event.getFrom().getDirection().clone();
//        Vector toDir = event.getTo().getDirection().clone();
//        Vector dirChange = toDir.clone().subtract(fromDir);
//        double bhopFactor = dirChange.length()-0.2;
//        if (bhopFactor < 0) return;
//        if (bhopFactor > 0.2) bhopFactor = 0.2;
//
//        Vector velocity = event.getTo().clone().subtract(event.getFrom()).toVector();
//        Vector addVelocity = velocity.clone().normalize().multiply(bhopFactor);
//        if (!player.isFlying())
//            addVelocity.setY(addVelocity.getY() - GRAVITY);
//        else
//            addVelocity.setY(0);
//        if (velocity.length() > 0 && !player.isOnGround()) {
//            velocity.add(addVelocity);
//            player.setVelocity(velocity);
//        }
//
////        player.sendMessage(String.valueOf(dirChange));
//    }


    // Cancel vanilla Minecraft air drag (EXPERIMENTAL)
//    @EventHandler
//    public void onPlayerMove(PlayerMoveEvent event) {
//        Player player = event.getPlayer();
//        Vector velocity = event.getTo().clone().subtract(event.getFrom()).toVector();
//        if (!(velocity.length() > 0 && !player.isOnGround()))
//            return;
//
//        Vector newVelocity = velocity.clone();
//        if (!player.isFlying())
//            newVelocity.setY(newVelocity.getY() - GRAVITY);
//        else
//            newVelocity.setY(0);
//
//        player.setVelocity(newVelocity);
//    }

}
