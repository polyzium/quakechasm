package ru.darkchronics.quake.events;

import com.destroystokyo.paper.event.entity.EntityKnockbackByEntityEvent;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import ru.darkchronics.quake.QuakePlugin;
import ru.darkchronics.quake.QuakeUserState;
import ru.darkchronics.quake.game.combat.ProjectileUtil;
import ru.darkchronics.quake.game.combat.WeaponType;
import ru.darkchronics.quake.game.combat.WeaponUserState;
import ru.darkchronics.quake.game.combat.WeaponUtil;
import ru.darkchronics.quake.misc.MiscUtil;

import java.util.*;

import static org.bukkit.craftbukkit.v1_20_R3.inventory.CraftItemStack.getItemMeta;

public class CombatListener implements Listener {
    private final QuakePlugin plugin;

    public CombatListener(QuakePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Player player = event.getPlayer();
        if (player.getInventory().getItemInMainHand().getType() != Material.CARROT_ON_A_STICK) return;

        QuakeUserState state = this.plugin.userStates.get(player);
        state.weaponState.shoot(player);
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (event.getEntityType() != EntityType.SNOWBALL) return;
        event.setCancelled(true);

        switch (ProjectileUtil.getProjectileType(event.getEntity())) {
            case "rocket":
                ProjectileUtil.impactRocket(event);
                break;
            case "plasma":
                ProjectileUtil.impactPlasma(event);
                break;
            case "bfg":
                ProjectileUtil.impactBFG(event);
                break;
        }
    }

    @EventHandler
    public void onKnockback(EntityKnockbackByEntityEvent event) {
        // Cancel knockback, because we have our own
        event.setCancelled(true);
    }

    // Handle 20+ health (for things like Mega Health or Regeneration)
    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        // Play hit sound
        if (
                event instanceof EntityDamageByEntityEvent attackEvent &&
                        event.getEntity() instanceof LivingEntity victim &&
                        event.getEntity() != attackEvent.getDamager()
        ) {
            Entity attacker = attackEvent.getDamager();
            float health = (float) (victim.getHealth() - event.getDamage());
            attacker.playSound(
                    Sound.sound(Key.key("quake.feedback.hit"),
                            Sound.Source.NEUTRAL,
                            1f,
                            (float) Math.round((1f + (health / 66)) * 6) /6
                    )
            );
        }

        if (
                event.getEntity().getType() != EntityType.PLAYER ||
                (event.getEntity().getType() == EntityType.PLAYER && event.getCause() == EntityDamageEvent.DamageCause.FALL && event.getEntity().getFallDistance() < 10)
                // ^^^ if player fell and the fall distance is less than 10 blocks. We have an event handler for that, so ignore if true
        ) return;

        Player player = (Player) event.getEntity();
        QuakeUserState userState = this.plugin.userStates.get(player);

        // Calculate armor factor
        double finalDamage = event.getDamage();
        double origDamage = event.getDamage();
        if (userState.armor > 0) {
            finalDamage /= 3;
            double damageToArmor = (event.getDamage() - finalDamage)*5;
            userState.armor -= (int) damageToArmor;
            if (userState.armor < 0) {
                finalDamage += (Math.abs(userState.armor))/5d;
                userState.armor = 0;
            }
            event.setDamage(finalDamage);
            player.sendActionBar(MiniMessage.miniMessage().deserialize(
                    String.format("Armor: <red>%d</red>", userState.armor)
            ));

            player.sendMessage("origDamage: "+origDamage);
            player.sendMessage("finalDamage: "+finalDamage);
            player.sendMessage("damageToArmor: "+damageToArmor);
        }

        double finalMaxHealth = player.getHealth() - finalDamage;
        if (finalMaxHealth < 20) finalMaxHealth = 20;
        player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(finalMaxHealth);

        // Display health in a scale of 5:1 (20 Minecraft HP = 10 hearts = 100 classic HP)
//        player.sendMessage(String.valueOf(
//                (player.getHealth()-event.getDamage())*5
//        ));
    }

    // No hunger
    @EventHandler
    public void onHunger(FoodLevelChangeEvent event) {
        event.setCancelled(true);
    }

    // No health regen from food + start decreaser on regain
    @EventHandler
    public void onRegen(EntityRegainHealthEvent event) {
        if (event.getRegainReason() == EntityRegainHealthEvent.RegainReason.SATIATED) {
            event.setCancelled(true);
            return;
        }

        if (event.getEntity().getType() != EntityType.PLAYER) return;
        QuakeUserState state = this.plugin.userStates.get((Player) event.getEntity());
        state.startHealthDecreaser();
    }

    // Respawn on spawnpoints
    // TODO adapt to map system
    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Location spawnPoint = this.plugin.spawnpoints.get(
                (int) (Math.random() * (this.plugin.spawnpoints.size() - 1))
        );
