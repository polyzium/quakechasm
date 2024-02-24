package ru.darkchronics.quake.game.combat;

import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;
import ru.darkchronics.quake.game.combat.powerup.Powerup;
import ru.darkchronics.quake.game.combat.powerup.PowerupType;

import static ru.darkchronics.quake.game.combat.WeaponUtil.damageCustom;

public abstract class ProjectileUtil {
    public static String getProjectileType(Projectile projectile) {
        PersistentDataContainer pdc = projectile.getPersistentDataContainer();
        return pdc.get(new NamespacedKey("darkchronics-quake", "projectile_type"), PersistentDataType.STRING);
    }

    public static void setProjectileType(Projectile projectile, String type) {
        PersistentDataContainer pdc = projectile.getPersistentDataContainer();
        pdc.set(new NamespacedKey("darkchronics-quake", "projectile_type"), PersistentDataType.STRING, type);
    }

    public static void impactRocket(ProjectileHitEvent event) {
        ProjectileSource attacker = event.getEntity().getShooter();
        Entity hitEntity = event.getHitEntity();
        if (hitEntity instanceof LivingEntity hitLivingEntity) {
            hitLivingEntity.setNoDamageTicks(0);
            event.getEntity().remove();
        }

        Location loc = event.getEntity().getLocation();
//        loc.getWorld().createExplosion(loc, 2f, false, false, (Entity) attacker);
        explodeRocket(loc, (Entity) attacker, hitEntity);
    }

    public static void explodeRocket(Location loc, Entity attacker, Entity impactEntity) {
        loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.7f);
        loc.getWorld().spawnParticle(Particle.EXPLOSION_NORMAL, loc, 16, 0,0,0, 0.25);
        loc.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, loc, 1, 0,0,0, 0.25, null, true);
        explodeCustom(loc, attacker, impactEntity, 5, 20, 10, 0.8, DamageCause.ROCKET, DamageCause.ROCKET_SPLASH);
    }

    public static void explodeBFG(Location loc, Entity attacker) {
        explodeCustom(loc, attacker, null, 10, 40, 40, 2, DamageCause.BFG, DamageCause.BFG_SPLASH);
        loc.getWorld().playSound(loc, "quake.weapons.bfg.explode", 2, 1);
        loc.getWorld().spawnParticle(Particle.EXPLOSION_NORMAL, loc, 16, 0,0,0, 0.3);
        loc.getWorld().spawnParticle(Particle.EXPLOSION_HUGE, loc, 1, 0, 0, 0, 0.0, null, true);
        loc.getWorld().spawnParticle(Particle.REDSTONE, loc, 64, 1, 1, 1, 1, new Particle.DustOptions(Color.fromRGB(0x00FF00), 2));
    }

    public static void impactPlasma(ProjectileHitEvent event) {
        ProjectileSource attacker = event.getEntity().getShooter();
        Entity hitEntity = event.getHitEntity();
        if (hitEntity instanceof LivingEntity hitLivingEntity && attacker instanceof Entity attackerEntity) {
//            hitLivingEntity.setNoDamageTicks(0);
//            hitLivingEntity.damage(4, attackerEntity);
            damageCustom(hitLivingEntity, 4, attackerEntity, DamageCause.PLASMA);
        } else if (event.getHitBlock() != null) {
            Location loc = event.getEntity().getLocation();
//            loc.getWorld().playSound(loc, Sound.BLOCK_FIRE_EXTINGUISH, 0.5f, 2);
            loc.getWorld().playSound(loc, "quake.weapons.impact_energy", 0.5f, 1);
            loc.getWorld().spawnParticle(Particle.SMOKE_NORMAL, loc, 16, 0,0,0, 0.1);
        }
    }

    public static void impactBFG(ProjectileHitEvent event) {
        ProjectileSource attacker = event.getEntity().getShooter();
        Entity hitEntity = event.getHitEntity();
        if (hitEntity instanceof LivingEntity hitLivingEntity) {
            hitLivingEntity.setNoDamageTicks(0);
            event.getEntity().remove();
        }

        Location loc = event.getEntity().getLocation();
        explodeBFG(loc, (Entity) attacker);
    }

    public static void explodeCustom(Location impactLocation, Entity attacker, Entity hitEntity, double explosionRadius, double directDamage, double splashDamage, double knockback, DamageCause directCause, DamageCause splashCause) {
        for (Entity entity : impactLocation.getWorld().getNearbyEntities(impactLocation, explosionRadius, explosionRadius, explosionRadius)) {
            if (!(entity instanceof LivingEntity victim)) continue;

            Location loc = entity.getLocation();
            Location eloc = victim.getEyeLocation();
            double distance = impactLocation.distance(loc);
            double entityDamage = splashDamage * (1.0 - distance / explosionRadius);
            if (entityDamage < 0) continue;
//            attacker.sendMessage(String.format("damage: %.2f, entityDamage: %.2f", splashDamage, entityDamage));
            victim.setNoDamageTicks(0);
            if (
                    attacker instanceof Player pAttacker &&
                            Powerup.hasPowerup(pAttacker, PowerupType.QUAD_DAMAGE)
            ) {
                knockback *= 3;
            }
            if (hitEntity != null && entity == hitEntity)
                damageCustom(victim, directDamage, attacker, directCause);
            else
                damageCustom(victim, entityDamage, attacker, splashCause);

            Vector direction = eloc.toVector().subtract(impactLocation.toVector()).normalize();

            Vector knockbackVector = direction.multiply(knockback);
            Vector currentVelocity = entity.getVelocity();
            entity.setVelocity(currentVelocity.add(knockbackVector));
        }
    }
}
