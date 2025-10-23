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

package com.github.polyzium.quakechasm.game.combat;

import com.github.polyzium.quakechasm.QuakePlugin;
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
import com.github.polyzium.quakechasm.game.combat.powerup.Powerup;
import com.github.polyzium.quakechasm.game.combat.powerup.PowerupType;

import static com.github.polyzium.quakechasm.game.combat.WeaponUtil.damageCustom;

public abstract class ProjectileUtil {
    public static String getProjectileType(Projectile projectile) {
        PersistentDataContainer pdc = projectile.getPersistentDataContainer();
        return pdc.get(new NamespacedKey(QuakePlugin.INSTANCE, "projectile_type"), PersistentDataType.STRING);
    }

    public static void setProjectileType(Projectile projectile, String type) {
        PersistentDataContainer pdc = projectile.getPersistentDataContainer();
        pdc.set(new NamespacedKey(QuakePlugin.INSTANCE, "projectile_type"), PersistentDataType.STRING, type);
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
        loc.getWorld().spawnParticle(Particle.POOF, loc, 16, 0,0,0, 0.25);
        loc.getWorld().spawnParticle(Particle.EXPLOSION, loc, 1, 0,0,0, 0.25, null, true);
        explodeCustom(loc, attacker, impactEntity, 5, 20, 10, 0.8, DamageCause.ROCKET, DamageCause.ROCKET_SPLASH);
    }

    public static void explodeBFG(Location loc, Entity attacker) {
        explodeCustom(loc, attacker, null, 10, 40, 40, 2, DamageCause.BFG, DamageCause.BFG_SPLASH);
        loc.getWorld().playSound(loc, "quake.weapons.bfg.explode", 2, 1);
//        loc.getWorld().spawnParticle(Particle.EXPLOSION, loc, 16, 0,0,0, 0.3);
        loc.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, loc, 1, 0, 0, 0, 0.0, null, true);
        loc.getWorld().spawnParticle(Particle.DUST, loc, 64, 1, 1, 1, 1, new Particle.DustOptions(Color.fromRGB(0x00FF00), 2));
    }

    public static void impactPlasma(ProjectileHitEvent event) {
        ProjectileSource attacker = event.getEntity().getShooter();
        Entity hitEntity = event.getHitEntity();
        if (attacker instanceof LivingEntity attackerEntity) {
            if (hitEntity instanceof LivingEntity hitLivingEntity) {
                hitLivingEntity.setNoDamageTicks(0);
                damageCustom(hitLivingEntity, 6, attackerEntity, DamageCause.PLASMA);
                event.getEntity().remove();
            }

            Location impactLoc = event.getEntity().getLocation();
            explodePlasma(impactLoc, (Entity) attacker, hitEntity, event.getHitBlock() != null);
        }
    }

    public static void explodePlasma(Location loc, Entity attacker, Entity impactEntity, boolean hitBlock) {
        if (hitBlock) {
            loc.getWorld().playSound(loc, "quake.weapons.impact_energy", 0.5f, 1);
            loc.getWorld().spawnParticle(Particle.SMOKE, loc, 16, 0, 0, 0, 0.1);
        }
        explodeCustom(loc, attacker, impactEntity, 2, 6, 6, 0.4, DamageCause.PLASMA, DamageCause.PLASMA_SPLASH);
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
