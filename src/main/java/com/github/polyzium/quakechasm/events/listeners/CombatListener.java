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

package com.github.polyzium.quakechasm.events.listeners;

import com.destroystokyo.paper.event.entity.EntityKnockbackByEntityEvent;
import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import com.github.polyzium.quakechasm.game.combat.*;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
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
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import com.github.polyzium.quakechasm.QuakePlugin;
import com.github.polyzium.quakechasm.QuakeUserState;
import com.github.polyzium.quakechasm.game.combat.*;
import com.github.polyzium.quakechasm.game.combat.powerup.Powerup;
import com.github.polyzium.quakechasm.game.combat.powerup.PowerupType;
import com.github.polyzium.quakechasm.hud.Hud;
import com.github.polyzium.quakechasm.matchmaking.matches.CTFMatch;
import com.github.polyzium.quakechasm.matchmaking.matches.Match;
import com.github.polyzium.quakechasm.misc.MiscUtil;
import com.github.polyzium.quakechasm.misc.TranslationManager;

import java.util.*;
import java.util.stream.Stream;

public class CombatListener implements Listener {
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Player player = event.getPlayer();
        if (player.getInventory().getItemInMainHand().getType() != Material.CARROT_ON_A_STICK) return;

        QuakeUserState state = QuakePlugin.INSTANCE.userStates.get(player);
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

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        // Contain player state for later use (if it's a player)
        QuakeUserState userState = null;
        if (event.getEntity() instanceof Player player)
            userState = QuakePlugin.INSTANCE.userStates.get(player);

        // Extinguish fires
        if (event.getCause() == EntityDamageEvent.DamageCause.FIRE_TICK) {
            event.getEntity().setFireTicks(0);
            event.setCancelled(true);
            return;
        }

        // Special cases for EntityDamageByEntityEvent
        if (
                event instanceof EntityDamageByEntityEvent attackEvent &&
                        event.getEntity() instanceof LivingEntity victim
        ) {
            Entity attacker = attackEvent.getDamager();

            // If the attacker has quad, triple the damage
            if (
                    attacker instanceof Player pAttacker &&
                            Powerup.hasPowerup(pAttacker, PowerupType.QUAD_DAMAGE)
            ) {
                event.setDamage(event.getDamage() * 3);
            }

            if (event.getEntity() != attackEvent.getDamager()) {
                // Play hit sound
                float health = (float) (victim.getHealth() - event.getDamage());
                attacker.playSound(
                        Sound.sound(Key.key("quake.feedback.hit"),
                                Sound.Source.NEUTRAL,
                                1f,
                                (float) Math.round((1f + (health / 66)) * 6) / 6
                        )
                );
            }
        }

        // If a player has protection powerup, deny any lava and explosion damage
        if (
                event.getEntity() instanceof Player player &&
                Powerup.hasPowerup(player, PowerupType.PROTECTION)
        ) {
            assert userState != null;
            DamageData damageData = userState.lastDamage;
            if (
                    (event.getCause() == EntityDamageEvent.DamageCause.LAVA || event.getCause() == EntityDamageEvent.DamageCause.DROWNING) ||
                            (damageData != null && (damageData.getCause() == DamageCause.ROCKET_SPLASH ||
                    damageData.getCause() == DamageCause.BFG_SPLASH || damageData.getCause() == DamageCause.PLASMA_SPLASH))
            ) {
                player.playSound(player, "quake.items.powerups.protection.protect", 0.5f, 1f);
                event.setCancelled(true);
                return;
            } else {
                event.setDamage(event.getDamage()/2d);
            }
        }

        // Kill the player if they receive any void damage
        if (event.getCause() == EntityDamageEvent.DamageCause.VOID)
            event.setDamage(1000);

        if (
                event.getEntity().getType() != EntityType.PLAYER ||
                (event.getEntity().getType() == EntityType.PLAYER && event.getCause() == EntityDamageEvent.DamageCause.FALL && event.getEntity().getFallDistance() < 10)
                // ^^^ if player fell and the fall distance is less than 10 blocks. We have an event handler for that, so ignore if true
        ) return;

        Player player = (Player) event.getEntity();
        assert userState != null;

