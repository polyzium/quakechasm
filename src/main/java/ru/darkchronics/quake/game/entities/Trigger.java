package ru.darkchronics.quake.game.entities;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.util.BoundingBox;

public interface Trigger {
    public void onTrigger(Entity entity);
    public void onUnload();
    public Location getLocation();
    public Entity getEntity();
    public BoundingBox getOffsetBoundingBox();
    public default boolean isDead() {
        return this.getEntity().isDead();
    };
    public void remove();
}
