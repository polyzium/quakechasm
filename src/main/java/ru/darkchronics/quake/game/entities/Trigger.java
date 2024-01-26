package ru.darkchronics.quake.game.entities;

import org.bukkit.Location;
import org.bukkit.entity.Entity;

public interface Trigger {
    public void onTrigger(Entity entity);
    public Location getLocation();
    public Entity getEntity();
    public default boolean isDead() {
        return this.getEntity().isDead();
    };
    public void remove();
}
