package ru.darkchronics.quake.game.entities.pickups;

import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import ru.darkchronics.quake.QuakePlugin;
import ru.darkchronics.quake.game.entities.QEntityUtil;

public class AmmoSpawner extends Spawner {
    public static final int[] AMOUNTS = {
            50, // bullets
            10, // shells
            5, // rockets
            60, // battery
            10, // slugs
            30, // cells
            1 // bfg charge
    };
    public static final String[] NAMES = {
            "Bullets",
            "Shells",
            "Rockets",
            "Battery",
            "Slugs",
            "Cells",
            "BFG Charge"
    };
    public static final String[] ALIASES = {
            "bullets",
            "shells",
            "rockets",
            "battery",
            "slugs",
            "cells",
            "bfg"
    };
    private ItemStack itemForRespawn;
    private BukkitTask respawnTask;
    private int ammoType;
    public AmmoSpawner(int ammoType, World world, Location location, QuakePlugin plugin) {
        super(new ItemStack(Material.GUNPOWDER), world, location, plugin);

        ItemStack displayItem = new ItemStack(Material.GUNPOWDER);
        ItemMeta displayItemMeta = displayItem.getItemMeta();
        displayItemMeta.setCustomModelData(ammoType);
        displayItem.setItemMeta(displayItemMeta);

        this.ammoType = ammoType;
        PersistentDataContainer displayData = display.getPersistentDataContainer();
        NamespacedKey ammoTypeKey = new NamespacedKey(super.plugin, "ammoType");
        displayData.set(ammoTypeKey, PersistentDataType.INTEGER, this.ammoType);

        super.display.setItemStack(displayItem);
        QEntityUtil.setEntityType(super.display, "ammo_spawner");

        plugin.triggers.add(this);
    }

    public AmmoSpawner(ItemDisplay display, QuakePlugin plugin) {
        super(display, plugin);

        PersistentDataContainer displayData = display.getPersistentDataContainer();
        NamespacedKey ammoTypeKey = new NamespacedKey(super.plugin, "ammoType");
        this.ammoType = displayData.get(ammoTypeKey, PersistentDataType.INTEGER);

        plugin.triggers.add(this);
    }

    @Override
    public void onPickup(Player player) {
        if (this.display.getItemStack().isEmpty()) return;

        this.itemForRespawn = this.display.getItemStack();
        plugin.userStates.get(player).weaponState.ammo[ammoType] += AMOUNTS[ammoType];

        this.display.setItemStack(new ItemStack(Material.AIR)); // Make invisible
        player.getWorld().playSound(player, "quake.items.ammo.pickup", 0.5f, 1f);
        player.sendActionBar(Component.text(NAMES[ammoType]));

        // Respawn in 40 seconds
        this.respawnTask = new BukkitRunnable() {
            public void run() {
                respawn();
            }
        }.runTaskLater(this.plugin, 20*40);
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
