package ru.darkchronics.quake.game.entities;

import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import ru.darkchronics.quake.QuakePlugin;

public class ItemSpawner extends SpawnerBase {
    public ItemSpawner(ItemStack item, World world, Location location, QuakePlugin plugin) {
        super(item, world, location, plugin);
        QEntityUtil.setEntityType(super.display, "item_spawner");
    }

    public ItemSpawner(ItemDisplay display, QuakePlugin plugin) {
        super(display, plugin);
    }

    public void onPickup(Player player) {
        if (super.display.getItemStack().isEmpty())
            return;

        ItemStack item = super.display.getItemStack();
        player.getInventory().addItem(item);
        super.display.setItemStack(new ItemStack(Material.AIR)); // Make invisible
        player.getWorld().playSound(player, "quake.weapons.pickup", 0.5f, 1f);
        player.sendActionBar(Component.text("You picked up ").append(item.displayName()));

        // Respawn in 5 seconds
        // TODO 30 seconds for Team Deathmatch
        new BukkitRunnable() {
             public void run() {
                display.setItemStack(item);
                display.getWorld().spawnParticle(Particle.SPELL_INSTANT, display.getLocation(), 16, 0.5, 0.5, 0.5);
                display.getWorld().playSound(display, "quake.items.respawn", 0.5f, 1f);
            }
        }.runTaskLater(this.plugin, 100);
    }
}
