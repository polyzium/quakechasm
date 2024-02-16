package ru.darkchronics.quake;

import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import ru.darkchronics.quake.game.combat.WeaponUserState;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class QuakeUserState {
    private Player player;
    public QuakePlugin plugin;
    public WeaponUserState weaponState;
    public Location portalLoc = null;
    public BukkitRunnable healthDecreaser;
    public BukkitRunnable armorDecreaser;
    public int armor = 0;

    public QuakeUserState(QuakePlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.weaponState = new WeaponUserState(plugin);
    }

    public void startArmorDecreaser() {
        if (this.armorDecreaser != null) return;

        this.armorDecreaser = new BukkitRunnable() {
            @Override
            public void run() {
                if (armor <= 100) {
                    armor = 100;
                    this.cancel();
                    armorDecreaser = null;
                    return;
                }
                armor -= 1;
            }
        };
        armorDecreaser.runTaskTimer(this.plugin, 20, 20);
    }

    public void startHealthDecreaser() {
        if (this.healthDecreaser != null) return;

        this.healthDecreaser = new BukkitRunnable() {
            @Override
            public void run() {
                double currentHealth = player.getHealth();
                if (currentHealth <= 20) {
                    player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(20);
                    this.cancel();
                    healthDecreaser = null;
                    return;
                }
                float newHealth = Math.round((currentHealth * 5) - 1) / 5f;
                player.setHealth(newHealth);
                player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(newHealth);
            }
        };
        healthDecreaser.runTaskTimer(this.plugin, 20, 20);
    }
}
