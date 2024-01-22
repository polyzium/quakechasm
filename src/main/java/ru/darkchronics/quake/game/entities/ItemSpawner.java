package ru.darkchronics.quake.game.entities;

import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import ru.darkchronics.quake.QuakePlugin;

public class ItemSpawner implements Pickup {
    private QuakePlugin plugin;
    public ItemDisplay display;
    private ItemStack item;

    public ItemSpawner(ItemStack item, int interval, World world, Location location, QuakePlugin plugin) {
        this.plugin = plugin;

        this.display = (ItemDisplay) world.spawnEntity(location, EntityType.ITEM_DISPLAY);
        this.display.setItemStack(item);
        this.item = item;

        PersistentDataContainer displayData = display.getPersistentDataContainer();
        NamespacedKey intervalKey = new NamespacedKey(plugin, "interval");
        displayData.set(intervalKey, PersistentDataType.INTEGER, interval);

        // Init rotator
        this.display.setInterpolationDuration(20);
        this.display.setTeleportDuration(20);
        this.display.setRotation(0, 0);

        plugin.itemSpawners.add(this);
    }

    public void onPickup(Player player) {
        if (this.display.getItemStack().isEmpty())
            return;

        player.getInventory().addItem(item);
        this.display.setItemStack(new ItemStack(Material.AIR)); // Make invisible
        player.sendActionBar(Component.text("You picked up ").append(this.item.displayName()));

        // Respawn in [interval] seconds
        PersistentDataContainer displayData = display.getPersistentDataContainer();
        NamespacedKey intervalKey = new NamespacedKey(plugin, "interval");
        int interval = displayData.get(intervalKey, PersistentDataType.INTEGER);
        new BukkitRunnable() {
             public void run() {
                display.setItemStack(item);
                display.getWorld().spawnParticle(Particle.SPELL_INSTANT, display.getLocation(), 16, 0.5, 0.5, 0.5);
            }
        }.runTaskLater(this.plugin, interval);
    }

    public void remove() {
        this.display.remove();
    }
}
