package ru.darkchronics.quake.game.weapons;

import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

public abstract class WeaponsHandler {
    public static RayTraceResult cast(Player player) {
        Location loc = player.getLocation();
        Vector look = player.getEyeLocation().getDirection();

        loc.setY(loc.y() + 1.7);

        return player.getWorld().rayTrace(loc, look, 256, FluidCollisionMode.NEVER, true, 0, e -> (e != player));
    }

    public static void bulletImpact(Location loc, Block hitBlock) {
        loc.getWorld().spawnParticle(Particle.BLOCK_CRACK, loc, 8, hitBlock.getBlockData());
        loc.getWorld().spawnParticle(Particle.CRIT, loc, 4, 0, 0, 0, 0.25);

        loc.getWorld().playSound(loc, Sound.BLOCK_STONE_BREAK, 0.5f, 2);
        loc.getWorld().playSound(loc, Sound.BLOCK_GRASS_BREAK, 0.1f, 2);
        loc.getWorld().playSound(loc, Sound.BLOCK_GLASS_BREAK, 0.25f, 2);
    }

    // Actual weapons are here
    public static void fireHMG(Player player) {
        player.getWorld().playSound(player, Sound.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR, 0.5f, 0.5f);

        RayTraceResult ray = cast(player);
        if (ray == null) return;

        Block hitBlock = ray.getHitBlock();
        Vector hitPos = ray.getHitPosition();
        Location hitLoc = new Location(player.getWorld(), hitPos.getX(), hitPos.getY(), hitPos.getZ());

        Entity entity = ray.getHitEntity();
        if (entity instanceof LivingEntity livingEntity) {
            livingEntity.damage(4, player);
        }

        if (hitBlock != null && !hitBlock.isEmpty()) {
            bulletImpact(hitLoc, hitBlock);
        }
    }
}
