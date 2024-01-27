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

public abstract class SpawnerBase implements DisplayPickup {
    QuakePlugin plugin;
    public ItemDisplay display;

    public SpawnerBase(ItemStack item, World world, Location location, QuakePlugin plugin) {
        this.plugin = plugin;

        this.display = (ItemDisplay) world.spawnEntity(location, EntityType.ITEM_DISPLAY);
        this.display.setItemStack(item);

//        QEntityUtil.setEntityType(this.display, "anything_you_want");

        // Init rotator
        this.display.setInterpolationDuration(20);
        this.display.setTeleportDuration(20);
        this.display.setRotation(0, 0);

        plugin.triggers.add(this);
    }

    public SpawnerBase(ItemDisplay display, QuakePlugin plugin) {
        assert display != null;

        this.plugin = plugin;
        this.display = display;

        // Init rotator
        this.display.setInterpolationDuration(20);
        this.display.setTeleportDuration(20);
        this.display.setRotation(0, 0);

        plugin.triggers.add(this);
    }

    public abstract void onPickup(Player player);

    public Location getLocation() {
        return this.display.getLocation();
    }

    public ItemDisplay getDisplay() {
        return this.display;
    }

    public Entity getEntity() {
        return this.display;
    }

    public void remove() {
        this.display.remove();
    }

    public void onTrigger(Entity entity) {
        if (!(entity instanceof Player)) return;
        this.onPickup((Player) entity);
    }
}
