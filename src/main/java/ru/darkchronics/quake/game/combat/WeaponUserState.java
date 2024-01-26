package ru.darkchronics.quake.game.combat;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import ru.darkchronics.quake.QuakePlugin;

public class WeaponUserState {
    private final QuakePlugin plugin;
    private BukkitRunnable shooter;
    private BukkitTask shooterTask;
    private BukkitRunnable clickDetector;
    private boolean shooting;


    public WeaponUserState(QuakePlugin plugin) {
        this.plugin = plugin;
    }

    public void shoot(Player player) {
        /*
        From the event listener we are assuming
        that a player is holding carrot-on-a-stick in their main hand
        */

        // No custom model data? No guns for you!
        if (!player.getInventory().getItemInMainHand().getItemMeta().hasCustomModelData()) return;
        // Otherwise, please continue
        int customModelData = player.getInventory().getItemInMainHand().getItemMeta().getCustomModelData();
        if (customModelData >= WeaponUtil.weaponPeriods.length) return;

        if (this.shooterTask == null || this.shooterTask.isCancelled()) {
            this.shooter = new BukkitRunnable() {
                private int ticksPassed = 0;
                private int period;
                @Override
                public void run() {
                    if (player.getInventory().getItemInMainHand().getItemMeta() == null || !player.getInventory().getItemInMainHand().getItemMeta().hasCustomModelData()) {
                        cancel();
                        return;
                    }
                    int customModelData2 = player.getInventory().getItemInMainHand().getItemMeta().getCustomModelData();
                    period = WeaponUtil.weaponPeriods[customModelData2];
                    if (ticksPassed >= period) {
                        ticksPassed = 0;
                    } else if (ticksPassed != 0) {
                        ticksPassed++;
                        return;
                    };
                    if (shooting && ticksPassed == 0) {
                        switch (customModelData2) {
                            case 0: // Machinegun
                                WeaponUtil.fireMachinegun(player);
                                break;
                            case 1: // Shotgun
                                WeaponUtil.fireShotgun(player);
                                break;
                            case 2: // Rocket Launcher
                                WeaponUtil.fireRocket(player);
                                break;
                            case 3: // Lightning Gun (NOT IMPLEMENTED)
                                WeaponUtil.fireLightning(player);
                                break;
                            case 4: // Railgun
                                WeaponUtil.fireRailgun(player);
                                break;
                            case 5: // Plasma Gun
                                WeaponUtil.firePlasma(player);
                                break;
                            case 6: // BFG
                                WeaponUtil.fireBFG(player);
                                break;
                            default:
                                player.sendMessage("This weapon is unknown or not yet implemented");
                                cancel();
                                return;
                        }

                        ticksPassed++;
                    } else {
                        shooterTask.cancel();
                    }
                }
            };
            this.shooterTask = this.shooter.runTaskTimer(this.plugin, 0, 1);
        }

        this.clickDetector = new BukkitRunnable() {
            @Override
            public void run() {
                shooting = false;
            }
        };
        shooting = true;
        clickDetector.runTaskLater(plugin, 4);
    }
}
