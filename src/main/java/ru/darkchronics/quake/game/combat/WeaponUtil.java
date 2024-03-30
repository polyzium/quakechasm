package ru.darkchronics.quake.game.combat;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import ru.darkchronics.quake.QuakePlugin;
import ru.darkchronics.quake.QuakeUserState;
import ru.darkchronics.quake.game.combat.powerup.Powerup;
import ru.darkchronics.quake.game.combat.powerup.PowerupType;
import ru.darkchronics.quake.matchmaking.Team;

import java.util.function.BiConsumer;

import static ru.darkchronics.quake.game.combat.ProjectileUtil.*;

public abstract class WeaponUtil {
    public static final int[] PERIODS = {
            2, // machinegun
            20, // shotgun
            16, // rocket
            1, // lightning gun
            30, // railgun
            2, // plasma
            50 // bfg
    };

    public static final int[] DEFAULT_AMMO = {
            100, // machinegun
            10, // shotgun
            10, // rocket
            100, // lightning gun
            10, // railgun
            50, // plasma
            1 // bfg
    };

    public static int WEAPONS_NUM = 7;

    public static int getHoldingWeaponIndex(Player player) {
        if (player.getInventory().getItemInMainHand().getItemMeta() == null) return -1;
        if (!player.getInventory().getItemInMainHand().getItemMeta().hasCustomModelData()) return -1;
        return player.getInventory().getItemInMainHand().getItemMeta().getCustomModelData();
    }

    private static void applySpread(Vector vector, double spread) {
        // Generate a random angle within the spread range
        double horizontalAngle = Math.toRadians(Math.random() * spread - spread / 2);
        double verticalAngle = Math.toRadians(Math.random() * spread - spread / 2);
        double zAngle = Math.toRadians(Math.random() * spread - spread / 2);

        // Rotate the vector around the Y-axis (horizontal angle)
        vector.rotateAroundY(horizontalAngle);

        // Rotate the vector around the X-axis (vertical angle)
        vector.rotateAroundX(verticalAngle);
        vector.rotateAroundZ(zAngle);

        // Normalize the vector to ensure it retains its original length
        vector.normalize();
    }

    public static RayTraceResult cast(Player player, double spread, double limit) {
        Location loc = player.getEyeLocation();
        Vector look = loc.getDirection();

        applySpread(look, spread);

        return player.getWorld().rayTrace(loc, look, limit, FluidCollisionMode.NEVER, true, 0.35, e -> (
                (e != player && (e instanceof LivingEntity))
        ));
    }

    public static void spawnParticlesLine(Location startLocation, Location endLocation, Particle particle) {
        World world = startLocation.getWorld();
        double density = 4;

        double distance = startLocation.distance(endLocation);
        int particleCount = (int) (distance * density);

        for (int i = 0; i < particleCount; i++) {
            double ratio = (double) i / particleCount;
            double x = startLocation.getX() + ratio * (endLocation.getX() - startLocation.getX());
            double y = startLocation.getY() + ratio * (endLocation.getY() - startLocation.getY());
            double z = startLocation.getZ() + ratio * (endLocation.getZ() - startLocation.getZ());

            Location particleLocation = new Location(world, x, y, z);
            world.spawnParticle(particle, particleLocation, 1, 0, 0, 0, 0);
        }
    }

    public static void railTrail(Location startLocation, Location endLocation) {
        World world = startLocation.getWorld();
        double density = 4;

        double distance = startLocation.distance(endLocation);
        int particleCount = (int) (distance * density);

        for (int i = 0; i < particleCount; i++) {
            double ratio = (double) i / particleCount;
            double x = startLocation.getX() + ratio * (endLocation.getX() - startLocation.getX());
            double y = startLocation.getY() + ratio * (endLocation.getY() - startLocation.getY());
            double z = startLocation.getZ() + ratio * (endLocation.getZ() - startLocation.getZ());

            Location particleLocation = new Location(world, x, y, z);
            world.spawnParticle(Particle.REDSTONE, particleLocation, 1, 0, 0, 0, 0, new Particle.DustOptions(Color.fromRGB(0x00FF00), 1), true);
        }
    }

    public static void bulletImpact(Location loc, Block hitBlock) {
        loc.getWorld().spawnParticle(Particle.BLOCK_CRACK, loc, 8, hitBlock.getBlockData());
        loc.getWorld().spawnParticle(Particle.CRIT, loc, 4, 0, 0, 0, 0.25);

//        loc.getWorld().playSound(loc, Sound.BLOCK_STONE_BREAK, 0.5f, 2);
//        loc.getWorld().playSound(loc, Sound.BLOCK_GRASS_BREAK, 0.1f, 2);
//        loc.getWorld().playSound(loc, Sound.BLOCK_GLASS_BREAK, 0.25f, 2);

        loc.getWorld().playSound(loc, "quake.weapons.impact", 0.5f, 1);
    }