//        spawnPoint.setX(spawnPoint.x()+0.5);
//        spawnPoint.setZ(spawnPoint.z()+0.5);

        // Clear all ammo
        WeaponUserState weaponState = this.plugin.userStates.get(event.getPlayer()).weaponState;
        Arrays.fill(weaponState.ammo, 0);

        ItemStack machinegun = new ItemStack(Material.CARROT_ON_A_STICK);
        ItemMeta mgMeta = machinegun.getItemMeta();
        mgMeta.setCustomModelData(0);
        mgMeta.displayName(
                Component.text("Machinegun").
                        decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.byBoolean(false))
        );
        machinegun.setItemMeta(mgMeta);

        weaponState.ammo[WeaponType.MACHINEGUN] = WeaponUtil.DEFAULT_AMMO[WeaponType.MACHINEGUN];

        event.getPlayer().getInventory().addItem(machinegun);
        event.setRespawnLocation(spawnPoint);

        event.getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(20);

        spawnPoint.getWorld().playSound(spawnPoint, "quake.world.tele_in", 1, 1);
        MiscUtil.teleEffect(spawnPoint);
    }

    // No inventory drop
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e) {
        Player player = e.getPlayer();
        ItemStack handItem = player.getInventory().getItemInMainHand().clone();
        ItemMeta itemMeta = handItem.getItemMeta();
        plugin.userStates.get(player).armor = 0;
        if (itemMeta != null && itemMeta.hasCustomModelData() && handItem.getType() == Material.CARROT_ON_A_STICK) {
            int modelData = itemMeta.getCustomModelData();
            WeaponUserState weaponState = plugin.userStates.get(player).weaponState;
            PersistentDataContainer pdc = itemMeta.getPersistentDataContainer();
            pdc.set(new NamespacedKey("darkchronics-quake", "ammo"), PersistentDataType.INTEGER, weaponState.ammo[modelData]);
            handItem.setItemMeta(itemMeta);
        }

        Collection<ItemStack> drops = e.getDrops();
        drops.clear();
        drops.add(handItem);
    }

    public static void sortGun(ItemStack gunItem, Player player, QuakePlugin plugin) {
        int modelData = gunItem.getItemMeta().getCustomModelData();
        PlayerInventory inv = player.getInventory();
        WeaponUserState weaponState = plugin.userStates.get(player).weaponState;

        // Find the gun, regardless of its NBT/PDC
        Optional<ItemStack> foundGun = Arrays.stream(inv.getContents()).filter(Objects::nonNull).filter(
                invItem -> invItem.getItemMeta().hasCustomModelData() && invItem.getItemMeta().getCustomModelData() == modelData
        ).findAny();

        if (foundGun.isPresent()) {
            PersistentDataContainer pdc = gunItem.getItemMeta().getPersistentDataContainer();
            Integer ammo = pdc.get(new NamespacedKey("darkchronics-quake", "ammo"), PersistentDataType.INTEGER);

            if (ammo != null) {
                weaponState.ammo[modelData] += ammo;
            } else {
                if (weaponState.ammo[modelData] < WeaponUtil.DEFAULT_AMMO[modelData])
                    weaponState.ammo[modelData] = WeaponUtil.DEFAULT_AMMO[modelData];
                else
                    weaponState.ammo[modelData] += 1;
            }
        } else {
            if (weaponState.ammo[modelData] < WeaponUtil.DEFAULT_AMMO[modelData])
                weaponState.ammo[modelData] = WeaponUtil.DEFAULT_AMMO[modelData];
            else
                weaponState.ammo[modelData] += 1;
            inv.setItem(modelData, gunItem);
            inv.setHeldItemSlot(modelData);
        }
    }

    // Sort picked up gun
    @EventHandler
    public void onItemPickup(EntityPickupItemEvent event) {
        ItemStack item = event.getItem().getItemStack();
        LivingEntity entity = event.getEntity();
        if (!(item.getItemMeta().hasCustomModelData() && item.getType() == Material.CARROT_ON_A_STICK && entity instanceof Player player)) return;

        sortGun(item, player, this.plugin);
        event.getItem().remove();
        player.playSound(entity, "quake.weapons.pickup", 0.5f,  1f);
        entity.sendActionBar(item.displayName());
        event.setCancelled(true);
    }

    // No offhand
    @EventHandler
    public void onHandSwap(PlayerSwapHandItemsEvent event) {
        event.getPlayer().sendMessage("§cYou can't put items into offhand!");
        event.setCancelled(true);
    }

    // Also no offhand, but in inventory
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getSlot() != 40)
            return;
        event.getView().getPlayer().sendMessage("§cYou can't put items into offhand!");
        ItemStack cursorItem = event.getCursor().clone();
        event.setCancelled(true);
//        event.getView().getPlayer().getInventory().addItem(cursorItem);
    }
}
