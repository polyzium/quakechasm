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

import com.github.polyzium.quakechasm.QuakePlugin;
import com.github.polyzium.quakechasm.QuakeUserState;
import com.github.polyzium.quakechasm.game.entities.Trigger;
import com.github.polyzium.quakechasm.game.entities.triggers.Jumppad;
import com.github.polyzium.quakechasm.game.mapper.EntityTool;
import com.github.polyzium.quakechasm.game.mapper.JumppadTool;
import com.github.polyzium.quakechasm.game.mapper.PortalTool;
import com.github.polyzium.quakechasm.game.mapper.SpawnerTool;
import com.github.polyzium.quakechasm.game.entities.triggers.Portal;
import com.github.polyzium.quakechasm.matchmaking.Team;
import com.github.polyzium.quakechasm.misc.MiscUtil;
import com.github.polyzium.quakechasm.misc.TranslationManager;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.title.Title;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.time.Duration;
import java.util.ArrayList;

import static com.github.polyzium.quakechasm.game.mapper.EntityTool.drawLine;

public class MapperToolListener implements Listener {
    
    private static final String ENTITY_TOOL_KEY = "entity_tool";
    private static final String JUMPPAD_TOOL_KEY = "jumppad_tool";
    private static final String SPAWNPOINT_TOOL_KEY = "spawnpoint_tool";
    private static final String PORTAL_TOOL_KEY = "portal_tool";

    public static final int TOOL_UPDATE_TICKS = 4;

    public static boolean isEntityTool(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        
        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().has(
            new NamespacedKey(QuakePlugin.INSTANCE, ENTITY_TOOL_KEY),
            PersistentDataType.BYTE
        );
    }

    public static ItemStack createEntityTool() {
        ItemStack tool = new ItemStack(Material.BLAZE_ROD);
        ItemMeta meta = tool.getItemMeta();
        
        meta.displayName(TranslationManager.t("mapper.tool.entity.name", TranslationManager.FALLBACK)
            .color(net.kyori.adventure.text.format.NamedTextColor.GOLD)
            .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
        
        meta.lore(java.util.List.of(
            TranslationManager.t("mapper.tool.entity.lore.leftClick", TranslationManager.FALLBACK)
                .color(net.kyori.adventure.text.format.NamedTextColor.GRAY)
                .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false),
            TranslationManager.t("mapper.tool.entity.lore.rightClick", TranslationManager.FALLBACK)
                .color(net.kyori.adventure.text.format.NamedTextColor.GRAY)
                .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false)
        ));
        
        meta.getPersistentDataContainer().set(
            new NamespacedKey(QuakePlugin.INSTANCE, ENTITY_TOOL_KEY),
            PersistentDataType.BYTE,
            (byte) 1
        );
        