        // Calculate armor factor
        // But first, cancel vanilla armor
        event.setDamage(EntityDamageEvent.DamageModifier.ARMOR, 0);
        double finalDamage = event.getDamage();
        if (userState.armor > 0) {
            finalDamage /= 3;
            double damageToArmor = (event.getDamage() - finalDamage)*5;
            userState.armor -= (int) damageToArmor;
            if (userState.armor < 0) {
                finalDamage += (Math.abs(userState.armor))/5d;
                userState.armor = 0;
            }
            event.setDamage(finalDamage);
        }

        double finalMaxHealth = player.getHealth() - finalDamage;
        if (finalMaxHealth < 20) finalMaxHealth = 20;
        Objects.requireNonNull(player.getAttribute(Attribute.MAX_HEALTH)).setBaseValue(finalMaxHealth);

        if (player.getHealth() - finalDamage <= 0) {
            DamageData lastDamage = userState.lastDamage;
            if (userState.currentMatch != null && lastDamage != null) {
                userState.currentMatch.onDeath(player, lastDamage.getAttacker(), lastDamage.getCause());
            } else if (userState.currentMatch != null && event instanceof EntityDamageByEntityEvent attackEvent) {
                userState.currentMatch.onDeath(player, attackEvent.getDamager(), DamageCause.UNKNOWN);
            } else if (userState.currentMatch != null) {
                DamageCause qCause = switch (event.getCause()) {
                    case FALL -> DamageCause.FALLING;
                    case FIRE, VOID -> DamageCause.TRIGGER_HURT;
                    case MELTING, LAVA, HOT_FLOOR -> DamageCause.LAVA;
                    case DROWNING -> DamageCause.WATER;
                    case SUICIDE -> DamageCause.SUICIDE;
                    case FALLING_BLOCK, CRAMMING, FLY_INTO_WALL -> DamageCause.CRUSH;
                    default -> DamageCause.UNKNOWN;
                };
                userState.currentMatch.onDeath(player, null, qCause);
            }
        }

