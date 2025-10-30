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

package com.github.polyzium.quakechasm.commands;

import com.github.polyzium.quakechasm.events.listeners.MapperToolListener;
import com.github.polyzium.quakechasm.game.mapper.PortalTool;
import com.github.polyzium.quakechasm.game.mapper.SpawnerTool;
import com.github.polyzium.quakechasm.game.combat.DamageCause;
import com.github.polyzium.quakechasm.game.entities.pickups.*;
import com.github.polyzium.quakechasm.matchmaking.factory.*;
import com.github.polyzium.quakechasm.matchmaking.matches.FFAMatch;
import com.github.polyzium.quakechasm.misc.Chatroom;
import com.github.polyzium.quakechasm.misc.MiscUtil;
import com.github.polyzium.quakechasm.misc.ParticleUtil;
import com.github.polyzium.quakechasm.misc.TranslationManager;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.*;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import joptsimple.internal.Strings;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.util.Vector;
import com.github.polyzium.quakechasm.QuakePlugin;
import com.github.polyzium.quakechasm.QuakeUserState;
import com.github.polyzium.quakechasm.game.combat.WeaponType;
import com.github.polyzium.quakechasm.game.combat.WeaponUtil;
import com.github.polyzium.quakechasm.game.combat.powerup.PowerupType;
import com.github.polyzium.quakechasm.game.entities.QEntityUtil;
import com.github.polyzium.quakechasm.game.entities.Trigger;
import com.github.polyzium.quakechasm.game.entities.triggers.Jumppad;
import com.github.polyzium.quakechasm.game.entities.triggers.Portal;
import com.github.polyzium.quakechasm.matchmaking.matches.MatchMode;
import com.github.polyzium.quakechasm.matchmaking.MatchmakingManager;
import com.github.polyzium.quakechasm.matchmaking.matches.Match;
import com.github.polyzium.quakechasm.matchmaking.matches.MatchManager;
import com.github.polyzium.quakechasm.matchmaking.Team;
import com.github.polyzium.quakechasm.matchmaking.map.QMap;
import com.github.polyzium.quakechasm.matchmaking.map.Spawnpoint;
import com.github.polyzium.quakechasm.menus.MenuGenerators;
import com.github.polyzium.quakechasm.menus.MenuManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import static com.github.polyzium.quakechasm.game.combat.WeaponUtil.spawnParticlesLine;

public abstract class Commands {
    public static Component getDeprecationMessage() {
        return MiniMessage.miniMessage().deserialize(
                "<red>This command is deprecated in favor of the <underlined><blue><click:run_command:/quake map toolkit><hover:show_text:'Click to get the toolkit'>Mapper Toolkit.</hover></click></blue></underlined>"
        );
    }

