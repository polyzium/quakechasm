package ru.darkchronics.quake;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import ru.darkchronics.quake.game.combat.DamageCause;
import ru.darkchronics.quake.game.combat.WeaponUserState;
import ru.darkchronics.quake.game.combat.powerup.Powerup;
import ru.darkchronics.quake.game.combat.powerup.PowerupType;
import ru.darkchronics.quake.hud.Hud;

import java.util.ArrayList;

public class QuakeUserState {
    private Player player;
    public WeaponUserState weaponState;
    public Location portalLoc = null;
    public BukkitRunnable healthDecreaser;
    public BukkitRunnable armorDecreaser;
    public int armor = 0;
    public ArrayList<Powerup> activePowerups = new ArrayList<>(3);
    public Hud hud;
    private DamageCause lastDamageCause;

    public QuakeUserState(Player player) {
        this.player = player;
        this.weaponState = new WeaponUserState(QuakePlugin.INSTANCE);
        this.hud = new Hud(this);
    }

    public Player getPlayer() {
        return player;
    }

    public void startArmorDecreaser() {
        if (this.armorDecreaser != null) return;

        this.armorDecreaser = new BukkitRunnable() {
            @Override
            public void run() {
                if (armor <= 100) {
                    this.cancel();
                    armorDecreaser = null;
                    return;
                }
                armor -= 1;
            }
        };
        armorDecreaser.runTaskTimer(QuakePlugin.INSTANCE, 20, 20);
    }

    public void startHealthDecreaser() {
        if (this.healthDecreaser != null) return;

        this.healthDecreaser = new BukkitRunnable() {
            @Override
            public void run() {
                if (Powerup.hasPowerup(player, PowerupType.REGENERATION)) return;

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
        healthDecreaser.runTaskTimer(QuakePlugin.INSTANCE, 20, 20);
    }

    public boolean isCustomDamage() {
        return lastDamageCause != null;
    }

    public DamageCause getLastDamageCause() {
        DamageCause t = lastDamageCause;
        lastDamageCause = null;
        return t;
    }

    public void setLastDamageCause(DamageCause lastDamageCause) {
        this.lastDamageCause = lastDamageCause;
    }
}
