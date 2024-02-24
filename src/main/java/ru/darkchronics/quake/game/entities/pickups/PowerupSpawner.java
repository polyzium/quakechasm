package ru.darkchronics.quake.game.entities.pickups;

import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import ru.darkchronics.quake.QuakePlugin;
import ru.darkchronics.quake.QuakeUserState;
import ru.darkchronics.quake.game.combat.powerup.Powerup;
import ru.darkchronics.quake.game.combat.powerup.PowerupType;
import ru.darkchronics.quake.game.entities.QEntityUtil;
import ru.darkchronics.quake.hud.Hud;

public class PowerupSpawner extends Spawner {
    private ItemStack itemForRespawn;
    private BukkitTask respawnTask;
    private PowerupType type;
    private boolean oneTime;
    private int duration;
    public PowerupSpawner(PowerupType type, World world, Location location, boolean oneTime, int duration, QuakePlugin plugin) {
        super(new ItemStack(Material.TOTEM_OF_UNDYING), world, location, plugin);
        this.type = type;
        this.oneTime = oneTime;
        this.duration = duration;

        PersistentDataContainer displayData = display.getPersistentDataContainer();
        NamespacedKey typeKey = new NamespacedKey(super.plugin, "type");
        displayData.set(typeKey, PersistentDataType.STRING, this.type.toString());

        if (!oneTime)
            QEntityUtil.setEntityType(super.display, "powerup_spawner");
    }

    public PowerupSpawner(ItemDisplay display, QuakePlugin plugin) {
        super(display, plugin);

        PersistentDataContainer displayData = display.getPersistentDataContainer();
        NamespacedKey typeKey = new NamespacedKey(super.plugin, "type");
        this.type = PowerupType.valueOf(displayData.get(typeKey, PersistentDataType.STRING));
        this.oneTime = false;
        this.duration = 30;

        plugin.triggers.add(this);
    }

    public static void doPowerup(Player player, PowerupType type, int time) {
        boolean found = false;
        Powerup powerup2;

        QuakeUserState state = QuakePlugin.INSTANCE.userStates.get(player);
        for (Powerup powerup : state.activePowerups) {
            if (powerup.getType() == type) {
                powerup.extendDuration(time);

                found = true;
                break;
            }
        }
        if (!found) {
            powerup2 = new Powerup(player, type, time);
            state.activePowerups.add(powerup2);
        }
    }

    @Override
    public void onPickup(Player player) {
        ItemStack item = super.display.getItemStack();
        if (item.isEmpty()) return;
        this.itemForRespawn = item;

        doPowerup(player, this.type, this.duration);

        this.display.setItemStack(new ItemStack(Material.AIR)); // Make invisible
        player.getWorld().playSound(player, Powerup.SOUNDS.get(type), 0.5f, 1f);
        // TODO
        Hud.pickupMessage(player, Component.text(Powerup.NAMES.get(type)));

        if (!this.oneTime)
            // Respawn in 2 minutes
            this.respawnTask = new BukkitRunnable() {
            public void run() {
                respawn();
            }
        }.runTaskLater(this.plugin, 20 * 5);
        else
            // Despawn
            this.display.remove();
    }

    public void respawn() {
        if (!super.display.getItemStack().isEmpty() || this.oneTime) return;

        display.setItemStack(this.itemForRespawn);
        display.getWorld().spawnParticle(Particle.SPELL_INSTANT, display.getLocation(), 16, 0.5, 0.5, 0.5);
        display.getWorld().playSound(display, "quake.items.powerups.respawn", 0.5f, 1f);

        if (!this.oneTime)
            this.respawnTask.cancel();
    }
}