    public static void initQuakeCommand() {
        
        CommandAPICommand jumppadCmd = new CommandAPICommand("jumppad")
                .withSubcommand(new CommandAPICommand("create")
                        .withArguments(new DoubleArgument("power"))
                        .executesPlayer((player, args) -> {
                            player.sendMessage(getDeprecationMessage());
                            Location loc = player.getLocation();
                            loc.set(
                                    Math.floor(loc.x()) + 0.5,
                                    Math.floor(loc.y()),
                                    Math.floor(loc.z()) + 0.5
                            );

                            new Jumppad(
                                    loc,
                                    player.getEyeLocation().getDirection().multiply((double) args.get("power"))
                            );
                            player.sendMessage(String.format("Made a Jumppad at %.1f %.1f %.1f", loc.x(), loc.y(), loc.z()));
                        })
                )
                .withSubcommand(new CommandAPICommand("visualize")
                        .executesPlayer((player, args) -> {
                            player.sendMessage(getDeprecationMessage());
                            Entity nearestJumppad = QEntityUtil.nearestEntity(player.getLocation(), 3, entity ->
                                    entity.getType() == EntityType.MARKER &&
                                            QEntityUtil.getEntityType(entity).equals("jumppad")
                            );
                            if (nearestJumppad == null) {
                                player.sendMessage(TranslationManager.t("error.entity.jumppad.noNearby", player));
                                return;
                            }

                            for (Trigger trigger : QuakePlugin.INSTANCE.triggers) {
                                if (!(trigger instanceof Jumppad jumppad)) continue;
                                if (trigger.getEntity() != nearestJumppad) continue;

                                ArrayList<Vector> trajectory = MiscUtil.calculateTrajectory(trigger.getLocation(), jumppad.getLaunchVec());
                                Location prevLoc = null;
                                for (int trajectoryIndex = 0; trajectoryIndex < trajectory.size(); trajectoryIndex++) {
                                    Vector vector = trajectory.get(trajectoryIndex);
                                    World world = trigger.getLocation().getWorld();
                                    Location loc = vector.toLocation(world);
                                    if (prevLoc == null)
                                        world.spawnParticle(Particle.TRAIL, loc, 1, 0, 0, 0, 0, new Particle.Trail(loc, Color.fromRGB(0x0077FF), 60));
                                    else {
                                        int finalTrajectoryIndex = trajectoryIndex;
                                        spawnParticlesLine(prevLoc, loc, 8, particleLocation -> loc.getWorld().spawnParticle(Particle.TRAIL, particleLocation, 1, 0, 0, 0, 0, new Particle.Trail(particleLocation, Color.fromRGB(0x0077FF), 40+finalTrajectoryIndex), true));
                                    }
                                    prevLoc = loc;
                                }
                            }
                        })
                )
                .withSubcommand(new CommandAPICommand("try")
                        .withArguments(
                                new DoubleArgument("power")
                        )
                        .executesPlayer((player, args) -> {
                            player.sendMessage(getDeprecationMessage());
                            double power = (double) args.get("power");
                            Location jpLoc = player.getLocation();
                            Vector launchVector = player.getEyeLocation().getDirection().multiply(power);

                            ArrayList<Vector> trajectory = MiscUtil.calculateTrajectory(jpLoc, launchVector);
                            Location prevLoc = null;
                            for (int trajectoryIndex = 0; trajectoryIndex < trajectory.size(); trajectoryIndex++) {
                                Vector vector = trajectory.get(trajectoryIndex);
                                World world = player.getLocation().getWorld();
                                Location loc = vector.toLocation(world);
                                if (prevLoc == null)
                                    world.spawnParticle(Particle.TRAIL, loc, 1, 0, 0, 0, 0, new Particle.Trail(loc, Color.fromRGB(0x00FF00), 60));
                                else {
                                    int finalTrajectoryIndex = trajectoryIndex;
                                    spawnParticlesLine(prevLoc, loc, 8, particleLocation -> loc.getWorld().spawnParticle(Particle.TRAIL, particleLocation, 1, 0, 0, 0, 0, new Particle.Trail(particleLocation, Color.fromRGB(0x0077FF), 40 + finalTrajectoryIndex), true));
                                }
                                prevLoc = loc;
                            }

                            Vector landPos = trajectory.get(trajectory.size() - 1).clone();
                            player.sendMessage(MiniMessage.miniMessage().deserialize(
                                    String.format("Landing position is <click:run_command:/teleport %f %f %f><hover:show_text:'Click to teleport'><green>%.2f %.2f %.2f</green>",
                                            landPos.getX(), landPos.getY()+1, landPos.getZ(),
                                            landPos.getX(), landPos.getY(), landPos.getZ()
                                    )
                            )
                                            .appendNewline()
                                    .append(MiniMessage.miniMessage().deserialize(
                                            String.format("<click:run_command:/quake entity jumppad fromTry %d %d %d %f %f %f><hover:show_text:'Click here to create a jumppad based on the results.'><green>[Create]</green>",
                                                    jpLoc.getBlockX(), jpLoc.getBlockY(), jpLoc.getBlockZ(),
                                                    launchVector.getX(), launchVector.getY(), launchVector.getZ()
                                            )
                                    )));
                        })
                )
                .withSubcommand(new CommandAPICommand("fromTry")
                        .withArguments(
                                new LocationArgument("jpLocation"),
                                new LocationArgument("launchVector")
                        )
                        .executesPlayer((player, args) -> {
                            player.sendMessage(getDeprecationMessage());
                            Location jpLocation = (Location) args.get("jpLocation");
                            Vector launchVector = ((Location) args.get("launchVector")).toVector().clone();

                            jpLocation.set(
                                    Math.floor(jpLocation.x()) + 0.5,
                                    Math.floor(jpLocation.y()),
                                    Math.floor(jpLocation.z()) + 0.5
                            );

                            new Jumppad(
                                    jpLocation,
                                    launchVector
                            );
                            player.sendMessage(String.format("Made a Jumppad at %.1f %.1f %.1f", jpLocation.x(), jpLocation.y(), jpLocation.z()));
                        })
                );

        CommandAPICommand portalCmd = new CommandAPICommand("portal")
                .withSubcommand(new CommandAPICommand("create")
                        .executesPlayer((player, args) -> {
                            player.sendMessage(getDeprecationMessage());
                            QuakeUserState state = QuakePlugin.INSTANCE.userStates.get(player);
                            if (state.portalLoc == null) {
                                state.portalLoc = player.getLocation();
                                player.sendMessage(TranslationManager.t("command.entity.portal.createInitiated", player));
                                return;
                            }

                            Location ploc = player.getLocation();
                            new Portal(state.portalLoc, ploc);
                            player.sendMessage(String.format("Made a Portal at %.1f %.1f %.1f, linked to %.1f %.1f %.1f",
                                    state.portalLoc.x(), state.portalLoc.y(), state.portalLoc.z(),
                                    ploc.x(), ploc.y(), ploc.z()
                            ));
                            state.portalLoc = null;
                        })
                )
                .withSubcommand(new CommandAPICommand("cancel")
                        .executesPlayer((player, args) -> {
                            player.sendMessage(getDeprecationMessage());
                            QuakeUserState state = QuakePlugin.INSTANCE.userStates.get(player);
                            if (state.portalLoc == null) {
                                player.sendMessage(TranslationManager.t("command.generic.cancelledAlready", player));
                                return;
                            }
                            state.portalLoc = null;
                            player.sendMessage(TranslationManager.t("command.entity.portal.createCanceled", player));
                        })
                );

        CommandAPICommand healthSpawnerCmd = new CommandAPICommand("healthspawner")
                .withSubcommand(new CommandAPICommand("create")
                        .withArguments(
                                new StringArgument("type")
                                        .includeSuggestions(ArgumentSuggestions.strings("small", "medium", "large", "mega"))
                        )
                        .executesPlayer((player, args) -> {
                            player.sendMessage(getDeprecationMessage());
                            Location loc = player.getLocation();
                            loc.set(
                                    Math.floor(loc.x())+0.5,
                                    loc.y() + 1,
                                    Math.floor(loc.z())+0.5
                            );
                            int health = 0;
                            switch ((String) args.get("type")) {
                                case "small":
                                    health = 1;
                                    break;
                                case "medium":
                                    health = 5;
                                    break;
                                case "large":
                                    health = 10;
                                    break;
                                case "mega":
                                    health = 20;
                                    break;
                                default:
                                    player.sendMessage(TranslationManager.t("error.healthSpawner.wrongType", player));
                                    return;
                            }
                            new HealthSpawner(
                                    health,
                                    player.getWorld(),
                                    loc
                            );
                            player.sendMessage(String.format("Made a HealthSpawner at %.1f %.1f %.1f", loc.x(), loc.y(), loc.z()));
                        })
                );

        CommandAPICommand ammoSpawnerCmd = new CommandAPICommand("ammospawner")
                .withSubcommand(new CommandAPICommand("create")
                        .withArguments(
                                new StringArgument("ammo_type")
                                        .includeSuggestions(ArgumentSuggestions.strings(AmmoSpawner.ALIASES))
                        )
                        .executesPlayer((player, args) -> {
                            player.sendMessage(getDeprecationMessage());
                            Location loc = player.getLocation();
                            loc.set(
                                    Math.floor(loc.x())+0.5,
                                    loc.y() + 1,
                                    Math.floor(loc.z())+0.5
                            );
                            int ammoType = Arrays.asList(AmmoSpawner.ALIASES).indexOf(args.get("ammo_type"));
                            if (ammoType >= WeaponUtil.WEAPONS_NUM) {
                                player.sendMessage("§cWrong type, use either of: "+String.join(",", AmmoSpawner.ALIASES));
                                return;
                            }

                            new AmmoSpawner(
                                    ammoType,
                                    player.getWorld(),
                                    loc
                            );
                            player.sendMessage(String.format("Made an AmmoSpawner at %.1f %.1f %.1f", loc.x(), loc.y(), loc.z()));
                        })
                );

        CommandAPICommand armorSpawnerCmd = new CommandAPICommand("armorspawner")
                .withSubcommand(new CommandAPICommand("create")
                        .withArguments(
                                new StringArgument("type")
                                        .includeSuggestions(ArgumentSuggestions.strings("shard", "light", "heavy"))
                        )
                        .executesPlayer((player, args) -> {
                            player.sendMessage(getDeprecationMessage());
                            Location loc = player.getLocation();
                            loc.set(
                                    Math.floor(loc.x())+0.5,
                                    loc.y() + 1,
                                    Math.floor(loc.z())+0.5
                            );
                            int armor = 0;
                            switch ((String) args.get("type")) {
                                case "shard":
                                    armor = 5;
                                    break;
                                case "light":
                                    armor = 50;
                                    break;
                                case "heavy":
                                    armor = 100;
                                    break;
                                default:
                                    player.sendMessage(TranslationManager.t("error.armorSpawner.wrongType", player));
                                    return;
                            }
                            new ArmorSpawner(
                                    armor,
                                    player.getWorld(),
                                    loc
                            );
                            player.sendMessage(String.format("Made an ArmorSpawner at %.1f %.1f %.1f", loc.x(), loc.y(), loc.z()));
                        })
                );

        CommandAPICommand powerupSpawnerCmd = new CommandAPICommand("powerupspawner")
                .withSubcommand(new CommandAPICommand("create")
                        .withArguments(
                                new StringArgument("type")
                                        .includeSuggestions(ArgumentSuggestions.strings(
                                                MiscUtil.getEnumNamesLowercase(PowerupType.class)
                                        ))
                        )
                        .executesPlayer((player, args) -> {
                            player.sendMessage(getDeprecationMessage());
                            Location loc = player.getLocation();
                            loc.set(
                                    Math.floor(loc.x())+0.5,
                                    loc.y() + 1,
                                    Math.floor(loc.z())+0.5
                            );

                            PowerupType type;
                            try {
                                type = PowerupType.valueOf(((String) args.get("type")).toUpperCase());
                            } catch (IllegalArgumentException e) {
                                player.sendMessage(TranslationManager.t("error.powerupSpawner.wrongType", player,
                                    Placeholder.unparsed("types", Strings.join(MiscUtil.getEnumNamesLowercase(PowerupType.class), ", "))));
                                return;
                            }

                            new PowerupSpawner(type, player.getWorld(), loc, false, 30);
                            player.sendMessage(String.format("Made a PowerupSpawner at %.1f %.1f %.1f", loc.x(), loc.y(), loc.z()));
                        })
                );

        CommandAPICommand ctfFlagCmd = new CommandAPICommand("ctfflag")
                .withSubcommand(new CommandAPICommand("create")
                        .withArguments(
                                new StringArgument("team")
                                        .includeSuggestions(ArgumentSuggestions.strings("red", "blue"))
                        )
                        .executesPlayer((player, args) -> {
                            player.sendMessage(getDeprecationMessage());
                            Location loc = player.getLocation();
                            loc.set(
                                    Math.floor(loc.x())+0.5,
                                    loc.y() + 1,
                                    Math.floor(loc.z())+0.5
                            );

                            Team team;
                            try {
                                team = Team.valueOf(((String) args.get("team")).toUpperCase());
                            } catch (IllegalArgumentException e) {
                                player.sendMessage(TranslationManager.t("error.ctfFlag.wrongTeam", player));
                                return;
                            }

                            new CTFFlag(team, false, null, loc);
                            player.sendMessage(String.format("Made a CTFFlag (%s team) at %.1f %.1f %.1f", team.name(), loc.x(), loc.y(), loc.z()));
                        })
                );

        CommandAPICommand weaponSpawnerCmd = new CommandAPICommand("weaponspawner")
                .withSubcommand(new CommandAPICommand("create")
                        .withArguments(new StringArgument("weapon").includeSuggestions(ArgumentSuggestions.strings(
                                "machinegun",
                                "shotgun",
                                "rocket_launcher",
                                "lightning_gun",
                                "railgun",
                                "plasma_gun",
                                "bfg"
                        )))
                        .executesPlayer((player, args) -> {
                            player.sendMessage(getDeprecationMessage());
                            int weaponIndex = switch ((String) args.get("weapon")) {
                                case "machinegun" -> WeaponType.MACHINEGUN;
                                case "shotgun" -> WeaponType.SHOTGUN;
                                case "rocket_launcher" -> WeaponType.ROCKET_LAUNCHER;
                                case "lightning_gun" -> WeaponType.LIGHTNING_GUN;
                                case "railgun" -> WeaponType.RAILGUN;
                                case "plasma_gun" -> WeaponType.PLASMA_GUN;
                                case "bfg" -> WeaponType.BFG;
                                default -> throw new IllegalStateException("Weapon out of range");
                            };

                            Location loc = player.getLocation();
                            loc.set(
                                    Math.floor(loc.x()) + 0.5,
                                    Math.floor(loc.y()) + 1,
                                    Math.floor(loc.z()) + 0.5
                            );
                            new WeaponSpawner(
                                    weaponIndex,
                                    player.getWorld(),
                                    loc
                            );
                            player.sendMessage(String.format("Made a WeaponSpawner at %.1f %.1f %.1f", loc.x(), loc.y(), loc.z()));
                        }));

        CommandAPICommand reloadCmd = new CommandAPICommand("reload")
                .withPermission("quake.admin")
                .executes((sender, args) -> {
                    Player player = null;
                    if (sender instanceof Player)
                        player = (Player) sender;

                    QuakePlugin.INSTANCE.onDisable();
                    QuakePlugin.INSTANCE.onEnable();
                    if (player != null)
                        sender.sendMessage(TranslationManager.t("plugin.reload", player));
                    else
                        sender.sendMessage(TranslationManager.t("plugin.reload", TranslationManager.FALLBACK));

                    Bukkit.getServer().broadcast(
                            Component.text("[Quakechasm]").color(TextColor.color(0x8e60e0)).append(
                                    Component.text(" Plugin reloaded").color(TextColor.color(0xffffff)))
                    );
                });

        CommandAPICommand giveCmd = new CommandAPICommand("give")
                .withPermission("quake.admin")
                .withArguments(new StringArgument("what").includeSuggestions(ArgumentSuggestions.strings("ammo", "quad", "protection", "regeneration")))
                .executesPlayer((player, args) -> {
                    String giveWhat = (String) args.get("what");
                    assert giveWhat != null;
                    switch (giveWhat) {
                        case "ammo":
                            int[] ammo = QuakePlugin.INSTANCE.userStates.get(player).weaponState.ammo;
                            for (int i = 0; i < WeaponUtil.WEAPONS_NUM; i++) ammo[i] = 999;
                            break;
                        case "quad":
                            PowerupSpawner.doPowerup(player, PowerupType.QUAD_DAMAGE, 30);
                            break;
                        case "protection":
                            PowerupSpawner.doPowerup(player, PowerupType.PROTECTION, 30);
                            break;
                        case "regeneration":
                            PowerupSpawner.doPowerup(player, PowerupType.REGENERATION, 30);
                            break;
                        default:
                            player.sendMessage(TranslationManager.t("error.give.invalidOption", player,
                                Placeholder.unparsed("option", giveWhat)));
                            break;
                    }
                });

        ListArgument<MatchMode> recommendedModesArg = new ListArgumentBuilder<MatchMode>("recommendedModes")
                .withList(List.of(MatchMode.values()))
                .withMapper(mode -> mode.name().toLowerCase())
                .buildGreedy();
        CommandAPICommand mapCmd = new CommandAPICommand("map")
                .withPermission("quake.builder")
                .withSubcommand(new CommandAPICommand("create")
                        .withArguments(new StringArgument("name"), new TextArgument("displayName"), new IntegerArgument("neededPlayers"), recommendedModesArg)
                        .executesPlayer((player, args) -> {
                            World world = player.getWorld();
                            BoundingBox bukkitSelection;
                            try {
                                WorldEditPlugin worldEditPlugin = (WorldEditPlugin) Bukkit.getPluginManager().getPlugin("WorldEdit");
                                if (worldEditPlugin == null) {
                                    player.sendMessage(TranslationManager.t("error.worldEdit", player));
                                    return;
                                }
                                Region selection = worldEditPlugin.getSession(player).getSelection(BukkitAdapter.adapt(world));
                                BlockVector3 pos1 = selection.getBoundingBox().getPos1();
                                BlockVector3 pos2 = selection.getBoundingBox().getPos2();
                                Location bukkitPos1 = BukkitAdapter.adapt(world, pos1);
                                Location bukkitPos2 = BukkitAdapter.adapt(world, pos2);

                                bukkitSelection = BoundingBox.of(bukkitPos1, bukkitPos2);
                            } catch (IncompleteRegionException e) {
                                player.sendMessage(TranslationManager.t("error.map.regionNotSelected", player));
                                return;
                            }

                            ArrayList<Spawnpoint> spawnPoints = QuakePlugin.scanSpawnpointsIn(player.getWorld(), bukkitSelection);
                            List<MatchMode> recommendedModes = (List<MatchMode>) args.get("recommendedModes");

                            String name = (String) args.get("name");
                            String displayName = (String) args.get("displayName");
                            Integer neededPlayers = (Integer) args.get("neededPlayers");
                            assert recommendedModes != null;
                            assert neededPlayers != null;
                            QMap qMap = new QMap(name, displayName, world, bukkitSelection, new ArrayList<>(spawnPoints), new ArrayList<>(recommendedModes), neededPlayers);
                            if (QuakePlugin.INSTANCE.maps == null)
                                QuakePlugin.INSTANCE.maps = new ArrayList<>(8);

                            if (QuakePlugin.INSTANCE.getMap(name) != null) {
                                player.sendMessage(TranslationManager.t("error.duplicateMap", player));
                                return;
                            }

                            for (Entity entity : player.getWorld().getNearbyEntities(qMap.bounds)) {
                                for (Spawnpoint spawnPoint : spawnPoints) {
                                    if (entity.getType() == EntityType.ARMOR_STAND && entity.getLocation().equals(spawnPoint.pos))
                                        entity.remove();
                                }
                            }

                            QuakePlugin.INSTANCE.maps.add(qMap);

                            for (Spawnpoint spawnPoint : spawnPoints) {
                                world.spawnParticle(Particle.INSTANT_EFFECT, spawnPoint.pos, 64, 0.5, 0.5, 0.5);
                                world.setBlockData(spawnPoint.pos, Material.AIR.createBlockData());
                            }

                            player.sendMessage(TranslationManager.t("command.map.created", player,
                                Placeholder.unparsed("map_name", displayName)));
                        })
                )
                .withSubcommand(new CommandAPICommand("remove")
                        .withPermission("quake.admin")
                        .withArguments(new StringArgument("mapName"))
                        .executesPlayer((player, args) -> {
                            String name = (String) args.get("mapName");
                            QMap map = QuakePlugin.INSTANCE.getMap(name);
                            if (map == null) {
                                player.sendMessage(TranslationManager.t("error.noSuchMap", player));
                                return;
                            }
                            // Place spawnpoints
                            for (Spawnpoint spawnPoint : map.spawnPoints) {
                                QuakePlugin.placeSpawnpoint(spawnPoint);
                                map.world.spawnParticle(Particle.DUST, spawnPoint.pos, 64, 0.5, 0.5, 0.5, new Particle.DustOptions(Color.fromRGB(0xFF0000), 2));
                            }

                            // Do removal
                            QuakePlugin.INSTANCE.maps.remove(map);
                            player.sendMessage(TranslationManager.t("command.map.removed", player,
                                Placeholder.unparsed("map_name", name)));
                        })
                )
                .withSubcommand(new CommandAPICommand("list")
                        .executes((sender, args) -> {
                            if (QuakePlugin.INSTANCE.maps.isEmpty()) {
                                sender.sendMessage("§cNo maps to list");
                                return;
                            }
                            for (QMap map : QuakePlugin.INSTANCE.maps) {
                                sender.sendMessage(map.name+" in "+map.world.getName()+" at "+ map.bounds.getMin());
                            }
                        })
                )
                .withSubcommand(new CommandAPICommand("select")
                        .withArguments(new StringArgument("mapName"))
                        .executesPlayer((player, args) -> {
                            WorldEditPlugin worldEditPlugin = (WorldEditPlugin) Bukkit.getPluginManager().getPlugin("WorldEdit");
                            if (worldEditPlugin == null) {
                                player.sendMessage(TranslationManager.t("error.worldEdit", player));
                                return;
                            }

                            String name = (String) args.get("mapName");
                            QMap map = QuakePlugin.INSTANCE.getMap(name);
                            if (map == null) {
                                player.sendMessage(TranslationManager.t("error.noSuchMap", player));
                                return;
                            }
//                            try {
                                // This does not work
//                                Region selection = worldEditPlugin.getSession(player).getSelection();
//                                CuboidRegion cuboidRegion = new CuboidRegion(
//                                        BukkitAdapter.adapt(player.getWorld()),
//                                        BukkitAdapter.adapt(map.bounds.getMin().clone().toLocation(player.getWorld())).toVector().toBlockPoint(),
//                                        BukkitAdapter.adapt(map.bounds.getMax().clone().toLocation(player.getWorld())).toVector().toBlockPoint()
//                                );

//                                selection.getMinimumPoint().withX(map.bounds.getMin().getBlockX());
//                                selection.getMinimumPoint().withY(map.bounds.getMin().getBlockY());
//                                selection.getMinimumPoint().withZ(map.bounds.getMin().getBlockZ());
//
//                                selection.getMaximumPoint().withX(map.bounds.getMax().getBlockX());
//                                selection.getMaximumPoint().withY(map.bounds.getMax().getBlockY());
//                                selection.getMaximumPoint().withZ(map.bounds.getMax().getBlockZ());

                                // ...so we will execute WorldEdit commands instead
                                player.performCommand(String.format(
                                        "/pos1 %s,%s,%s",
                                        map.bounds.getMin().getBlockX(),
                                        map.bounds.getMin().getBlockY(),
                                        map.bounds.getMin().getBlockZ()
                                ));

                                player.performCommand(String.format(
                                        "/pos2 %s,%s,%s",
                                        map.bounds.getMax().getBlockX(),
                                        map.bounds.getMax().getBlockY(),
                                        map.bounds.getMax().getBlockZ()
                                ));

                                player.sendMessage(TranslationManager.t("command.map.selectResult", player));
//                            } catch (IncompleteRegionException e) {
//                                throw new RuntimeException(e);
//                            }
                       })
               )
               .withSubcommand(new CommandAPICommand("toolkit")
                       .executesPlayer((player, args) -> {
                           // Core tools
                           player.getInventory().addItem(MapperToolListener.createEntityTool());
                           player.getInventory().addItem(MapperToolListener.createJumppadTool());
                           player.getInventory().addItem(PortalTool.createPortalTool());
                           
                           // Spawnpoint tools
                           player.getInventory().addItem(MapperToolListener.createSpawnpointTool(Team.RED));
                           player.getInventory().addItem(MapperToolListener.createSpawnpointTool(Team.BLUE));
                           player.getInventory().addItem(MapperToolListener.createSpawnpointTool(Team.FREE));
                           player.getInventory().addItem(MapperToolListener.createSpawnpointTool(Team.SPECTATOR));
                           
                           // CTF Flag tools
                           player.getInventory().addItem(SpawnerTool.createRedFlagTool());
                           player.getInventory().addItem(SpawnerTool.createBlueFlagTool());
                           
                           // Weapon Spawner tools (7 weapons)
                           for (int i = 0; i < 7; i++) {
                               player.getInventory().addItem(SpawnerTool.createWeaponSpawnerTool(i));
                           }
                           
                           // Ammo Spawner tools (7 ammo types)
                           for (int i = 0; i < 7; i++) {
                               player.getInventory().addItem(SpawnerTool.createAmmoSpawnerTool(i));
                           }
                           
                           // Health Spawner tools (4 types)
                           player.getInventory().addItem(SpawnerTool.createHealthSpawnerTool(1));  // Small
                           player.getInventory().addItem(SpawnerTool.createHealthSpawnerTool(5));  // Medium
                           player.getInventory().addItem(SpawnerTool.createHealthSpawnerTool(10)); // Large
                           player.getInventory().addItem(SpawnerTool.createHealthSpawnerTool(20)); // Mega
                           
                           // Armor Spawner tools (3 types)
                           player.getInventory().addItem(SpawnerTool.createArmorSpawnerTool(5));   // Shard
                           player.getInventory().addItem(SpawnerTool.createArmorSpawnerTool(50));  // Light
                           player.getInventory().addItem(SpawnerTool.createArmorSpawnerTool(100)); // Heavy
                           
                           // Powerup Spawner tools (3 types)
                           player.getInventory().addItem(SpawnerTool.createPowerupSpawnerTool(PowerupType.QUAD_DAMAGE));
                           player.getInventory().addItem(SpawnerTool.createPowerupSpawnerTool(PowerupType.REGENERATION));
                           player.getInventory().addItem(SpawnerTool.createPowerupSpawnerTool(PowerupType.PROTECTION));
                           
                           player.sendMessage(net.kyori.adventure.text.Component.text("Mapper toolkit added to inventory")
                               .color(net.kyori.adventure.text.format.NamedTextColor.GREEN));
                       })
               );


        StringArgument matchModeArg = (StringArgument) new StringArgument("mode")
                .includeSuggestions(ArgumentSuggestions.strings("debug", "ffa", "tdm", "ctf"));
        IntegerArgument needPlayersArg = new IntegerArgument("needPlayers");
        CommandAPICommand matchCmd = new CommandAPICommand("match")
                .withPermission("quake.admin")
                .withSubcommand(new CommandAPICommand("create")
                        .withArguments(matchModeArg, needPlayersArg, new StringArgument("mapName"))
                        .executes((sender, args) -> {
                            Player player = null;
                            if (sender instanceof Player)
                                player = (Player) sender;

                            String mode = (String) args.get("mode");
                            String mapName = (String) args.get("mapName");
                            Integer needPlayers = (Integer) args.get("needPlayers");

                            QMap map = QuakePlugin.INSTANCE.getMap(mapName);
                            if (map == null) {
                                sender.sendMessage(TranslationManager.t("error.noSuchMap", player));
                                return;
                            }

                            for (Match match : QuakePlugin.INSTANCE.matchManager.matches) {
                                if (match.getMap().name.equals(mapName)) {
                                    sender.sendMessage(TranslationManager.t("error.match.mapOccupied", player,
                                        Placeholder.unparsed("map_name", mapName)));
                                    return;
                                }
                            }

                            MatchFactory matchFactory = switch (mode) {
                                case "debug" -> new DebugMatchFactory();
                                case "ffa" -> new FFAMatchFactory();
                                case "tdm" -> new TDMMatchFactory();
                                case "ctf" -> new CTFMatchFactory();
                                default -> null;
                            };
                            if (matchFactory == null) {
                                if (player != null)
                                    sender.sendMessage(TranslationManager.t("error.invalidMode", player,
                                        Placeholder.unparsed("mode", mode)));
                                else
                                    sender.sendMessage(TranslationManager.t("error.invalidMode", TranslationManager.FALLBACK,
                                        Placeholder.unparsed("mode", mode)));

                                return;
                            }

                            MatchManager matchManager = QuakePlugin.INSTANCE.matchManager;
                            Match match = matchManager.newMatch(matchFactory, map);
                            if (match == null) {
                                sender.sendMessage(TranslationManager.t("error.generic", player));
                                return;
                            }
                            match.setNeedPlayers(needPlayers);
                            Locale locale;
                            if (player == null)
                                locale = TranslationManager.FALLBACK;
                            else
                                locale =  player.locale();

                            sender.sendMessage("Made a new "+TranslationManager.tLegacy(matchFactory.getNameKey(), locale)+" match with index "+ matchManager.matches.indexOf(match));
                        })
                )
                .withSubcommand(new CommandAPICommand("join")
                        .withArguments(new IntegerArgument("index"))
                        .executesPlayer((player, args) -> {
                            MatchManager matchManager = QuakePlugin.INSTANCE.matchManager;
                            QuakeUserState userState = QuakePlugin.INSTANCE.userStates.get(player);
                            if (userState.currentMatch != null) {
                                player.sendMessage(TranslationManager.t("error.match.already", player));
                                return;
                            }

                            int index = (int) args.get("index");
                            if (index >= matchManager.matches.size() || index < 0) {
                                player.sendMessage(TranslationManager.t("error.noSuchMatch", player));
                                return;
                            }

                            matchManager.matches.get(index).join(player, null);
                        })
                )
                .withSubcommand(new CommandAPICommand("leave")
                        .executesPlayer((player, args) -> {
                            QuakeUserState userState = QuakePlugin.INSTANCE.userStates.get(player);
                            if (userState.currentMatch == null) {
                                player.sendMessage(TranslationManager.t("error.match.notInMatch", player));
                                return;
                            }
                            userState.currentMatch.leave(player);
                        })
                )
                .withSubcommand(new CommandAPICommand("list")
                        .executes((sender, args) -> {
                            MatchManager matchManager = QuakePlugin.INSTANCE.matchManager;
                            if (matchManager.matches.isEmpty()) {
                                if (sender instanceof Player player)
                                    sender.sendMessage(TranslationManager.t("error.noMatches", player));
                                else
                                    sender.sendMessage(TranslationManager.t("error.noMatches", TranslationManager.FALLBACK));

                                return;
                            }

                            for (int i = 0; i < matchManager.matches.size(); i++) {
                                Match match = matchManager.matches.get(i);

                                sender.sendMessage(String.format("%d: %s on %s, %d players", i, match.getNameKey(), match.getMap().name, match.getPlayers().size()));
                            }

                        })
                );

        StringArgument matchmakingModeArg = (StringArgument) new StringArgument("mode")
                .includeSuggestions(ArgumentSuggestions.strings("ffa", "tdm", "ctf"));
        ListArgument<QMap> matchmakingMapsArg = new ListArgumentBuilder<QMap>("maps")
                .withList(new ArrayList<>(QuakePlugin.INSTANCE.maps))
                .withMapper(qMap -> qMap.name)
                .buildGreedy();
        CommandAPICommand matchmakingCmd = new CommandAPICommand("matchmaking")
                .withSubcommand(new CommandAPICommand("search")
                        .withArguments(matchmakingModeArg, matchmakingMapsArg)
                        .executesPlayer((player, args) -> {
                            QuakeUserState userState = QuakePlugin.INSTANCE.userStates.get(player);
                            if (userState.mmState.currentParty.leader != player) {
                                player.sendMessage(TranslationManager.t("error.party.command.leaderOnly", player));
                                return;
                            }

                            if (MatchmakingManager.INSTANCE.findPendingParty(player) != null) {
                                player.sendMessage(TranslationManager.t("error.matchmaking.alreadySearching", player));
                                return;
                            }

                            if (userState.currentMatch != null) {
                                player.sendMessage(TranslationManager.t("error.matchmaking.search.inMatch", player));
                                return;
                            }

                            String modeString = (String) args.get("mode");
                            assert modeString != null;
                            MatchMode mode = switch (modeString) {
                                case "ffa" -> MatchMode.FFA;
                                case "tdm" -> MatchMode.TDM;
                                case "ctf" -> MatchMode.CTF;
                                default -> null;
                            };
                            if (mode == null) {
                                player.sendMessage("Invalid match mode "+modeString);
                                return;
                            }

                            List<QMap> maps = (List<QMap>) args.get("maps");
                            assert maps != null;
                            if (maps.size() > 1) {
                                player.sendMessage(TranslationManager.t("error.matchmaking.onlyOneMap", player));
                                return;
                            }
                            List<String> stringedMaps = maps.stream().map(qMap -> qMap.name).collect(Collectors.toList());
                            MatchmakingManager.INSTANCE.startSearching(stringedMaps, mode, userState.mmState.currentParty);
                        })
                )
                .withSubcommand(new CommandAPICommand("cancel")
                        .executesPlayer((player, args) -> {
                            QuakeUserState userState = QuakePlugin.INSTANCE.userStates.get(player);
                            if (userState.mmState.currentParty.leader != player) {
                                player.sendMessage(TranslationManager.t("error.party.command.leaderOnly", player));
                                return;
                            }

                            if (userState.mmState.currentPendingMatch != null) {
                                player.sendMessage(TranslationManager.t("error.matchmaking.notAccepted", player));
                                return;
                            } else if (userState.mmState.acceptedPendingMatch != null) {
                                player.sendMessage(TranslationManager.t("error.matchmaking.noCancel", player));
                                return;
                            }

                            boolean wasSearching = MatchmakingManager.INSTANCE.stopSearching(player);
                            if (wasSearching)
                                player.sendMessage(TranslationManager.t("matchmaking.search.canceled", player));
                            else
                                player.sendMessage(TranslationManager.t("command.generic.cancelledAlready", player));
                        })
                )
                .withSubcommand(new CommandAPICommand("accept")
                        .executesPlayer((player, args) -> {
                            QuakeUserState userState = QuakePlugin.INSTANCE.userStates.get(player);
                            if (userState.mmState.currentPendingMatch != null) {
                                userState.mmState.currentPendingMatch.accept(player);
                            } else {
                                player.sendMessage(TranslationManager.t("error.matchmaking.noPendingMatch", player));
                                return;
                            }
                        })
                );

        CommandAPICommand partyCmd = new CommandAPICommand("party")
                .withSubcommand(new CommandAPICommand("invite")
                        .withArguments(new PlayerProfileArgument("player"))
                        .executesPlayer((player, args) -> {
                            Player invitee = (Player) args.get("player");
                            assert invitee != null;

                            if (player == invitee) {
                                player.sendMessage(TranslationManager.t("error.party.unableSelfInvite", player));
                                return;
                            }

                            QuakeUserState inviteeState = QuakePlugin.INSTANCE.userStates.get(invitee);
                            QuakeUserState inviterState = QuakePlugin.INSTANCE.userStates.get(player);
                            if (inviterState.mmState.currentParty.getPlayers().contains(invitee)) {
                                player.sendMessage(TranslationManager.t("error.party.alreadyThere", player));
                                return;
                            }
                            inviteeState.mmState.invitationParty = inviterState.mmState.currentParty;

                            invitee.sendMessage(TranslationManager.t("party.invitedBy", player,
                                Placeholder.unparsed("inviter_name", player.getName())));
                            inviterState.mmState.currentParty.sendMessage(Component.textOfChildren(
                                    MatchmakingManager.Party.localizedPrefix(inviterState.mmState.currentParty.leader.locale()),
                                    TranslationManager.t("party.invited", inviterState.mmState.currentParty.leader,
                                        Placeholder.unparsed("inviter_name", player.getName()),
                                        Placeholder.unparsed("invitee_name", invitee.getName()))
                            ));

                            new BukkitRunnable() {
                                @Override
                                public void run() {
                                    if (inviteeState.mmState.invitationParty == null) return;

                                    invitee.sendMessage(TranslationManager.t("error.party.inviteExpired", player));
                                    player.sendMessage(TranslationManager.t("error.party.invite.didntAccept", player,
                                        Placeholder.unparsed("player_name", invitee.getName())));

                                    inviteeState.mmState.invitationParty = null;
                                }
                            }.runTaskLater(QuakePlugin.INSTANCE, 30*20);
                        })
                )
                .withSubcommand(new CommandAPICommand("accept")
                        .executesPlayer((player, args) -> {
                            QuakeUserState inviteeState = QuakePlugin.INSTANCE.userStates.get(player);

                            if (inviteeState.mmState.invitationParty == null) {
                                player.sendMessage(TranslationManager.t("error.party.noInvite", player));
                                return;
                            }

                            inviteeState.mmState.currentParty.removePlayer(player);
                            inviteeState.mmState.invitationParty.addPlayer(player);
                            inviteeState.mmState.currentParty = inviteeState.mmState.invitationParty;
                            inviteeState.mmState.invitationParty = null;
                        })
                )
                .withSubcommand(new CommandAPICommand("list")
                        .executesPlayer((player, args) -> {
                            player.sendMessage(TranslationManager.t("command.party.list.begin", player));
                            QuakeUserState userState = QuakePlugin.INSTANCE.userStates.get(player);
                            for (Player partyPlayer : userState.mmState.currentParty.getPlayers()) {
                                boolean isLeader = partyPlayer == userState.mmState.currentParty.leader;
                                player.sendMessage(Component.textOfChildren(
                                        partyPlayer.displayName(),
                                        isLeader?TranslationManager.t("command.party.list.leader", player):Component.empty()
                                ));
                            }
                        })
                )
                .withSubcommand(new CommandAPICommand("kick")
                        .withArguments(new PlayerProfileArgument("player"))
                        .executesPlayer((player, args) -> {
                            QuakeUserState userState = QuakePlugin.INSTANCE.userStates.get(player);
                            if (userState.mmState.currentParty.leader != player) {
                                player.sendMessage(TranslationManager.t("error.party.command.leaderOnly", player));
                                return;
                            }

                            Player annoyingPlayer = (Player) args.get("player");
                            assert annoyingPlayer != null;

                            if (player == annoyingPlayer) {
                                player.sendMessage(TranslationManager.t("error.party.unableSelfKick", player));
                                return;
                            }

                            if (!userState.mmState.currentParty.getPlayers().contains(annoyingPlayer)) {
                                player.sendMessage(TranslationManager.t("error.party.absent", player,
                                    Placeholder.unparsed("player_name", annoyingPlayer.getName())));
                                return;
                            }
                            userState.mmState.currentParty.removePlayer(annoyingPlayer);

                            userState.mmState.currentParty.sendMessage(Component.textOfChildren(
                                    MatchmakingManager.Party.localizedPrefix(userState.mmState.currentParty.leader.locale()),
                                    TranslationManager.t("party.leader.kicked", userState.mmState.currentParty.leader,
                                        Placeholder.component("leader_name", player.displayName()),
                                        Placeholder.component("player_name", annoyingPlayer.displayName()))
                            ));
                            annoyingPlayer.sendMessage(TranslationManager.t("error.party.kicked", annoyingPlayer,
                                Placeholder.unparsed("leader_name", player.getName())));
                        })
                )
                .withSubcommand(new CommandAPICommand("leave")
                        .executesPlayer((player, args) -> {
                            QuakeUserState userState = QuakePlugin.INSTANCE.userStates.get(player);
                            MatchmakingManager.Party partyToLeave = userState.mmState.currentParty;
                            if (userState.mmState.currentParty.size() == 1) {
                                player.sendMessage(TranslationManager.t("error.party.single", player));
                                return;
                            }

                            if (userState.mmState.currentParty.leader == player)
                                player.sendMessage(TranslationManager.t("warning.party.leavingWhileLeader", player));
                            userState.mmState.currentParty.removePlayer(player);
                            player.sendMessage(TranslationManager.t("party.left", player,
                                Placeholder.unparsed("leader_name", partyToLeave.leader.getName())));
                        })
                );

        CommandAPICommand entityCmd = new CommandAPICommand("entity")
                .withPermission("quake.builder")
                .withSubcommands(
                        weaponSpawnerCmd,
                        healthSpawnerCmd,
                        ammoSpawnerCmd,
                        armorSpawnerCmd,
                        powerupSpawnerCmd,
                        ctfFlagCmd,
                        jumppadCmd,
                        portalCmd
                );

        CommandAPICommand chatCmd = new CommandAPICommand("chat")
                .withArguments(new MultiLiteralArgument("chatroom", MiscUtil.getEnumNamesLowercase(Chatroom.class)))
                .executesPlayer((player, args) -> {
                    Chatroom chatroom;
                    try {
                        chatroom = Chatroom.valueOf(((String) args.get("chatroom")).toUpperCase());
                    } catch (IllegalArgumentException e) {
                        player.sendMessage(TranslationManager.t("error.chatroom.invalid", player));
                        player.sendMessage("§c"+e.getMessage());
                        return;
                    }

                    QuakeUserState userState = QuakePlugin.INSTANCE.userStates.get(player);
                    userState.switchChat(chatroom);
                });

        CommandAPICommand test = new CommandAPICommand("test")
                .withPermission("quake.admin")
                .executesPlayer((player, args) -> {
//                    TableBuilder table = new TableBuilder();
//
//                    table.addRow(TranslationManager.t("NAME", player), "Score");
//                    table.addRow("Polyzium7", "1");
//
//                    player.sendMessage(Component.text(table.build()).font(Key.key("mono")));
//                    for (Entity entity : player.getWorld().getNearbyEntities(player.getLocation(), 300, 300, 300)) {
//                        String entityType = QEntityUtil.getEntityType(entity);
//                        if (entityType == null || !entityType.equals("weapon_spawner")) continue;
//                        player.sendMessage(Component.textOfChildren(
//                                ((ItemDisplay) entity).getItemStack().displayName(),
//                                Component.text(": "+entity.getLocation().toVector())
//                        ));
////                        player.sendMessage(((ItemDisplay) entity).getItemStack().displayName());
//                    }

//                    QuakePlugin.INSTANCE.userStates.get(player).currentMatch.onDeath(player, player, DamageCause.UNKNOWN);

//                    for (DamageCause damageCause : DamageCause.values()) {
//                        player.sendMessage("§bSelf: "+damageCause.name());
//                        QuakePlugin.INSTANCE.userStates.get(player).currentMatch.onDeath(player, player, damageCause);
//                    }
//
//                    Entity attacker = player.getWorld().spawnEntity(player.getLocation(), EntityType.PIG);
//                    for (DamageCause damageCause : DamageCause.values()) {
//                        player.sendMessage("§bAttacker: "+damageCause.name());
//                        QuakePlugin.INSTANCE.userStates.get(player).currentMatch.onDeath(player, attacker, damageCause);
//                    }
//                    attacker.remove();

                    QuakeUserState userState = QuakePlugin.INSTANCE.userStates.get(player);
//                    for (int i = 0; i < 60; i++) {
//                        userState.currentMatch.onDeath(userState.currentMatch.getPlayers().getLast(), player, DamageCause.UNKNOWN);
//                    }
                    ((FFAMatch) userState.currentMatch).fraglimit = 200;
                });

        CommandAPICommand menuCmd = new CommandAPICommand("menu")
                .executesPlayer((player, args) -> {
                    MenuManager.INSTANCE.showMenu(MenuGenerators.mainMenu(player.locale()), player);
                });

        new CommandAPICommand("quake")
                .withAliases("quakechasm")
                .withSubcommands(
                        entityCmd,
                        reloadCmd,
                        giveCmd,
                        mapCmd,
                        matchCmd,
                        matchmakingCmd,
                        partyCmd,
                        chatCmd,
                        menuCmd,
                        test
                )
                .register();
    }
}