        userState.lastDamage = null;
    }

    // Do not take durability from armor. This will be used for teams later on
    @EventHandler
    public void onArmorChange(PlayerArmorChangeEvent event) {
        if (Stream.of(Material.LEATHER_BOOTS, Material.LEATHER_LEGGINGS, Material.LEATHER_CHESTPLATE).allMatch(material -> event.getOldItem().getType() != material))
            return;

        Damageable oldMeta = (Damageable) event.getOldItem().getItemMeta();
        Damageable newMeta = (Damageable) event.getNewItem().getItemMeta();

        // FIXME: I didn't use the "or" operator here because nullpointerexception
        if (newMeta == null) {
            return;
        } else if (oldMeta.getDamage() >= newMeta.getDamage()) {
            return;
        }

        newMeta.setDamage(oldMeta.getDamage());
        ItemStack newItem = event.getNewItem();
        newItem.setItemMeta(newMeta);

        switch (event.getSlotType()) {
            case HEAD -> event.getPlayer().getInventory().setHelmet(newItem);
            case CHEST -> event.getPlayer().getInventory().setChestplate(newItem);
            case LEGS -> event.getPlayer().getInventory().setLeggings(newItem);
            case FEET -> event.getPlayer().getInventory().setBoots(newItem);
        }
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
        QuakeUserState state = QuakePlugin.INSTANCE.userStates.get((Player) event.getEntity());
        state.startHealthDecreaser();
    }

    // Respawn on spawnpoints
    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        QuakeUserState userState = QuakePlugin.INSTANCE.userStates.get(player);
        if (userState.currentMatch == null) {
            event.setRespawnLocation(QuakePlugin.LOBBY);
            return;
        }

        Location spawnPoint = userState.prepareRespawn();
        event.setRespawnLocation(spawnPoint);

        MiscUtil.teleEffect(spawnPoint, false);
        if (userState.currentMatch.isTeamMatch())
            Match.setArmor(player, userState.currentMatch.getTeamOfPlayer(player));
    }

    // No inventory drop + drop powerups + call match death event
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e) {
        Player player = e.getPlayer();
        ItemStack handItem = player.getInventory().getItemInMainHand().clone();
        ItemMeta itemMeta = handItem.getItemMeta();
        QuakeUserState userState = QuakePlugin.INSTANCE.userStates.get(player);
        userState.armor = 0;
        if (itemMeta != null && itemMeta.hasCustomModelData() && handItem.getType() == Material.CARROT_ON_A_STICK) {
            int modelData = itemMeta.getCustomModelData();
            WeaponUserState weaponState = userState.weaponState;
            PersistentDataContainer pdc = itemMeta.getPersistentDataContainer();
            pdc.set(new NamespacedKey(QuakePlugin.INSTANCE, "ammo"), PersistentDataType.INTEGER, weaponState.ammo[modelData]);
            handItem.setItemMeta(itemMeta);
        }

        Powerup.dropPowerups(player);

        Collection<ItemStack> drops = e.getDrops();
        drops.clear();
        drops.add(handItem);

        if (userState.currentMatch != null) {
            e.deathMessage(Component.empty());
        }
    }

    public static void sortGun(ItemStack gunItem, Player player) {
        int modelData = gunItem.getItemMeta().getCustomModelData();
        PlayerInventory inv = player.getInventory();
        WeaponUserState weaponState = QuakePlugin.INSTANCE.userStates.get(player).weaponState;

        // Find the gun, regardless of its NBT/PDC
        Optional<ItemStack> foundGun = Arrays.stream(inv.getContents()).filter(Objects::nonNull).filter(
                invItem -> invItem.getItemMeta().hasCustomModelData() && invItem.getItemMeta().getCustomModelData() == modelData
        ).findAny();

        PersistentDataContainer pdc = gunItem.getItemMeta().getPersistentDataContainer();
        Integer ammo = pdc.get(new NamespacedKey(QuakePlugin.INSTANCE, "ammo"), PersistentDataType.INTEGER);
        if (foundGun.isPresent()) {
            if (ammo != null) {
                weaponState.ammo[modelData] += ammo;
            } else {
                if (weaponState.ammo[modelData] < WeaponUtil.DEFAULT_AMMO[modelData])
                    weaponState.ammo[modelData] = WeaponUtil.DEFAULT_AMMO[modelData];
                else
                    weaponState.ammo[modelData] += 1;
            }
        } else {
            if (ammo != null && ammo > WeaponUtil.DEFAULT_AMMO[modelData]) {
                weaponState.ammo[modelData] += ammo;
            } else {
                if (weaponState.ammo[modelData] < WeaponUtil.DEFAULT_AMMO[modelData])
                    weaponState.ammo[modelData] = WeaponUtil.DEFAULT_AMMO[modelData];
                else
                    weaponState.ammo[modelData] += 1;
            }

            inv.setItem(modelData, gunItem);
            inv.setHeldItemSlot(modelData);
        }

        if (weaponState.ammo[modelData] > 200)
            weaponState.ammo[modelData] = 200;
    }

    // Sort picked up gun
    @EventHandler
    public void onItemPickup(EntityPickupItemEvent event) {
        ItemStack item = event.getItem().getItemStack();
        LivingEntity entity = event.getEntity();
        if (!(item.getItemMeta().hasCustomModelData() && item.getType() == Material.CARROT_ON_A_STICK && entity instanceof Player player)) return;

        sortGun(item, player);
        event.getItem().remove();
        player.playSound(entity, "quake.weapons.pickup", 0.5f,  1f);
        Hud.pickupMessage(player, item.getItemMeta().displayName());
        event.setCancelled(true);
    }

    // No offhand (default key is F)
    @EventHandler
    public void onHandSwap(PlayerSwapHandItemsEvent event) {
        event.setCancelled(true);
        Player player = event.getPlayer();
        player.sendMessage(TranslationManager.t("error.offhand", player));
    }

    // Also no offhand, but in inventory
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getSlot() != 40)
            return;

        Player player = (Player) event.getView().getPlayer();
        event.getView().getPlayer().sendMessage(TranslationManager.t("error.offhand", player));
        // TODO find a more reliable fix
        ItemStack cursorItem = event.getCursor().clone();
        event.setCancelled(true);
        event.getView().getPlayer().getInventory().addItem(cursorItem);
    }

    // Cancel sprint speed
    @EventHandler
    public void onSprint(PlayerToggleSprintEvent event) {
        boolean sprinting = event.isSprinting();
        Player player = event.getPlayer();

        player.setWalkSpeed(sprinting ? 0.3076923077f : 0.4f);
    }
}
