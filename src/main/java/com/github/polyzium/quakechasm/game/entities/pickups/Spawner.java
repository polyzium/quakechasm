/*
 * Quakechasm, a Quake minigame plugin for Minecraft servers running PaperMC
 * 
 * Copyright (C) 2024-present Polyzium
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.github.polyzium.quakechasm.game.entities.pickups;

import org.bukkit.*;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BoundingBox;
import com.github.polyzium.quakechasm.QuakePlugin;
import com.github.polyzium.quakechasm.game.entities.DisplayPickup;

public abstract class Spawner implements DisplayPickup {
    public ItemDisplay display;
    static BoundingBox boundingBox = new BoundingBox(-1, -1, -1, 1, 0, 1);

    public Spawner(ItemStack item, World world, Location location) {
        this.display = (ItemDisplay) world.spawnEntity(location, EntityType.ITEM_DISPLAY);
        this.display.setItemStack(item);

//        QEntityUtil.setEntityType(this.display, "anything_you_want");

        // Init rotator
        this.display.setInterpolationDuration(20);
        this.display.setTeleportDuration(20);
        this.display.setRotation(0, 0);

        QuakePlugin.INSTANCE.triggers.add(this);
    }

    public Spawner(ItemDisplay display) {
        assert display != null;

        this.display = display;

        // Init rotator
        this.display.setInterpolationDuration(20);
        this.display.setTeleportDuration(20);
        this.display.setRotation(0, 0);

        // Normally here you would QuakePlugin.INSTANCE.triggers.add(this),
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

    public void onUnload() {
        // do nothing
    }
}
