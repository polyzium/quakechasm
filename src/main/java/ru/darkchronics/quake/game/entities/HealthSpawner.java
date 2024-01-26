package ru.darkchronics.quake.game.entities;

import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import ru.darkchronics.quake.QuakePlugin;

public class HealthSpawner extends ItemSpawner {
    private int health;
    public HealthSpawner(int health, World world, Location location, QuakePlugin plugin) {
        super(new ItemStack(Material.STONE), world, location, plugin);

        ItemStack item = null;
        switch (health) {
            case 1:
                item = new ItemStack(Material.CARROT);
                break;
            case 5:
                item = new ItemStack(Material.BAKED_POTATO);
                break;
            case 10:
                item = new ItemStack(Material.COOKED_BEEF);
                break;
        }

        this.health = health;
        PersistentDataContainer displayData = display.getPersistentDataContainer();
        NamespacedKey healthKey = new NamespacedKey(super.plugin, "health");
        displayData.set(healthKey, PersistentDataType.INTEGER, this.health);

        super.display.setItemStack(item);
        QEntityUtil.setEntityType(super.display, "health_spawner");

        NamespacedKey itemKey = new NamespacedKey(plugin, "spawner_item");
        displayData.set(itemKey, PersistentDataType.BYTE_ARRAY, item.serializeAsBytes());
    }

    public HealthSpawner(ItemDisplay display, QuakePlugin plugin) {
        super(display, plugin);

        PersistentDataContainer displayData = display.getPersistentDataContainer();
        NamespacedKey healthKey = new NamespacedKey(super.plugin, "health");
        this.health = displayData.get(healthKey, PersistentDataType.INTEGER);
    }

    @Override
    public void onPickup(Player player) {
        // TODO move common code of Item- and HealthSpawner to elsewhere
        if (this.display.getItemStack().isEmpty() || player.getHealth() == player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue())
            return;

        PersistentDataContainer displayData = display.getPersistentDataContainer();
        NamespacedKey intervalKey = new NamespacedKey(super.plugin, "spawner_item");
        byte[] itemData = displayData.get(intervalKey, PersistentDataType.BYTE_ARRAY);
        ItemStack item = ItemStack.deserializeBytes(itemData);

        double totalHealth = player.getHealth() + this.health;
        if (totalHealth > 20) {
            totalHealth = 20;
        }
        player.setHealth(totalHealth);
        player.sendHealthUpdate();

        this.display.setItemStack(new ItemStack(Material.AIR)); // Make invisible
        player.getWorld().playSound(player, "quake.items.health.pickup_"+this.health, 0.5f, 1f);
        player.sendActionBar(Component.text("Picked up ").append(Component.text(this.health)).append(Component.text(" health")));

        // Respawn in 35 seconds
        new BukkitRunnable() {
            public void run() {
                display.setItemStack(item);
                display.getWorld().spawnParticle(Particle.SPELL_INSTANT, display.getLocation(), 16, 0.5, 0.5, 0.5);
                display.getWorld().playSound(display, "quake.items.respawn", 0.5f, 1f);
            }
        }.runTaskLater(this.plugin, 20*35);
    }
}
