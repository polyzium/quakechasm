package ru.darkchronics.quake.game.entities.pickups;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import ru.darkchronics.quake.QuakePlugin;
import ru.darkchronics.quake.QuakeUserState;
import ru.darkchronics.quake.events.listeners.CombatListener;
import ru.darkchronics.quake.game.combat.WeaponType;
import ru.darkchronics.quake.game.entities.QEntityUtil;
import ru.darkchronics.quake.hud.Hud;
import ru.darkchronics.quake.matchmaking.Team;

import java.util.Objects;

public class WeaponSpawner extends Spawner {
    private ItemStack itemForRespawn;
    private BukkitTask respawnTask;
    public WeaponSpawner(int weaponIndex, World world, Location location) {
        super(new ItemStack(Material.AIR), world, location);

        ItemStack item = getWeapon(weaponIndex);
        super.display.setItemStack(item);
        QEntityUtil.setEntityType(super.display, "weapon_spawner");

        QuakePlugin.INSTANCE.triggers.add(this);
    }

    public WeaponSpawner(ItemDisplay display) {
        super(display);

        QuakePlugin.INSTANCE.triggers.add(this);
    }

    public static void convert(ItemDisplay displayToConvert) {
        int modelData = displayToConvert.getItemStack().getItemMeta().getCustomModelData();

        ItemStack item = getWeapon(modelData);
        displayToConvert.setItemStack(item);
        QEntityUtil.setEntityType(displayToConvert, "weapon_spawner");

        new WeaponSpawner(displayToConvert);
    }

    public static ItemStack getWeapon(int index) {
        ItemStack itemStack = new ItemStack(Material.CARROT_ON_A_STICK);
        ItemMeta weaponMeta = itemStack.getItemMeta();
        String weaponName = switch (index) {
            case WeaponType.MACHINEGUN -> "Machinegun";
            case WeaponType.SHOTGUN -> "Shotgun";
            case WeaponType.ROCKET_LAUNCHER -> "Rocket Launcher";
            case WeaponType.LIGHTNING_GUN -> "Lightning Gun";
            case WeaponType.RAILGUN -> "Railgun";
            case WeaponType.PLASMA_GUN -> "Plasma Gun";
            case WeaponType.BFG -> "BFG10K";
            default -> throw new IllegalStateException("Weapon out of range");
        };
        weaponMeta.displayName(
                Component.text(weaponName).
                        decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.byBoolean(false))
        );
        weaponMeta.setCustomModelData(index);

        itemStack.setItemMeta(weaponMeta);

        return itemStack;
    }

    public void onPickup(Player player) {
        ItemStack item = super.display.getItemStack();
        if (item.isEmpty())
            return;

        this.itemForRespawn = item;

        CombatListener.sortGun(item, player);
        super.display.setItemStack(new ItemStack(Material.AIR)); // Make invisible
        player.getWorld().playSound(player, "quake.weapons.pickup", 0.5f, 1f);
        Hud.pickupMessage(player, Objects.requireNonNull(item.getItemMeta().displayName()));

        // Respawn in 5 seconds, or 30 seconds if team based
        QuakeUserState userState = QuakePlugin.INSTANCE.userStates.get(player);
        int respawnTimeTicks = 5*20;
        if (userState.currentMatch != null && (userState.currentMatch.getTeamOfPlayer(player) != Team.FREE)) {
            respawnTimeTicks = 30*20;
        }
        this.respawnTask = new BukkitRunnable() {
            public void run() {
                respawn();
            }
        }.runTaskLater(QuakePlugin.INSTANCE, respawnTimeTicks);
    }

    @Override
    public void respawn() {
        if (!super.display.getItemStack().isEmpty()) return;

        display.setItemStack(itemForRespawn);
        display.getWorld().spawnParticle(Particle.SPELL_INSTANT, display.getLocation(), 16, 0.5, 0.5, 0.5);
        display.getWorld().playSound(display, "quake.items.respawn", 0.5f, 1f);

        if (this.respawnTask != null)
            this.respawnTask.cancel();
    }
}