    public static void lightningImpact(Location loc, Block hitBlock) {
        loc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, loc, 8, 0,0,0, 1);

        loc.getWorld().playSound(loc, "quake.weapons.impact_lightning", 0.5f, 1);
    }

    public static void railImpact(Location loc, Block hitBlock) {
        loc.getWorld().spawnParticle(Particle.SMOKE_LARGE, loc, 8, 0,0,0, 0.05);

//        loc.getWorld().playSound(loc, Sound.BLOCK_FIRE_EXTINGUISH, 0.5f, 2);

        loc.getWorld().playSound(loc, "quake.weapons.impact_energy", 0.5f, 1);
    }

    public static RayTraceResult fireHitscan(Player player, double damage, double spread, double limit, DamageCause cause, BiConsumer<Location, Block> impact) {
        RayTraceResult ray = cast(player, spread, limit);
        if (ray == null) {
            Location eyeLocation = player.getEyeLocation().clone();
            Vector look = eyeLocation.getDirection().clone().multiply(limit);
            Vector hitPos = eyeLocation.toVector().clone().add(look);

            return new RayTraceResult(hitPos);
        }

        Block hitBlock = ray.getHitBlock();
        Vector hitPos = ray.getHitPosition();
        Location hitLoc = new Location(player.getWorld(), hitPos.getX(), hitPos.getY(), hitPos.getZ());

        Location playerLoc = player.getLocation();
        playerLoc.setY(playerLoc.y() + player.getHeight()-0.1);

        Entity entity = ray.getHitEntity();
        if (entity instanceof LivingEntity livingEntity) {
            damageCustom(livingEntity, damage, player, cause);
        } else if (entity instanceof EnderCrystal crystal) {
            Location cloc = crystal.getLocation();
            crystal.remove();
            cloc.getWorld().createExplosion(cloc, 4, false, false);
        }

        if (hitBlock != null && !hitBlock.isEmpty() && impact != null) {
            impact.accept(hitLoc, hitBlock);
        };

        return ray;
    }

    public static void damageCustom(LivingEntity victim, double amount, Entity attacker, DamageCause cause) {
        if (cause == null)
            cause = DamageCause.UNKNOWN;

        victim.setNoDamageTicks(0);
        if (victim instanceof Player player && player.getGameMode() != GameMode.CREATIVE)
            QuakePlugin.INSTANCE.userStates.get(player).lastDamage = new DamageData(attacker, amount, cause);

        victim.damage(amount, attacker);
    }

    public static void knockback(Location from, Entity victim, double power) {
        victim.setVelocity(victim.getVelocity().add(from.getDirection().multiply(power)));
    }

    public static boolean hasLineOfSight(Entity viewer, Entity target) {
        Location viewerLocation = viewer.getLocation();
        Location targetLocation = target.getLocation();
        World world = viewer.getWorld();

        Vector direction = targetLocation.toVector().subtract(viewerLocation.toVector()).normalize();

        double maxDistance = viewerLocation.distance(targetLocation)+1;
        RayTraceResult rayTraceResult = world.rayTrace(viewerLocation, direction, maxDistance, FluidCollisionMode.NEVER, true, 0, null);

        return rayTraceResult != null && rayTraceResult.getHitEntity() == target;
    }

    // Actual weapons are here
    public static void fireMachinegun(Player player) {
//        player.getWorld().playSound(player, Sound.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR, 0.5f, 0.5f);
        player.getWorld().playSound(player, "quake.weapons.machinegun.fire", 0.5f, 1);
        fireHitscan(player, 1.4, 2, 256, DamageCause.MACHINEGUN, WeaponUtil::bulletImpact);
    }

    public static void fireShotgun(Player player) {
//        player.getWorld().playSound(player, Sound.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR, 0.5f, 0.5f);
//        player.getWorld().playSound(player, Sound.ENTITY_GENERIC_EXPLODE, 0.25f, 2f);
        player.getWorld().playSound(player, "quake.weapons.shotgun.fire", 0.5f, 1);
        for (int i = 0; i < 11; i++) {
            fireHitscan(player, 2, 8, 256, DamageCause.SHOTGUN, WeaponUtil::bulletImpact);
        }
    }

    public static void fireRocket(Player player) {
        player.getWorld().playSound(player, "quake.weapons.rocket_launcher.fire", 0.5f, 1);

        Location playerLocation = player.getLocation();
        playerLocation.setY(playerLocation.getY() + (player.getHeight() - 0.35));
        RayTraceResult raycast = cast(player, 0, 2);
        if (raycast != null) {
            Location hitLoc = raycast.getHitPosition().toLocation(player.getWorld());
            if (playerLocation.distance(hitLoc) < 1.3) {
                explodeRocket(hitLoc, player, null);
                return;
            }
        }

        Snowball projectile = player.launchProjectile(Snowball.class);
        setProjectileType(projectile, "rocket");
        projectile.setItem(new ItemStack(Material.MAGMA_CREAM));
        projectile.setGravity(false);
        Vector vel = player.getEyeLocation().getDirection().multiply(1);
        projectile.setVelocity(vel);
        // TODO flyby sounds maybe?
//        projectile.getWorld().playSound(projectile, Sound.MUSIC_DISC_PIGSTEP, 1, 1);

        BukkitRunnable particleEmitterRunnable = new BukkitRunnable() {
            private int ticks;
            @Override
            public void run() {
                if (projectile.isDead()) {
                    this.cancel();
                    return;
                }

                if (this.ticks > 100) {
                    projectile.remove();
                    this.cancel();
                    return;
                }

                projectile.setVelocity(vel);

                Location ploc = projectile.getLocation();
                ploc.getWorld().spawnParticle(Particle.SMOKE_LARGE, ploc, 1, 0,0,0, 0.02);

                this.ticks++;
            }
        };
        particleEmitterRunnable.runTaskTimer(Bukkit.getPluginManager().getPlugin("DarkChronics-Quake"), 1, 1);
    }

    public static void fireLightning(Player player, boolean emitSound) {
        if (emitSound)
            player.getWorld().playSound(player, "quake.weapons.lightning_gun.fire", 0.5f, 1);
        RayTraceResult ray = fireHitscan(player, 1.6, 0, 16, DamageCause.LIGHTNING, WeaponUtil::lightningImpact);
        if (ray == null) return;
        if (ray.getHitEntity() != null) {
            Entity victim = ray.getHitEntity();
            knockback(player.getLocation(), victim, 0.25);
        }

        Location playerLoc = player.getLocation();
        playerLoc.setY(playerLoc.y() + (player.getHeight()-0.4));
        Vector hitPos = ray.getHitPosition();
        Location hitLoc = new Location(player.getWorld(), hitPos.getX(), hitPos.getY(), hitPos.getZ());

        spawnParticlesLine(playerLoc, hitLoc, Particle.ELECTRIC_SPARK);

//        Entity entity = ray.getHitEntity();
//        if (entity instanceof LivingEntity livingEntity) {
//            player.teleport(livingEntity);
//            livingEntity.damage(1000, player);
//        }
    }

    public static void firePlasma(Player player) {
//        player.getWorld().playSound(player, Sound.ENTITY_ARROW_SHOOT, 0.5f, 1f);
        player.getWorld().playSound(player, "quake.weapons.plasma.fire", 0.5f, 1f);

        Location loc = player.getLocation();
        loc.setY(loc.y() + player.getHeight()-0.35);

        Snowball projectile = player.launchProjectile(Snowball.class);
        setProjectileType(projectile, "plasma");
        projectile.setShooter(player);
//        projectile.setItem(new ItemStack(Material.MAGMA_CREAM));
        projectile.setGravity(false);
        Vector vel = player.getEyeLocation().getDirection().multiply(1.75);
        projectile.setVelocity(vel);

        BukkitRunnable particleEmitterRunnable = new BukkitRunnable() {
            private int ticks;
            @Override
            public void run() {
                if (projectile.isDead()) {
                    this.cancel();
                    return;
                }

                if (this.ticks > 100) {
                    projectile.remove();
                    this.cancel();
                    return;
                }

                projectile.setVelocity(vel);

                Location ploc = projectile.getLocation();
                ploc.getWorld().spawnParticle(Particle.REDSTONE, ploc, 1, 0,0,0, new Particle.DustOptions(Color.fromRGB(0x00FFFF), 1));
//                ploc.getWorld().spawnParticle(Particle.SMOKE_LARGE, ploc, 1, 0,0,0, 0.02);

                this.ticks++;
            }
        };
       particleEmitterRunnable.runTaskTimer(Bukkit.getPluginManager().getPlugin("DarkChronics-Quake"), 1, 1);
    }

    public static void fireRailgun(Player player) {
//        player.getWorld().playSound(player, Sound.BLOCK_BEACON_DEACTIVATE, 0.5f, 1f);
//        player.getWorld().playSound(player, Sound.BLOCK_BEACON_ACTIVATE, 0.5f, 1f);
        player.getWorld().playSound(player, "quake.weapons.railgun.fire", 0.5f, 1);
        RayTraceResult ray = fireHitscan(player, 20, 0, 256, DamageCause.RAILGUN, WeaponUtil::railImpact);
        if (ray == null) return;
        if (ray.getHitEntity() != null) {
            Entity victim = ray.getHitEntity();
            knockback(player.getLocation(), victim, 1.5);
        }
        Location playerLoc = player.getLocation();
        playerLoc.setY(playerLoc.y() + (player.getHeight()-0.4));
        Vector hitPos = ray.getHitPosition();
        Location hitLoc = new Location(player.getWorld(), hitPos.getX(), hitPos.getY(), hitPos.getZ());

        railTrail(playerLoc, hitLoc);

//        Entity entity = ray.getHitEntity();
//        if (entity instanceof LivingEntity livingEntity) {
//            player.teleport(livingEntity);
//            livingEntity.damage(1000, player);
//        }
    }

    public static void fireBFG(Player player) {
        // Play charge + fire sound
        player.getWorld().playSound(player, "quake.weapons.bfg.fire", 1, 1);

        // Wait 16 ticks, then fire
        new BukkitRunnable() {
            @Override
            public void run() {
                fireBFGGuts(player);
                if (Powerup.hasPowerup(player, PowerupType.QUAD_DAMAGE))
                    player.getWorld().playSound(player, "quake.items.powerups.quad_damage.fire", 0.5f, 1f);
            }
        }.runTaskLater(QuakePlugin.INSTANCE, 18);
    }

    private static void fireBFGGuts(Player player) {
        Location playerLocation = player.getLocation();
        playerLocation.setY(playerLocation.getY() + (player.getHeight() - 0.35));
        RayTraceResult raycast = cast(player, 0, 2);
        if (raycast != null) {
            Location hitLoc = raycast.getHitPosition().toLocation(player.getWorld());
            if (playerLocation.distance(hitLoc) < 1.3) {
                explodeBFG(hitLoc, player);
                return;
            }
        }

        Snowball projectile = player.launchProjectile(Snowball.class);
        setProjectileType(projectile, "bfg");
        ItemDisplay display = (ItemDisplay) player.getWorld().spawnEntity(playerLocation, EntityType.ITEM_DISPLAY);
        display.setItemStack(new ItemStack(Material.SLIME_BLOCK));
        projectile.addPassenger(display);
        projectile.setItem(new ItemStack(Material.AIR));
        projectile.setGravity(false);
        Vector vel = player.getEyeLocation().getDirection().multiply(0.5);
        projectile.setVelocity(vel);
//        projectile.getWorld().playSound(projectile, Sound.MUSIC_DISC_PIGSTEP, 1, 1);

        BukkitRunnable particleEmitterRunnable = new BukkitRunnable() {
            private int ticks;
            @Override
            public void run() {
                if (projectile.isDead()) {
                    display.remove();
                    this.cancel();
                    return;
                }

                if (this.ticks > 200) {
                    display.remove();
                    projectile.remove();
                    this.cancel();
                    return;
                }

                projectile.setVelocity(vel);

                Location ploc = projectile.getLocation();
                ploc.getWorld().spawnParticle(Particle.COMPOSTER, ploc, 4, 0.5,0.5,0.5, 1);

                if (this.ticks % 2 == 0) {
                    for (Entity entity : ploc.getNearbyEntities(10, 10, 10)) {
                        if (!(entity instanceof LivingEntity livingEntity) || !hasLineOfSight(projectile, livingEntity) || entity == player) continue;
                        if (livingEntity instanceof Player victim) {
                            QuakeUserState attackerState = QuakePlugin.INSTANCE.userStates.get(player);
                            QuakeUserState victimState = QuakePlugin.INSTANCE.userStates.get(victim);

                            if (victimState.currentMatch != null) {
                                if (!(
                                    victimState.currentMatch == attackerState.currentMatch && // same match
                                    (
                                            victimState.currentMatch.getTeamOfPlayer(victim) != attackerState.currentMatch.getTeamOfPlayer(player) || // different teams
                                                    (victimState.currentMatch.getTeamOfPlayer(victim) == Team.FREE && attackerState.currentMatch.getTeamOfPlayer(player) == Team.FREE) // ...or free
                                    )
                                )) {
                                    continue;
                                }
                            }
                        }

                        damageCustom(livingEntity, 1, player, DamageCause.BFG_RAY);
                        spawnParticlesLine(ploc, livingEntity.getEyeLocation(), Particle.ELECTRIC_SPARK);
                        livingEntity.getWorld().playSound(livingEntity.getLocation(), "quake.weapons.bfg.laser", 0.5f, 1);
                    }
                }

                this.ticks++;
            }
        };
        particleEmitterRunnable.runTaskTimer(Bukkit.getPluginManager().getPlugin("DarkChronics-Quake"), 1, 1);
    }
}
