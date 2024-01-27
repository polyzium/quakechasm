package ru.darkchronics.quake.game.entities.pickups;

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
import ru.darkchronics.quake.game.entities.QEntityUtil;

public class HealthSpawner extends SpawnerBase {
    private int health;
    public HealthSpawner(int health, World world, Location location, QuakePlugin plugin) {
        super(new ItemStack(Material.PORKCHOP), world, location, plugin);

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
    }

    public HealthSpawner(ItemDisplay display, QuakePlugin plugin) {
        super(display, plugin);

        PersistentDataContainer displayData = display.getPersistentDataContainer();
        NamespacedKey healthKey = new NamespacedKey(super.plugin, "health");
        this.health = displayData.get(healthKey, PersistentDataType.INTEGER);
    }

    @Override
    public void onPickup(Player player) {
        if (this.display.getItemStack().isEmpty() || player.getHealth() == player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue())
            return;

        ItemStack item = this.display.getItemStack();

        double totalHealth = player.getHealth() + this.health;
        if (totalHealth > 20) {
            totalHealth = 20;
        }
        player.setHealth(totalHealth);
        player.sendHealthUpdate();

        this.display.setItemStack(new ItemStack(Material.AIR)); // Make invisible
        player.getWorld().playSound(player, "quake.items.health.pickup_"+this.health, 0.5f, 1f);
        player.sendActionBar(Component.text(this.health).append(Component.text(" Health")));

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
