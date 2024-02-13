package ru.darkchronics.quake.game.entities.pickups;

import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import ru.darkchronics.quake.QuakePlugin;
import ru.darkchronics.quake.game.entities.QEntityUtil;

public class HealthSpawner extends Spawner {
    private ItemStack itemForRespawn;
    private BukkitTask respawnTask;
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
            case 20:
                item = new ItemStack(Material.GOLDEN_APPLE);
                break;
        }

        this.health = health;
        PersistentDataContainer displayData = display.getPersistentDataContainer();
        NamespacedKey healthKey = new NamespacedKey(super.plugin, "health");
        displayData.set(healthKey, PersistentDataType.INTEGER, this.health);

        super.display.setItemStack(item);
        QEntityUtil.setEntityType(super.display, "health_spawner");

        plugin.triggers.add(this);
    }

    public HealthSpawner(ItemDisplay display, QuakePlugin plugin) {
        super(display, plugin);

        PersistentDataContainer displayData = display.getPersistentDataContainer();
        NamespacedKey healthKey = new NamespacedKey(super.plugin, "health");
        this.health = displayData.get(healthKey, PersistentDataType.INTEGER);

        plugin.triggers.add(this);
    }

    @Override
    public void onPickup(Player player) {
        boolean isMegaOrSmall = this.health == 20 || this.health == 1;
        if (
                this.display.getItemStack().isEmpty() ||
                        (isMegaOrSmall && player.getHealth() == 40) ||
                        (!isMegaOrSmall && player.getHealth() >= 20)
        )
            return;

        this.itemForRespawn = this.display.getItemStack();

        double totalHealth = player.getHealth() + this.health;
        if (isMegaOrSmall) {
            if (totalHealth > 40) totalHealth = 40;
            if (totalHealth > 20)
                player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(totalHealth);
        } else if (totalHealth > 20) {
            totalHealth = 20;
        }
        player.setHealth(totalHealth);
        player.sendHealthUpdate();
        EntityRegainHealthEvent event = new EntityRegainHealthEvent(player, this.health, EntityRegainHealthEvent.RegainReason.CUSTOM);
        event.callEvent();

        this.display.setItemStack(new ItemStack(Material.AIR)); // Make invisible
        player.getWorld().playSound(player, "quake.items.health.pickup_"+this.health, 0.5f, 1f);
        if (this.health == 20)
            player.sendActionBar(Component.text("Mega Health"));
        else
            player.sendActionBar(Component.text(this.health).append(Component.text(" Health")));

        // Respawn in 35 seconds
        this.respawnTask = new BukkitRunnable() {
            public void run() {
                respawn();
            }
        }.runTaskLater(this.plugin, 20*35);
    }

    @Override
    public void respawn() {
        if (!super.display.getItemStack().isEmpty()) return;

        display.setItemStack(this.itemForRespawn);
        display.getWorld().spawnParticle(Particle.SPELL_INSTANT, display.getLocation(), 16, 0.5, 0.5, 0.5);
        display.getWorld().playSound(display, "quake.items.respawn", 0.5f, 1f);

        this.respawnTask.cancel();
    }
}
