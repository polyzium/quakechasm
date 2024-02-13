package ru.darkchronics.quake.events;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerVelocityEvent;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import ru.darkchronics.quake.QuakePlugin;

public class MiscListener implements Listener {
    private QuakePlugin plugin;

    public MiscListener(QuakePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        plugin.initPlayer(player);
    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        plugin.userStates.remove(player);
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        // Set fall damage to always deal 2 hearts no matter what (repro of -10 HP on fall in Quake)
        if (!(event.getEntityType() == EntityType.PLAYER && event.getCause() == EntityDamageEvent.DamageCause.FALL)) return;
        if (event.getEntity().getFallDistance() < 10)
            event.setCancelled(true);
        event.setDamage(2);
    }

//    @EventHandler
//    public void onPlayerMove(PlayerMoveEvent event) {
//        Player player = event.getPlayer();
////        Vector velocity = player.getVelocity();
//        Vector velocity2 = event.getTo().clone().subtract(event.getFrom()).toVector();
//        Vector velocity = event.getTo().clone().subtract(event.getFrom()).toVector();
//        velocity.setY(0);
//        velocity.rotateAroundY(Math.PI / 4);
//        velocity.normalize();
////        velocity.normalize();
//        Vector look = player.getEyeLocation().getDirection();
////        player.setVelocity(player.getVelocity().multiply(1+speed/20));
//        double bhopFactor = 1 - Math.abs(look.dot(velocity));
//
//        if (!Double.isNaN(bhopFactor) && !player.isOnGround()) {
//            player.sendActionBar(Component.text(velocity.toString()));
//            Vector v = velocity2;
//            v.setX(v.getX() * (1 + (bhopFactor / 4)));
//            v.setZ(v.getZ() * (1 + (bhopFactor / 4)));
////            v.setX(v.getX()*(1));
////            v.setZ(v.getZ()*(1));
//
//            player.setVelocity(v);
//        }
//    }
}