        tool.setItemMeta(meta);
        return tool;
    }

    public static boolean isJumppadTool(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        
        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().has(
            new NamespacedKey(QuakePlugin.INSTANCE, JUMPPAD_TOOL_KEY),
            PersistentDataType.BYTE
        );
    }

    public static ItemStack createJumppadTool() {
        ItemStack tool = new ItemStack(Material.STICK);
        ItemMeta meta = tool.getItemMeta();
        
        meta.displayName(TranslationManager.t("mapper.tool.jumppad.name", TranslationManager.FALLBACK)
            .color(net.kyori.adventure.text.format.NamedTextColor.AQUA)
            .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
        
        meta.lore(java.util.List.of(
            TranslationManager.t("mapper.tool.jumppad.lore.pointAtJumppad", TranslationManager.FALLBACK)
                .color(net.kyori.adventure.text.format.NamedTextColor.GRAY)
                .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false),
            TranslationManager.t("mapper.tool.jumppad.lore.leftClickEdit", TranslationManager.FALLBACK)
                .color(net.kyori.adventure.text.format.NamedTextColor.GRAY)
                .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false),
            TranslationManager.t("mapper.tool.jumppad.lore.pointAtBlock", TranslationManager.FALLBACK)
                .color(net.kyori.adventure.text.format.NamedTextColor.GRAY)
                .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false),
            TranslationManager.t("mapper.tool.jumppad.lore.rightClickPlace", TranslationManager.FALLBACK)
                .color(net.kyori.adventure.text.format.NamedTextColor.GRAY)
                .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false),
            TranslationManager.t("mapper.tool.jumppad.lore.rightClickLanding", TranslationManager.FALLBACK)
                .color(net.kyori.adventure.text.format.NamedTextColor.GRAY)
                .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false),
            TranslationManager.t("mapper.tool.jumppad.lore.leftClickCancel", TranslationManager.FALLBACK)
                .color(net.kyori.adventure.text.format.NamedTextColor.GRAY)
                .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false)
        ));
        
        meta.getPersistentDataContainer().set(
            new NamespacedKey(QuakePlugin.INSTANCE, JUMPPAD_TOOL_KEY),
            PersistentDataType.BYTE,
            (byte) 1
        );
        
        tool.setItemMeta(meta);
        return tool;
    }

    public static boolean isSpawnpointTool(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        
        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().has(
            new NamespacedKey(QuakePlugin.INSTANCE, SPAWNPOINT_TOOL_KEY),
            PersistentDataType.STRING
        );
    }

    public static Team getSpawnpointTeam(ItemStack item) {
        if (!isSpawnpointTool(item)) {
            return null;
        }
        
        ItemMeta meta = item.getItemMeta();
        String teamName = meta.getPersistentDataContainer().get(
            new NamespacedKey(QuakePlugin.INSTANCE, SPAWNPOINT_TOOL_KEY),
            PersistentDataType.STRING
        );
        
        try {
            return Team.valueOf(teamName);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static ItemStack createSpawnpointTool(Team team) {
        ItemStack tool = new ItemStack(Material.LEATHER_CHESTPLATE);
        LeatherArmorMeta meta = (LeatherArmorMeta) tool.getItemMeta();

        int colorRGB = switch (team) {
            case RED -> 0xb02e26;
            case BLUE -> 0x3c44aa;
            case FREE -> 0xfed83d;
            case SPECTATOR -> 0x8932b8;
        };
        meta.setColor(Color.fromRGB(colorRGB));

        String teamKey = switch (team) {
            case RED -> "mapper.tool.spawnpoint.name.red";
            case BLUE -> "mapper.tool.spawnpoint.name.blue";
            case FREE -> "mapper.tool.spawnpoint.name.free";
            case SPECTATOR -> "mapper.tool.spawnpoint.name.spectator";
        };
        
        String teamNameKey = switch (team) {
            case RED -> "mapper.tool.spawnpoint.team.red";
            case BLUE -> "mapper.tool.spawnpoint.team.blue";
            case FREE -> "mapper.tool.spawnpoint.team.free";
            case SPECTATOR -> "mapper.tool.spawnpoint.team.spectator";
        };
        
        meta.displayName(TranslationManager.t(teamKey, TranslationManager.FALLBACK)
            .color(net.kyori.adventure.text.format.NamedTextColor.GOLD)
            .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
        
        meta.lore(java.util.List.of(
            TranslationManager.t("mapper.tool.spawnpoint.lore.rightClick", TranslationManager.FALLBACK)
                .color(net.kyori.adventure.text.format.NamedTextColor.GRAY)
                .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false),
            TranslationManager.t("mapper.tool.spawnpoint.lore.team", TranslationManager.FALLBACK,
                Placeholder.unparsed("team", TranslationManager.tLegacy(teamNameKey, TranslationManager.FALLBACK)))
                .color(net.kyori.adventure.text.format.NamedTextColor.GRAY)
                .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false)
        ));

        meta.getPersistentDataContainer().set(
            new NamespacedKey(QuakePlugin.INSTANCE, SPAWNPOINT_TOOL_KEY),
            PersistentDataType.STRING,
            team.name()
        );
        
        tool.setItemMeta(meta);
        return tool;
    }
    
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (isEntityTool(item)) {
            handleEntityToolInteraction(event, player);
            return;
        }

        if (isJumppadTool(item)) {
            handleJumppadToolInteraction(event, player);
            return;
        }

        if (isSpawnpointTool(item)) {
            handleSpawnpointToolInteraction(event, player, item);
            return;
        }

        if (PortalTool.isPortalTool(item)) {
            handlePortalToolInteraction(event, player);
            return;
        }

        if (SpawnerTool.isSpawnerTool(item)) {
            handleSpawnerToolInteraction(event, player, item);
            return;
        }
    }
    
    private void handleSpawnpointToolInteraction(PlayerInteractEvent event, Player player, ItemStack item) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        
        event.setCancelled(true);
        
        Team team = getSpawnpointTeam(item);
        if (team == null) {
            player.sendMessage(TranslationManager.t("mapper.tool.spawnpoint.message.invalid", player)
                .color(net.kyori.adventure.text.format.NamedTextColor.RED));
            return;
        }

        Location clickedBlock = event.getClickedBlock().getLocation();

        Location spawnLoc = clickedBlock.clone().add(0.5, 1, 0.5);

        float playerYaw = player.getLocation().getYaw();
        playerYaw += 180.0f;
        playerYaw = (playerYaw % 360 + 360) % 360;
        float snappedYaw = Math.round(playerYaw / 45.0f) * 45.0f;
        
        spawnLoc.setYaw(snappedYaw);

        ArmorStand armorStand = (ArmorStand) player.getWorld().spawnEntity(spawnLoc, EntityType.ARMOR_STAND);
        armorStand.setGravity(false);
        armorStand.setVisible(true);
        armorStand.setBasePlate(true);
        armorStand.setArms(false);

        ItemStack chestplate = new ItemStack(Material.LEATHER_CHESTPLATE);
        LeatherArmorMeta chestplateMeta = (LeatherArmorMeta) chestplate.getItemMeta();
        int chestplateColor = switch (team) {
            case RED -> 0xb02e26;
            case BLUE -> 0x3c44aa;
            case FREE -> 0xfed83d;
            case SPECTATOR -> 0x8932b8;
        };
        chestplateMeta.setColor(Color.fromRGB(chestplateColor));
        chestplate.setItemMeta(chestplateMeta);
        
        EntityEquipment equipment = armorStand.getEquipment();
        equipment.setChestplate(chestplate);
        
        String teamNameKey = switch (team) {
            case RED -> "mapper.tool.spawnpoint.team.red";
            case BLUE -> "mapper.tool.spawnpoint.team.blue";
            case FREE -> "mapper.tool.spawnpoint.team.free";
            case SPECTATOR -> "mapper.tool.spawnpoint.team.spectator";
        };
        
        player.sendMessage(TranslationManager.t("mapper.tool.spawnpoint.message.placed", player,
            Placeholder.unparsed("team", TranslationManager.tLegacy(teamNameKey, player)),
            Placeholder.unparsed("location", String.format("%.1f %.1f %.1f",
                spawnLoc.getX(), spawnLoc.getY(), spawnLoc.getZ())))
            .color(net.kyori.adventure.text.format.NamedTextColor.GREEN));
    }
    
    private void handleEntityToolInteraction(PlayerInteractEvent event, Player player) {
        event.setCancelled(true);
        
        QuakeUserState userState = QuakePlugin.INSTANCE.userStates.get(player);
        if (userState == null) {
            return;
        }
        
        Action action = event.getAction();
        
        if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
            if (userState.movingEntity != null) {
                userState.movingEntity = null;
                player.sendMessage(TranslationManager.t("mapper.tool.entity.message.cancelled", player)
                    .color(net.kyori.adventure.text.format.NamedTextColor.YELLOW));
            } else {
                Trigger target = EntityTool.getTargetEntity(player);
                EntityTool.removeEntity(player, target);
            }
        } else if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            if (userState.movingEntity == null) {
                Trigger target = EntityTool.getTargetEntity(player);
                if (target != null) {
                    userState.movingEntity = target;
                    EntityTool.startMovingEntity(player, target);
                }
            } else {
                org.bukkit.util.RayTraceResult blockRayTrace = player.rayTraceBlocks(10.0);
                
                if (blockRayTrace != null && blockRayTrace.getHitBlock() != null) {
                    org.bukkit.util.Vector hitPosition = blockRayTrace.getHitPosition();

                    Location newLocation = hitPosition.toLocation(player.getWorld());

                    org.bukkit.util.BoundingBox offsetBox = userState.movingEntity.getOffsetBoundingBox();
                    double minYOffset = offsetBox != null ? -offsetBox.getMinY() : 1.0;

                    newLocation.set(
                        Math.floor(newLocation.x()) + 0.5,
                        Math.floor(newLocation.y()) + minYOffset,
                        Math.floor(newLocation.z()) + 0.5
                    );
                    
                    EntityTool.stopMovingEntity(player, userState.movingEntity, newLocation);
                } else {
                    player.sendMessage(TranslationManager.t("mapper.tool.entity.message.noBlock", player)
                        .color(net.kyori.adventure.text.format.NamedTextColor.RED));
                }
                
                userState.movingEntity = null;
            }
        }
    }
    
    private void handleJumppadToolInteraction(PlayerInteractEvent event, Player player) {
        event.setCancelled(true);
        
        QuakeUserState userState = QuakePlugin.INSTANCE.userStates.get(player);
        if (userState == null) {
            return;
        }
        
        Action action = event.getAction();
        
        if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
            if (userState.jumppadPlacementLoc != null) {
                userState.jumppadPlacementLoc = null;
                player.sendMessage(TranslationManager.t("mapper.tool.jumppad.message.cancelled", player)
                    .color(net.kyori.adventure.text.format.NamedTextColor.YELLOW));
            } else if (userState.settingLandingPos && userState.editingJumppad != null) {
                userState.editingJumppad = null;
                userState.settingLandingPos = false;
                player.sendMessage(TranslationManager.t("mapper.tool.jumppad.message.editCancelled", player)
                    .color(net.kyori.adventure.text.format.NamedTextColor.YELLOW));
            } else {
                Trigger target = EntityTool.getTargetEntity(player);
                if (target instanceof Jumppad jumppad) {
                    userState.editingJumppad = jumppad;
                    userState.settingLandingPos = true;
                    player.sendMessage(TranslationManager.t("mapper.tool.jumppad.message.clickLanding", player)
                        .color(net.kyori.adventure.text.format.NamedTextColor.GREEN));
                } else {
                    player.sendMessage(TranslationManager.t("mapper.tool.jumppad.message.pointAtJumppad", player)
                        .color(net.kyori.adventure.text.format.NamedTextColor.RED));
                }
            }
        } else if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            org.bukkit.util.RayTraceResult blockRayTrace = player.rayTraceBlocks(100.0);
            
            if (blockRayTrace == null || blockRayTrace.getHitBlock() == null) {
                player.sendMessage(TranslationManager.t("mapper.tool.jumppad.message.noBlock", player)
                    .color(net.kyori.adventure.text.format.NamedTextColor.RED));
                return;
            }
            
            org.bukkit.util.Vector hitPosition = blockRayTrace.getHitPosition();
            Location targetLoc = hitPosition.toLocation(player.getWorld());

            targetLoc.set(
                Math.floor(targetLoc.x()) + 0.5,
                Math.floor(targetLoc.y()),
                Math.floor(targetLoc.z()) + 0.5
            );
            
            if (userState.settingLandingPos && userState.editingJumppad != null) {
                Location jumppadLoc = userState.editingJumppad.getLocation();
                Vector launchVec = JumppadTool.calculateLaunchVector(jumppadLoc, targetLoc, userState.jumppadPowerMultiplier);
                
                if (launchVec != null) {
                    userState.editingJumppad.remove();
                    QuakePlugin.INSTANCE.triggers.remove(userState.editingJumppad);

                    new Jumppad(jumppadLoc, launchVec);
                    
                    player.sendMessage(TranslationManager.t("mapper.tool.jumppad.message.updated", player,
                        Placeholder.unparsed("location", String.format("%.1f %.1f %.1f", targetLoc.getX(), targetLoc.getY(), targetLoc.getZ())),
                        Placeholder.unparsed("power", String.format("%.1f", userState.jumppadPowerMultiplier)))
                        .color(net.kyori.adventure.text.format.NamedTextColor.GREEN));
                    
                    userState.editingJumppad = null;
                    userState.settingLandingPos = false;
                } else {
                    player.showTitle(Title.title(
                        net.kyori.adventure.text.Component.empty(),
                        TranslationManager.t("mapper.tool.jumppad.message.trajectoryFailed", player)
                            .color(net.kyori.adventure.text.format.NamedTextColor.RED),
                        Title.Times.times(Duration.ZERO, Duration.ofMillis(1000), Duration.ofMillis(200))
                    ));
                }
            } else if (userState.jumppadPlacementLoc == null) {
                userState.jumppadPlacementLoc = targetLoc;
                player.sendMessage(TranslationManager.t("mapper.tool.jumppad.message.locationSet", player,
                    Placeholder.unparsed("location", String.format("%.1f %.1f %.1f", targetLoc.getX(), targetLoc.getY(), targetLoc.getZ())))
                    .color(net.kyori.adventure.text.format.NamedTextColor.GREEN));
            } else {
                Location jumppadLoc = userState.jumppadPlacementLoc;
                Vector launchVec = JumppadTool.calculateLaunchVector(jumppadLoc, targetLoc, userState.jumppadPowerMultiplier);
                
                if (launchVec != null) {
                    new Jumppad(jumppadLoc, launchVec);
                    
                    player.sendMessage(TranslationManager.t("mapper.tool.jumppad.message.created", player,
                        Placeholder.unparsed("location1", String.format("%.1f %.1f %.1f", jumppadLoc.getX(), jumppadLoc.getY(), jumppadLoc.getZ())),
                        Placeholder.unparsed("location2", String.format("%.1f %.1f %.1f", targetLoc.getX(), targetLoc.getY(), targetLoc.getZ())),
                        Placeholder.unparsed("power", String.format("%.1f", userState.jumppadPowerMultiplier)))
                        .color(net.kyori.adventure.text.format.NamedTextColor.GREEN));
                    
                    userState.jumppadPlacementLoc = null;
                } else {
                    player.showTitle(Title.title(
                        net.kyori.adventure.text.Component.empty(),
                        TranslationManager.t("mapper.tool.jumppad.message.trajectoryFailed", player)
                            .color(net.kyori.adventure.text.format.NamedTextColor.RED),
                        Title.Times.times(Duration.ZERO, Duration.ofMillis(1000), Duration.ofMillis(200))
                    ));
                }
            }
        }
    }
    
    @EventHandler
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        ItemStack newItem = player.getInventory().getItem(event.getNewSlot());
        ItemStack oldItem = player.getInventory().getItem(event.getPreviousSlot());
        
        QuakeUserState userState = QuakePlugin.INSTANCE.userStates.get(player);

        if (isJumppadTool(oldItem) && (userState.jumppadPlacementLoc != null || userState.settingLandingPos)) {
            int slotDiff = event.getNewSlot() - event.getPreviousSlot();

            if (slotDiff > 5) {
                slotDiff -= 9;
            } else if (slotDiff < -5) {
                slotDiff += 9;
            }

            userState.jumppadPowerMultiplier += slotDiff * 0.1;
            userState.jumppadPowerMultiplier = Math.max(0.1, Math.min(10.0, userState.jumppadPowerMultiplier));

            JumppadTool.displayPowerMultiplier(player, userState.jumppadPowerMultiplier);

            event.setCancelled(true);
            return;
        }

        if (isEntityTool(newItem)) {
            userState.holdingEntityTool = true;
            startEntityToolVisualization(player, userState);
            return;
        } else {
            userState.holdingEntityTool = false;
        }

        if (isJumppadTool(newItem)) {
            startJumppadToolVisualization(player, userState);
            return;
        }

        if (PortalTool.isPortalTool(newItem)) {
            startPortalToolVisualization(player, userState);
            return;
        }
    }
    
    private void startEntityToolVisualization(Player player, QuakeUserState userState) {
        new BukkitRunnable() {
            @Override
            public void run() {
                ItemStack currentItem = player.getInventory().getItemInMainHand();
                if (!isEntityTool(currentItem)) {
                    this.cancel();
                    return;
                }

                Trigger target = EntityTool.getTargetEntity(player);
                EntityTool.displayEntityInfo(player, target);

                if (userState.movingEntity != null) {
                    EntityTool.visualizeEntityWithColor(player, userState.movingEntity, org.bukkit.Color.fromRGB(0xFFFF00));

                    org.bukkit.util.RayTraceResult blockRayTrace = player.rayTraceBlocks(10.0);
                    if (blockRayTrace != null && blockRayTrace.getHitBlock() != null) {
                        org.bukkit.util.Vector hitPosition = blockRayTrace.getHitPosition();
                        
                        Location targetLocation = hitPosition.toLocation(player.getWorld());

                        org.bukkit.util.BoundingBox offsetBox = userState.movingEntity.getOffsetBoundingBox();
                        double minYOffset = offsetBox != null ? -offsetBox.getMinY() : 1.0;
                        
                        targetLocation.set(
                            Math.floor(targetLocation.x()) + 0.5,
                            Math.floor(targetLocation.y()) + minYOffset,
                            Math.floor(targetLocation.z()) + 0.5
                        );

                        if (offsetBox != null) {
                            org.bukkit.util.BoundingBox previewBox = offsetBox.clone().shift(targetLocation.toVector());
                            EntityTool.drawBoundingBox(player.getWorld(), previewBox, org.bukkit.Color.fromRGB(0x00FF00), TOOL_UPDATE_TICKS, 16);
                        }
                    }
                } else if (target != null) {
                    EntityTool.visualizeTargetEntity(player, target);
                }
            }
        }.runTaskTimer(QuakePlugin.INSTANCE, 0, TOOL_UPDATE_TICKS);
    }
    
    private void startJumppadToolVisualization(Player player, QuakeUserState userState) {
        new BukkitRunnable() {
            @Override
            public void run() {
                ItemStack currentItem = player.getInventory().getItemInMainHand();
                if (!isJumppadTool(currentItem)) {
                    this.cancel();
                    return;
                }

                org.bukkit.util.RayTraceResult blockRayTrace = player.rayTraceBlocks(100.0);
                
                if (userState.settingLandingPos && userState.editingJumppad != null) {
                    JumppadTool.visualizeTrajectory(
                        userState.editingJumppad.getLocation(),
                        userState.editingJumppad.getLaunchVec(),
                        org.bukkit.Color.fromRGB(0xFFFF00),
                        TOOL_UPDATE_TICKS
                    );

                    if (blockRayTrace != null && blockRayTrace.getHitBlock() != null) {
                        Location targetLoc = blockRayTrace.getHitPosition().toLocation(player.getWorld());
                        targetLoc.set(
                            Math.floor(targetLoc.x()) + 0.5,
                            Math.floor(targetLoc.y()),
                            Math.floor(targetLoc.z()) + 0.5
                        );
                        
                        Vector newLaunchVec = JumppadTool.calculateLaunchVector(
                            userState.editingJumppad.getLocation(),
                            targetLoc,
                            userState.jumppadPowerMultiplier
                        );
                        
                        if (newLaunchVec != null) {
                            JumppadTool.visualizeTrajectory(
                                userState.editingJumppad.getLocation(),
                                newLaunchVec,
                                org.bukkit.Color.fromRGB(0x00FF00),
                                TOOL_UPDATE_TICKS
                            );
                        }
                    }

                } else if (userState.jumppadPlacementLoc != null) {
                    Location jpLoc = userState.jumppadPlacementLoc;
                    jpLoc.getWorld().spawnParticle(
                        org.bukkit.Particle.TRAIL,
                        jpLoc.clone().add(0, 0.5, 0),
                        1, 0, 0, 0, 0,
                        new org.bukkit.Particle.Trail(jpLoc, org.bukkit.Color.fromRGB(0xFFFF00), 20)
                    );

                    if (blockRayTrace != null && blockRayTrace.getHitBlock() != null) {
                        Location targetLoc = blockRayTrace.getHitPosition().toLocation(player.getWorld());
                        targetLoc.set(
                            Math.floor(targetLoc.x()) + 0.5,
                            Math.floor(targetLoc.y()),
                            Math.floor(targetLoc.z()) + 0.5
                        );
                        
                        Vector launchVec = JumppadTool.calculateLaunchVector(jpLoc, targetLoc, userState.jumppadPowerMultiplier);
                        if (launchVec != null) {
                            JumppadTool.visualizeTrajectory(jpLoc, launchVec, org.bukkit.Color.fromRGB(0x00FF00), TOOL_UPDATE_TICKS);
                        }
                    }
                } else {
                    Trigger target = EntityTool.getTargetEntity(player);
                    if (target instanceof Jumppad jumppad) {
                        JumppadTool.visualizeTrajectory(
                            jumppad.getLocation(),
                            jumppad.getLaunchVec(),
                            org.bukkit.Color.fromRGB(0x0077FF),
                            20
                        );
                        JumppadTool.displayJumppadInfo(player, jumppad);
                    } else if (userState.jumppadPlacementLoc != null || userState.settingLandingPos) {
                        JumppadTool.displayPowerMultiplier(player, userState.jumppadPowerMultiplier);
                    }
                }
            }
        }.runTaskTimer(QuakePlugin.INSTANCE, 0, TOOL_UPDATE_TICKS);
    }
    
    private void handlePortalToolInteraction(PlayerInteractEvent event, Player player) {
        event.setCancelled(true);
        
        QuakeUserState userState = QuakePlugin.INSTANCE.userStates.get(player);
        if (userState == null) {
            return;
        }
        
        PortalTool.PortalToolData toolData = userState.portalToolData;
        Action action = event.getAction();
        
        if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
            if (toolData.state == PortalTool.PortalToolState.PLACING_PORTAL) {
                toolData.reset();
                player.sendMessage(TranslationManager.t("mapper.tool.portal.message.placementCancelled", player)
                    .color(net.kyori.adventure.text.format.NamedTextColor.YELLOW));
            } else if (toolData.state == PortalTool.PortalToolState.SETTING_TARGET) {
                toolData.reset();
                player.sendMessage(TranslationManager.t("mapper.tool.portal.message.targetCancelled", player)
                    .color(net.kyori.adventure.text.format.NamedTextColor.YELLOW));
            } else if (toolData.state == PortalTool.PortalToolState.EDITING_TARGET) {
                toolData.reset();
                player.sendMessage(TranslationManager.t("mapper.tool.portal.message.editCancelled", player)
                    .color(net.kyori.adventure.text.format.NamedTextColor.YELLOW));
            } else {
                Trigger target = EntityTool.getTargetEntity(player);
                if (target instanceof Portal portal) {
                    toolData.state = PortalTool.PortalToolState.EDITING_TARGET;
                    toolData.editingPortal = portal;
                    player.sendMessage(TranslationManager.t("mapper.tool.portal.message.clickTarget", player)
                        .color(net.kyori.adventure.text.format.NamedTextColor.GREEN));
                } else {
                    player.sendMessage(TranslationManager.t("mapper.tool.portal.message.pointAtPortal", player)
                        .color(net.kyori.adventure.text.format.NamedTextColor.RED));
                }
            }
        } else if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            org.bukkit.util.RayTraceResult blockRayTrace = player.rayTraceBlocks(100.0);
            
            if (blockRayTrace == null || blockRayTrace.getHitBlock() == null) {
                player.sendMessage(TranslationManager.t("mapper.tool.portal.message.noBlock", player)
                    .color(net.kyori.adventure.text.format.NamedTextColor.RED));
                return;
            }
            
            org.bukkit.util.Vector hitPosition = blockRayTrace.getHitPosition();
            Location targetLoc = hitPosition.toLocation(player.getWorld());

            targetLoc.set(
                Math.floor(targetLoc.x()) + 0.5,
                Math.floor(targetLoc.y()),
                Math.floor(targetLoc.z()) + 0.5
            );

            float exitYaw = player.getLocation().getYaw();
            exitYaw = (exitYaw % 360 + 360) % 360;
            exitYaw = Math.round(exitYaw / 45.0f) * 45.0f;
            targetLoc.setYaw(exitYaw);
            targetLoc.setPitch(0.0f);
            
            if (toolData.state == PortalTool.PortalToolState.EDITING_TARGET) {
                Location portalLoc = toolData.editingPortal.getLocation();

                toolData.editingPortal.remove();
                QuakePlugin.INSTANCE.triggers.remove(toolData.editingPortal);

                new Portal(portalLoc, targetLoc);
                
                player.sendMessage(TranslationManager.t("mapper.tool.portal.message.targetUpdated", player,
                    Placeholder.unparsed("location", String.format("%.1f %.1f %.1f", targetLoc.getX(), targetLoc.getY(), targetLoc.getZ())))
                    .color(net.kyori.adventure.text.format.NamedTextColor.GREEN));
                
                toolData.reset();
            } else if (toolData.state == PortalTool.PortalToolState.SETTING_TARGET) {
                Location portalLoc = toolData.portalLocation;
                
                new Portal(portalLoc, targetLoc);
                
                player.sendMessage(TranslationManager.t("mapper.tool.portal.message.created", player,
                    Placeholder.unparsed("location1", String.format("%.1f %.1f %.1f", portalLoc.getX(), portalLoc.getY(), portalLoc.getZ())),
                    Placeholder.unparsed("location2", String.format("%.1f %.1f %.1f", targetLoc.getX(), targetLoc.getY(), targetLoc.getZ())))
                    .color(net.kyori.adventure.text.format.NamedTextColor.GREEN));
                
                toolData.reset();
            } else {
                toolData.state = PortalTool.PortalToolState.SETTING_TARGET;
                toolData.portalLocation = targetLoc;
                player.sendMessage(TranslationManager.t("mapper.tool.portal.message.locationSet", player,
                    Placeholder.unparsed("location", String.format("%.1f %.1f %.1f", targetLoc.getX(), targetLoc.getY(), targetLoc.getZ())))
                    .color(net.kyori.adventure.text.format.NamedTextColor.GREEN));
            }
        }
    }
    
    private void startPortalToolVisualization(Player player, QuakeUserState userState) {
        new BukkitRunnable() {
            @Override
            public void run() {
                ItemStack currentItem = player.getInventory().getItemInMainHand();
                if (!PortalTool.isPortalTool(currentItem)) {
                    this.cancel();
                    return;
                }
                
                PortalTool.PortalToolData toolData = userState.portalToolData;

                org.bukkit.util.RayTraceResult blockRayTrace = player.rayTraceBlocks(100.0);
                
                if (toolData.state == PortalTool.PortalToolState.EDITING_TARGET) {
                    Location portalLoc = toolData.editingPortal.getLocation();
                    org.bukkit.util.BoundingBox portalBox = new org.bukkit.util.BoundingBox(
                        portalLoc.getX() - 0.5, portalLoc.getY(), portalLoc.getZ() - 0.5,
                        portalLoc.getX() + 0.5, portalLoc.getY() + 2, portalLoc.getZ() + 0.5
                    );
                    EntityTool.drawBoundingBox(player.getWorld(), portalBox, org.bukkit.Color.fromRGB(0xFFFF00), TOOL_UPDATE_TICKS, 16);

                    if (blockRayTrace != null && blockRayTrace.getHitBlock() != null) {
                        Location targetLoc = blockRayTrace.getHitPosition().toLocation(player.getWorld());
                        targetLoc.set(
                            Math.floor(targetLoc.x()) + 0.5,
                            Math.floor(targetLoc.y()),
                            Math.floor(targetLoc.z()) + 0.5
                        );

                        float exitYaw = player.getLocation().getYaw();
                        exitYaw = (exitYaw % 360 + 360) % 360;
                        exitYaw = Math.round(exitYaw / 45.0f) * 45.0f;
                        
                        org.bukkit.util.BoundingBox targetBox = new org.bukkit.util.BoundingBox(
                            targetLoc.getX() - 0.5, targetLoc.getY(), targetLoc.getZ() - 0.5,
                            targetLoc.getX() + 0.5, targetLoc.getY() + 2, targetLoc.getZ() + 0.5
                        );
                        EntityTool.drawBoundingBox(player.getWorld(), targetBox, org.bukkit.Color.fromRGB(0x00FF00), TOOL_UPDATE_TICKS, 16);

                        EntityTool.drawLine(player.getWorld(),
                            portalLoc.getX(), portalLoc.getY() + 1, portalLoc.getZ(),
                            targetLoc.getX(), targetLoc.getY() + 1, targetLoc.getZ(),
                            org.bukkit.Color.fromRGB(0xFFFFFF), TOOL_UPDATE_TICKS, 16);

                        Location eyePos = targetLoc.clone().add(0, 1.62, 0);
                        double angleRadians = Math.toRadians(exitYaw);
                        Location angleEnd = eyePos.clone().add(
                            -Math.sin(angleRadians) * 1.5,
                            0,
                            Math.cos(angleRadians) * 1.5
                        );
                        EntityTool.drawLine(player.getWorld(),
                            eyePos.getX(), eyePos.getY(), eyePos.getZ(),
                            angleEnd.getX(), angleEnd.getY(), angleEnd.getZ(),
                            org.bukkit.Color.fromRGB(0xFF0000), TOOL_UPDATE_TICKS, 16);
                    }
                } else if (toolData.state == PortalTool.PortalToolState.SETTING_TARGET) {
                    Location portalLoc = toolData.portalLocation;
                    org.bukkit.util.BoundingBox portalBox = new org.bukkit.util.BoundingBox(
                        portalLoc.getX() - 0.5, portalLoc.getY(), portalLoc.getZ() - 0.5,
                        portalLoc.getX() + 0.5, portalLoc.getY() + 2, portalLoc.getZ() + 0.5
                    );
                    EntityTool.drawBoundingBox(player.getWorld(), portalBox, org.bukkit.Color.fromRGB(0xFFFF00), TOOL_UPDATE_TICKS, 16);

                    if (blockRayTrace != null && blockRayTrace.getHitBlock() != null) {
                        Location targetLoc = blockRayTrace.getHitPosition().toLocation(player.getWorld());
                        targetLoc.set(
                            Math.floor(targetLoc.x()) + 0.5,
                            Math.floor(targetLoc.y()),
                            Math.floor(targetLoc.z()) + 0.5
                        );

                        float exitYaw = player.getLocation().getYaw();
                        exitYaw = (exitYaw % 360 + 360) % 360;
                        exitYaw = Math.round(exitYaw / 45.0f) * 45.0f;
                        
                        org.bukkit.util.BoundingBox targetBox = new org.bukkit.util.BoundingBox(
                            targetLoc.getX() - 0.5, targetLoc.getY(), targetLoc.getZ() - 0.5,
                            targetLoc.getX() + 0.5, targetLoc.getY() + 2, targetLoc.getZ() + 0.5
                        );
                        EntityTool.drawBoundingBox(player.getWorld(), targetBox, org.bukkit.Color.fromRGB(0x00FF00), TOOL_UPDATE_TICKS, 16);

                        EntityTool.drawLine(player.getWorld(),
                            portalLoc.getX(), portalLoc.getY() + 1, portalLoc.getZ(),
                            targetLoc.getX(), targetLoc.getY() + 1, targetLoc.getZ(),
                            org.bukkit.Color.fromRGB(0xFFFFFF), TOOL_UPDATE_TICKS, 16);

                        Location eyePos = targetLoc.clone().add(0, 1.62, 0);
                        double angleRadians = Math.toRadians(exitYaw);
                        Location angleEnd = eyePos.clone().add(
                            -Math.sin(angleRadians) * 1.5,
                            0,
                            Math.cos(angleRadians) * 1.5
                        );
                        EntityTool.drawLine(player.getWorld(),
                            eyePos.getX(), eyePos.getY(), eyePos.getZ(),
                            angleEnd.getX(), angleEnd.getY(), angleEnd.getZ(),
                            org.bukkit.Color.fromRGB(0xFF0000), TOOL_UPDATE_TICKS, 16);
                    }
                } else {
                    Trigger target = EntityTool.getTargetEntity(player);
                    if (target instanceof Portal portal) {
                        Location portalLoc = portal.getLocation();

                        EntityTool.visualizeTargetEntity(player, portal);

                        org.bukkit.persistence.PersistentDataContainer pdc = portal.getEntity().getPersistentDataContainer();
                        byte[] targetPosData = pdc.get(new org.bukkit.NamespacedKey(QuakePlugin.INSTANCE, "target_pos"),
                            org.bukkit.persistence.PersistentDataType.BYTE_ARRAY);
                        byte[] targetDirData = pdc.get(new org.bukkit.NamespacedKey(QuakePlugin.INSTANCE, "target_dir"),
                            org.bukkit.persistence.PersistentDataType.BYTE_ARRAY);
                        
                        if (targetPosData != null && targetDirData != null) {
                            org.bukkit.util.Vector targetPos = org.bukkit.util.Vector.fromJOML(
                                (org.joml.Vector3d) org.apache.commons.lang3.SerializationUtils.deserialize(targetPosData));
                            org.bukkit.util.Vector targetDir = org.bukkit.util.Vector.fromJOML(
                                (org.joml.Vector3d) org.apache.commons.lang3.SerializationUtils.deserialize(targetDirData));
                            Location targetLoc = targetPos.toLocation(portalLoc.getWorld()).setDirection(targetDir);

                            org.bukkit.util.BoundingBox targetBox = new org.bukkit.util.BoundingBox(
                                targetLoc.getX() - 0.5, targetLoc.getY(), targetLoc.getZ() - 0.5,
                                targetLoc.getX() + 0.5, targetLoc.getY() + 2, targetLoc.getZ() + 0.5
                            );
                            EntityTool.drawBoundingBox(player.getWorld(), targetBox, org.bukkit.Color.fromRGB(0x00FF00), TOOL_UPDATE_TICKS, 16);

                            EntityTool.drawLine(player.getWorld(),
                                portalLoc.getX(), portalLoc.getY() + 1, portalLoc.getZ(),
                                targetLoc.getX(), targetLoc.getY() + 1, targetLoc.getZ(),
                                org.bukkit.Color.fromRGB(0xFFFFFF), TOOL_UPDATE_TICKS, 16);

                            float exitYaw = targetLoc.getYaw();
                            Location eyePos = targetLoc.clone().add(0, 1.62, 0);
                            double angleRadians = Math.toRadians(exitYaw);
                            Location angleEnd = eyePos.clone().add(
                                -Math.sin(angleRadians) * 1.5,
                                0,
                                Math.cos(angleRadians) * 1.5
                            );
                            EntityTool.drawLine(player.getWorld(),
                                eyePos.getX(), eyePos.getY(), eyePos.getZ(),
                                angleEnd.getX(), angleEnd.getY(), angleEnd.getZ(),
                                org.bukkit.Color.fromRGB(0xFF0000), TOOL_UPDATE_TICKS, 16);

                            player.showTitle(net.kyori.adventure.title.Title.title(
                                net.kyori.adventure.text.Component.empty(),
                                TranslationManager.t("mapper.tool.portal.subtitle.target", player,
                                    Placeholder.unparsed("location", String.format("%.1f %.1f %.1f", targetLoc.getX(), targetLoc.getY(), targetLoc.getZ())),
                                    Placeholder.unparsed("yaw", String.format("%.0f", exitYaw))),
                                    Title.Times.times(Duration.ZERO, Duration.ofMillis(500), Duration.ofMillis(200))
                            ));
                        }
                    }
                }
            }
        }.runTaskTimer(QuakePlugin.INSTANCE, 0, TOOL_UPDATE_TICKS);
    }
    
    private void handleSpawnerToolInteraction(PlayerInteractEvent event, Player player, ItemStack item) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        
        event.setCancelled(true);
        
        String toolType = SpawnerTool.getSpawnerToolType(item);
        if (toolType == null) {
            player.sendMessage(TranslationManager.t("mapper.tool.spawner.message.invalid", player)
                .color(net.kyori.adventure.text.format.NamedTextColor.RED));
            return;
        }

        Location clickedBlock = event.getClickedBlock().getLocation();

        Location spawnLoc = clickedBlock.clone().add(0.5, 2, 0.5);

        SpawnerTool.placeSpawner(toolType, spawnLoc);

        String spawnerName = item.getItemMeta().displayName() != null
            ? net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                .serialize(item.getItemMeta().displayName())
            : toolType;
        
        player.sendMessage(TranslationManager.t("mapper.tool.spawner.message.placed", player,
            Placeholder.unparsed("spawner", spawnerName),
            Placeholder.unparsed("location", String.format("%.1f %.1f %.1f", spawnLoc.getX(), spawnLoc.getY(), spawnLoc.getZ())))
            .color(net.kyori.adventure.text.format.NamedTextColor.GREEN));
    }
}