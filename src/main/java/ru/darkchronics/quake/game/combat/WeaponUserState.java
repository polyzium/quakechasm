package ru.darkchronics.quake.game.combat;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import ru.darkchronics.quake.QuakePlugin;

import java.util.Arrays;

import static ru.darkchronics.quake.game.combat.WeaponUtil.WEAPONS_NUM;

public class WeaponUserState {
    private final QuakePlugin plugin;
    private BukkitRunnable shooter;
    private BukkitTask shooterTask;
    private BukkitRunnable clickDetector;
    private boolean shooting;
    public int[] ammo = new int[]{100, 0, 0, 0, 0, 0, 0};
    public int[] cooldowns = new int[]{0, 0, 0, 0, 0, 0, 0};

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
        if (customModelData >= WEAPONS_NUM) return;
        // ...unless you have no ammo
        if (ammo[customModelData] <= 0) {
            player.sendActionBar(MiniMessage.miniMessage().deserialize("<red>OUT OF AMMO</red>"));
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
                            case 0: // Machinegun
                                WeaponUtil.fireMachinegun(player);
                                break;
                            case 1: // Shotgun
                                WeaponUtil.fireShotgun(player);
                                break;
                            case 2: // Rocket Launcher
                                WeaponUtil.fireRocket(player);
                                break;
                            case 3: // Lightning Gun
                                WeaponUtil.fireLightning(player, justStartedShooting);
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
                        justStartedShooting = false;
                        ammo[customModelData2] -= 1;
                        player.sendActionBar(MiniMessage.miniMessage().deserialize(
                                String.format("Ammo: <green>%d</green>", ammo[customModelData2])
                        ));

                        cooldowns[customModelData2]++;
                    }

                    if (Arrays.stream(cooldowns).allMatch(Integer.valueOf(0)::equals)) cancel();
                }
            };
            this.shooterTask = this.shooter.runTaskTimer(this.plugin, 0, 1);
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
        clickDetector.runTaskLater(plugin, 4);
    }
}
