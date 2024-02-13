package ru.darkchronics.quake.game.entities.pickups;

import org.bukkit.*;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BoundingBox;
import ru.darkchronics.quake.QuakePlugin;
import ru.darkchronics.quake.game.entities.DisplayPickup;

public abstract class Spawner implements DisplayPickup {
    QuakePlugin plugin;
    public ItemDisplay display;
    static BoundingBox boundingBox = new BoundingBox(-1, -1, -1, 1, 0, 1);

    public Spawner(ItemStack item, World world, Location location, QuakePlugin plugin) {
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

    public Spawner(ItemDisplay display, QuakePlugin plugin) {
        assert display != null;

        this.plugin = plugin;
        this.display = display;

        // Init rotator
        this.display.setInterpolationDuration(20);
        this.display.setTeleportDuration(20);
        this.display.setRotation(0, 0);

        // Normally here you would plugin.triggers.add(this),
        // but this adds only the super class (i.e. Spawner).
        // Please do so in derived classes instead!
    }

    public abstract void onPickup(Player player);

    public abstract void respawn();

    public Location getLocation() {
        return this.display.getLocation();
    }

    public ItemDisplay getDisplay() {
        return this.display;
    }

    public BoundingBox getOffsetBoundingBox() {
        return boundingBox;
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
