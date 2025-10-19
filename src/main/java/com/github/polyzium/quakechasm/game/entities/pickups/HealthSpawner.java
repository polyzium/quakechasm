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
import com.github.polyzium.quakechasm.QuakePlugin;
import com.github.polyzium.quakechasm.game.entities.QEntityUtil;
import com.github.polyzium.quakechasm.hud.Hud;
import com.github.polyzium.quakechasm.misc.TranslationManager;

public class HealthSpawner extends Spawner {
    private ItemStack itemForRespawn;
    private BukkitTask respawnTask;
    private int health;
    public HealthSpawner(int health, World world, Location location) {
        super(new ItemStack(Material.PORKCHOP), world, location);

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
        NamespacedKey healthKey = new NamespacedKey(QuakePlugin.INSTANCE, "health");
        displayData.set(healthKey, PersistentDataType.INTEGER, this.health);

        super.display.setItemStack(item);
        QEntityUtil.setEntityType(super.display, "health_spawner");

        QuakePlugin.INSTANCE.triggers.add(this);
    }

    public HealthSpawner(ItemDisplay display) {
        super(display);

        PersistentDataContainer displayData = display.getPersistentDataContainer();
        NamespacedKey healthKey = new NamespacedKey(QuakePlugin.INSTANCE, "health");
        this.health = displayData.get(healthKey, PersistentDataType.INTEGER);

        QuakePlugin.INSTANCE.triggers.add(this);
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
                player.getAttribute(Attribute.MAX_HEALTH).setBaseValue(totalHealth);
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
            Hud.pickupMessage(player, Component.text(TranslationManager.t("PICKUP_HEALTH_MEGA", player)));
        else
            Hud.pickupMessage(player, Component.text(this.health*5).append(Component.text(" Health")));

        // Respawn in 35 seconds
        this.respawnTask = new BukkitRunnable() {
            public void run() {
                respawn();
            }
        }.runTaskLater(QuakePlugin.INSTANCE, 20*35);
    }

    @Override
    public void respawn() {
        if (!super.display.getItemStack().isEmpty()) return;

        display.setItemStack(this.itemForRespawn);
        display.getWorld().spawnParticle(Particle.INSTANT_EFFECT, display.getLocation(), 16, 0.5, 0.5, 0.5);
        display.getWorld().playSound(display, "quake.items.respawn", 0.5f, 1f);

        if (this.respawnTask != null)
            this.respawnTask.cancel();
    }
}
