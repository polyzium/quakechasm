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
import ru.darkchronics.quake.game.combat.WeaponUserState;
import ru.darkchronics.quake.game.entities.QEntityUtil;
import ru.darkchronics.quake.hud.Hud;

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
    public AmmoSpawner(int ammoType, World world, Location location) {
        super(new ItemStack(Material.GUNPOWDER), world, location);

        ItemStack displayItem = new ItemStack(Material.GUNPOWDER);
        ItemMeta displayItemMeta = displayItem.getItemMeta();
        displayItemMeta.setCustomModelData(ammoType);
        displayItem.setItemMeta(displayItemMeta);

        this.ammoType = ammoType;
        PersistentDataContainer displayData = display.getPersistentDataContainer();
        NamespacedKey ammoTypeKey = new NamespacedKey(QuakePlugin.INSTANCE, "ammoType");
        displayData.set(ammoTypeKey, PersistentDataType.INTEGER, this.ammoType);

        super.display.setItemStack(displayItem);
        QEntityUtil.setEntityType(super.display, "ammo_spawner");

        QuakePlugin.INSTANCE.triggers.add(this);
    }

    public AmmoSpawner(ItemDisplay display) {
        super(display);

        PersistentDataContainer displayData = display.getPersistentDataContainer();
        NamespacedKey ammoTypeKey = new NamespacedKey(QuakePlugin.INSTANCE, "ammoType");
        this.ammoType = displayData.get(ammoTypeKey, PersistentDataType.INTEGER);

        QuakePlugin.INSTANCE.triggers.add(this);
    }

    @Override
    public void onPickup(Player player) {
        WeaponUserState weaponState = QuakePlugin.INSTANCE.userStates.get(player).weaponState;
        if (this.display.getItemStack().isEmpty() || weaponState.ammo[ammoType] >= 200) return;

        this.itemForRespawn = this.display.getItemStack();
        weaponState.ammo[ammoType] += AMOUNTS[ammoType];
        if (weaponState.ammo[ammoType] > 200)
            weaponState.ammo[ammoType] = 200;

        this.display.setItemStack(new ItemStack(Material.AIR)); // Make invisible
        player.getWorld().playSound(player, "quake.items.ammo.pickup", 0.5f, 1f);
        Hud.pickupMessage(player, Component.text(NAMES[ammoType]));

        // Respawn in 40 seconds
        this.respawnTask = new BukkitRunnable() {
            public void run() {
                respawn();
            }
        }.runTaskLater(QuakePlugin.INSTANCE, 20*40);
    }

    @Override
    public void respawn() {
        if (!super.display.getItemStack().isEmpty()) return;

        display.setItemStack(this.itemForRespawn);
        display.getWorld().spawnParticle(Particle.SPELL_INSTANT, display.getLocation(), 16, 0.5, 0.5, 0.5);
        display.getWorld().playSound(display, "quake.items.respawn", 0.5f, 1f);

        if (this.respawnTask != null)
            this.respawnTask.cancel();
    }
}
