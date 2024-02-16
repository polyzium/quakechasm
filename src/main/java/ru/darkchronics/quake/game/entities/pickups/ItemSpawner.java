package ru.darkchronics.quake.game.entities.pickups;

import org.bukkit.*;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import ru.darkchronics.quake.QuakePlugin;
import ru.darkchronics.quake.events.CombatListener;
import ru.darkchronics.quake.game.entities.QEntityUtil;

import java.util.Objects;

public class ItemSpawner extends Spawner {
    private ItemStack itemForRespawn;
    private BukkitTask respawnTask;
    public ItemSpawner(ItemStack item, World world, Location location, QuakePlugin plugin) {
        super(item, world, location, plugin);
        QEntityUtil.setEntityType(super.display, "item_spawner");

        plugin.triggers.add(this);
    }

    public ItemSpawner(ItemDisplay display, QuakePlugin plugin) {
        super(display, plugin);

        plugin.triggers.add(this);
    }

    public void onPickup(Player player) {
        ItemStack item = super.display.getItemStack();
        if (item.isEmpty())
            return;

        this.itemForRespawn = item;

        PlayerInventory inv = player.getInventory();
        // Special case for guns (uses carrot-on-a-stick as a base)
        if (item.getItemMeta().hasCustomModelData() && item.getType() == Material.CARROT_ON_A_STICK) {
            CombatListener.sortGun(item, player, this.plugin);
        } else {
            inv.addItem(item);
        }
        super.display.setItemStack(new ItemStack(Material.AIR)); // Make invisible
        player.getWorld().playSound(player, "quake.weapons.pickup", 0.5f, 1f);
        player.sendActionBar(Objects.requireNonNull(item.getItemMeta().displayName()));

        // Respawn in 5 seconds
        // TODO 30 seconds for Team Deathmatch
        this.respawnTask = new BukkitRunnable() {
            public void run() {
                respawn();
            }
        }.runTaskLater(this.plugin, 100);
    }

    @Override
    public void respawn() {
        if (!super.display.getItemStack().isEmpty()) return;

        display.setItemStack(itemForRespawn);
        display.getWorld().spawnParticle(Particle.SPELL_INSTANT, display.getLocation(), 16, 0.5, 0.5, 0.5);
        display.getWorld().playSound(display, "quake.items.respawn", 0.5f, 1f);

        this.respawnTask.cancel();
    }
}
