package ru.darkchronics.quake.game.combat;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import ru.darkchronics.quake.QuakePlugin;
import ru.darkchronics.quake.game.combat.powerup.Powerup;
import ru.darkchronics.quake.game.combat.powerup.PowerupType;

import java.util.Arrays;

import static ru.darkchronics.quake.game.combat.WeaponUtil.WEAPONS_NUM;

public class WeaponUserState {
    private BukkitRunnable shooter;
    private BukkitTask shooterTask;
    private BukkitRunnable clickDetector;
    private boolean shooting;
    public int[] ammo = new int[]{100, 0, 0, 0, 0, 0, 0};
    public int[] cooldowns = new int[]{0, 0, 0, 0, 0, 0, 0};
    public BossBar ammoWarning = BossBar.bossBar(Component.text("LOW AMMO WARNING"), 0, BossBar.Color.WHITE, BossBar.Overlay.PROGRESS);

    public void shoot(Player player) {
        /*
        From the event listener we are assuming
        that a player is holding carrot-on-a-stick in their main hand
        */

        // No custom model data? No guns for you!
        if (!player.getInventory().getItemInMainHand().getItemMeta().hasCustomModelData()) return;
        // Otherwise, please continue
        int customModelData = player.getInventory().getItemInMainHand().getItemMeta().getCustomModelData();
        if (customModelData >= WEAPONS_NUM) return;
        // ...unless you have no ammo
        if (ammo[customModelData] <= 0) {
            player.playSound(player, "quake.weapons.no_ammo", 0.5f, 1f);
            return;
        }

        if (this.shooterTask == null || this.shooterTask.isCancelled()) {
            this.shooter = new BukkitRunnable() {
                boolean justStartedShooting = true;
                @Override
                public void run() {
                    for (int i = 0; i < cooldowns.length; i++) {
                        if (cooldowns[i] >= WeaponUtil.PERIODS[i]) {
                            cooldowns[i] = 0;
                        } else if (cooldowns[i] != 0) {
                            cooldowns[i]++;
                        }
                    }
//                    player.sendActionBar(Component.text(Arrays.toString(cooldowns)));
                    if (
                            player.getInventory().getItemInMainHand().getItemMeta() == null ||
                                    !player.getInventory().getItemInMainHand().getItemMeta().hasCustomModelData()
                    ) {
                        if (Arrays.stream(cooldowns).allMatch(Integer.valueOf(0)::equals)) cancel();
                        return;
                    }
                    int customModelData2 = player.getInventory().getItemInMainHand().getItemMeta().getCustomModelData();

                    if (ammo[customModelData2] <= 0) return;
                    if (shooting && cooldowns[customModelData2] == 0) {
                        switch (customModelData2) {
                            case WeaponType.MACHINEGUN:
                                WeaponUtil.fireMachinegun(player);
                                break;
                            case WeaponType.SHOTGUN:
                                WeaponUtil.fireShotgun(player);
                                break;
                            case WeaponType.ROCKET_LAUNCHER:
                                WeaponUtil.fireRocket(player);
                                break;
                            case WeaponType.LIGHTNING_GUN:
                                WeaponUtil.fireLightning(player, justStartedShooting);
                                break;
                            case WeaponType.RAILGUN:
                                WeaponUtil.fireRailgun(player);
                                break;
                            case WeaponType.PLASMA_GUN:
                                WeaponUtil.firePlasma(player);
                                break;
                            case WeaponType.BFG:
                                WeaponUtil.fireBFG(player);
                                break;
                            default:
                                player.sendMessage("This weapon is unknown or not yet implemented");
                                cancel();
                                return;
                        }
                        boolean hasQuad = Powerup.hasPowerup(player, PowerupType.QUAD_DAMAGE);
                        boolean isLightningOrBFG = customModelData2 != WeaponType.LIGHTNING_GUN && customModelData2 != WeaponType.BFG;
                        if (
                                (hasQuad && isLightningOrBFG) ||
                                        (hasQuad && customModelData2 == WeaponType.LIGHTNING_GUN && justStartedShooting)
                        )
                            player.getWorld().playSound(player, "quake.items.powerups.quad_damage.fire", 0.5f, 1f);

                        justStartedShooting = false;
                        ammo[customModelData2] -= 1;

                        // Determine whether to show the ammo warning
//                        int zeroesAmount = 0;
//                        for (int i : ammo) {
//                            if (i == 0) {
//                                zeroesAmount++;
//                            }
//                        }
//                        if (zeroesAmount >= ammo.length-1 && ammo[customModelData2] <= 30) {
//                            player.playSound(player, "quake.weapons.no_ammo", 0.5f, 1f);
//                            player.showBossBar(ammoWarning);
//                        } else
//                            player.hideBossBar(ammoWarning);

                        cooldowns[customModelData2]++;
                    }

                    if (Arrays.stream(cooldowns).allMatch(Integer.valueOf(0)::equals)) cancel();
                }
            };
            this.shooterTask = this.shooter.runTaskTimer(QuakePlugin.INSTANCE, 0, 1);
        }

        if (this.clickDetector != null)
            this.clickDetector.cancel();
        shooting = true;
        this.clickDetector = new BukkitRunnable() {
            @Override
            public void run() {
                shooting = false;
            }
        };
        clickDetector.runTaskLater(QuakePlugin.INSTANCE, 4);
    }
}
