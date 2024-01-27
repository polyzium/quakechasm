package ru.darkchronics.quake.events;

import com.destroystokyo.paper.event.entity.EntityKnockbackByEntityEvent;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.darkchronics.quake.QuakePlugin;
import ru.darkchronics.quake.QuakeUserState;
import ru.darkchronics.quake.game.combat.ProjectileUtil;

import java.util.ArrayList;
import java.util.Collection;

import static org.bukkit.craftbukkit.v1_20_R3.inventory.CraftItemStack.getItemMeta;

public class CombatListener implements Listener {
    private final QuakePlugin plugin;

    public CombatListener(QuakePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Player player = event.getPlayer();
        if (player.getInventory().getItemInMainHand().getType() != Material.CARROT_ON_A_STICK) return;

        QuakeUserState state = this.plugin.userStates.get(player);
        state.weaponState.shoot(player);
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (event.getEntityType() != EntityType.SNOWBALL) return;
        event.setCancelled(true);

//        ProjectileUtil.impactBFG(event);
        switch (ProjectileUtil.getProjectileType(event.getEntity())) {
            case "rocket":
                ProjectileUtil.impactRocket(event);
                break;
            case "plasma":
                ProjectileUtil.impactPlasma(event);
                break;
            case "bfg":
                ProjectileUtil.impactBFG(event);
                break;
        }
    }

    @EventHandler
    public void onKnockback(EntityKnockbackByEntityEvent event) {
        // Cancel knockback, because we have our own
        event.setCancelled(true);
    }

    // Play hit sound
    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
//        if (event.getEntityType() != EntityType.PLAYER) return;
        if (!(event.getEntity() instanceof LivingEntity victim)) return;
        if (event.getEntity() == event.getDamager()) return;

        Entity attacker = event.getDamager();
        float health = (float) (victim.getHealth() - event.getDamage());
        attacker.playSound(
                Sound.sound(Key.key("quake.feedback.hit"), Sound.Source.NEUTRAL, 1f, (float) Math.round((1f + (health / 66)) * 6) /6 )
        );
    }

    // Handle 20+ health (for things like Mega Health or Regeneration)
//    @EventHandler
//    public void onEntityDamage(EntityDamageEvent event) {
//
//    }

    // No hunger
    @EventHandler
    public void onHunger(FoodLevelChangeEvent event) {
        event.setCancelled(true);
    }

    // No health regen from food
    @EventHandler
    public void onRegen(EntityRegainHealthEvent event) {
        if (event.getRegainReason() == EntityRegainHealthEvent.RegainReason.SATIATED)
            event.setCancelled(true);
    }

    // Respawn on red carpets (they act as spawnpoints)
    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        World world = Bukkit.getWorld("flat");
        ArrayList<Block> carpets = new ArrayList<>(16);
        for (Chunk chunk : world.getLoadedChunks()) {
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    for (int y = 0; y < 256; y++) {
                        Block block = chunk.getBlock(x, y, z);
                        if (block.getType() == Material.RED_CARPET) {
                            carpets.add(block);
                        }
                    }
                }
            }
        }

        Location spawnPoint = carpets.get(
                (int) (Math.random() * (carpets.size() - 1))
        ).getLocation();
        spawnPoint.setX(spawnPoint.x()+0.5);
        spawnPoint.setZ(spawnPoint.z()+0.5);

        ItemStack machinegun = new ItemStack(Material.CARROT_ON_A_STICK);
        ItemMeta mgMeta = machinegun.getItemMeta();
        mgMeta.setCustomModelData(0);
        mgMeta.displayName(Component.text("Machinegun"));
        machinegun.setItemMeta(mgMeta);

        event.getPlayer().getInventory().addItem(machinegun);
        event.setRespawnLocation(spawnPoint);
    }

    // No inventory drop
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e) {
        ItemStack handItem = e.getPlayer().getInventory().getItemInMainHand();
        final Collection<ItemStack> drops = e.getDrops();
        if (!drops.isEmpty()) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    Entity[] entities = e.getEntity().getChunk().getEntities();
                    for (ItemStack item : drops) {
                        for (Entity entity : entities) {
                            if (entity.getType() == EntityType.DROPPED_ITEM) {
                                if (entity instanceof Item) {
                                    if (item.isSimilar(((Item) entity).getItemStack()) && !handItem.isSimilar(((Item) entity).getItemStack())) {
                                        entity.remove();
                                    }
                                }
                            }
                        }
                    }
                }
            }.runTaskLater(this.plugin, 1L);
        }
    }
}
