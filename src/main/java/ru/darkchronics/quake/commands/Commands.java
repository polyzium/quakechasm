/*
 * DarkChronics-Quake, a Quake minigame plugin for Minecraft servers running PaperMC
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

package ru.darkchronics.quake.commands;

import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.*;
import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.key.Key;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import joptsimple.internal.Strings;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.util.Vector;
import ru.darkchronics.quake.QuakePlugin;
import ru.darkchronics.quake.QuakeUserState;
import ru.darkchronics.quake.game.combat.WeaponType;
import ru.darkchronics.quake.game.combat.WeaponUtil;
import ru.darkchronics.quake.game.combat.powerup.PowerupType;
import ru.darkchronics.quake.game.entities.QEntityUtil;
import ru.darkchronics.quake.game.entities.Trigger;
import ru.darkchronics.quake.game.entities.pickups.*;
import ru.darkchronics.quake.game.entities.triggers.Jumppad;
import ru.darkchronics.quake.game.entities.triggers.Portal;
import ru.darkchronics.quake.matchmaking.matches.MatchMode;
import ru.darkchronics.quake.matchmaking.MatchmakingManager;
import ru.darkchronics.quake.matchmaking.matches.Match;
import ru.darkchronics.quake.matchmaking.matches.MatchManager;
import ru.darkchronics.quake.matchmaking.Team;
import ru.darkchronics.quake.matchmaking.factory.*;
import ru.darkchronics.quake.matchmaking.map.QMap;
import ru.darkchronics.quake.matchmaking.map.Spawnpoint;
import ru.darkchronics.quake.menus.Menu;
import ru.darkchronics.quake.menus.MenuGenerators;
import ru.darkchronics.quake.menus.MenuManager;
import ru.darkchronics.quake.misc.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public abstract class Commands {
   
    public static void initQuakeCommand() {
        CommandAPICommand jumppadCmd = new CommandAPICommand("jumppad")
                .withSubcommand(new CommandAPICommand("create")
                        .withArguments(new DoubleArgument("power"))
                        .executesPlayer((player, args) -> {
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
                            Entity nearestJumppad = QEntityUtil.nearestEntity(player.getLocation(), 3, entity ->
                                    entity.getType() == EntityType.MARKER &&
                                            QEntityUtil.getEntityType(entity).equals("jumppad")
                            );
                            if (nearestJumppad == null) {
                                player.sendMessage(TranslationManager.t("ERROR_ENTITY_JUMPPAD_NONEARBY", player));
                                return;
                            }

                            for (Trigger trigger : QuakePlugin.INSTANCE.triggers) {
                                if (!(trigger instanceof Jumppad jumppad)) continue;
                                if (trigger.getEntity() != nearestJumppad) continue;

                                ArrayList<Vector> trajectory = MiscUtil.calculateTrajectory(trigger.getLocation(), jumppad.getLaunchVec());
                                Location prevLoc = null;
                                for (Vector vector : trajectory) {
                                    World world = trigger.getLocation().getWorld();
                                    Location loc = vector.toLocation(world);
                                    if (prevLoc == null)
                                        world.spawnParticle(Particle.DUST, loc, 1, 0, 0, 0, 0, new Particle.DustOptions(Color.fromRGB(0x0000FF), 3));
                                    else
                                        ParticleUtil.drawRedstoneLine(prevLoc, loc, new Particle.DustOptions(Color.fromRGB(0x0000FF), 4));
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
                            double power = (double) args.get("power");
                            Location jpLoc = player.getLocation();
                            Vector launchVector = player.getEyeLocation().getDirection().multiply(power);

                            ArrayList<Vector> trajectory = MiscUtil.calculateTrajectory(jpLoc, launchVector);
                            Location prevLoc = null;
                            for (Vector vector : trajectory) {
                                World world = player.getLocation().getWorld();
                                Location loc = vector.toLocation(world);
                                if (prevLoc == null)
                                    world.spawnParticle(Particle.DUST, loc, 1, 0, 0, 0, 0, new Particle.DustOptions(Color.fromRGB(0x0000FF), 3));
                                else
                                    ParticleUtil.drawRedstoneLine(prevLoc, loc, new Particle.DustOptions(Color.fromRGB(0x0000FF), 4));
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
                            QuakeUserState state = QuakePlugin.INSTANCE.userStates.get(player);
                            if (state.portalLoc == null) {
                                state.portalLoc = player.getLocation();
                                player.sendMessage(TranslationManager.t("COMMAND_ENTITY_PORTAL_CREATE_INITIATED", player));
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
                            QuakeUserState state = QuakePlugin.INSTANCE.userStates.get(player);
                            if (state.portalLoc == null) {
                                player.sendMessage(TranslationManager.t("COMMAND_GENERIC_CANCELED_ALREADY", player));
                                return;
                            }
                            state.portalLoc = null;
                            player.sendMessage(TranslationManager.t("COMMAND_ENTITY_PORTAL_CREATE_CANCELED", player));
                        })
                );

        CommandAPICommand healthSpawnerCmd = new CommandAPICommand("healthspawner")
                .withSubcommand(new CommandAPICommand("create")
                        .withArguments(
                                new StringArgument("type")
                                        .includeSuggestions(ArgumentSuggestions.strings("small", "medium", "large", "mega"))
                        )
                        .executesPlayer((player, args) -> {
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
                                    player.sendMessage(TranslationManager.t("ERROR_HEALTHSPAWNER_WRONGTYPE", player));
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
                                    player.sendMessage(TranslationManager.t("ERROR_ARMORSPAWNER_WRONGTYPE", player));
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
                                player.sendMessage("§cWrong powerup type, use either of: "+ Strings.join(MiscUtil.getEnumNamesLowercase(PowerupType.class), ", "));
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
                                player.sendMessage(TranslationManager.t("ERROR_CTFFLAG_WRONGTEAM", player));
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
                        sender.sendMessage(TranslationManager.t("PLUGIN_RELOAD", player));
                    else
                        sender.sendMessage(TranslationManager.t("PLUGIN_RELOAD", TranslationManager.FALLBACK));

                    Bukkit.getServer().broadcast(
                            Component.text("[DarkChronics-Quake]").color(TextColor.color(0x8e60e0)).append(
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
                            player.sendMessage(TranslationManager.t("ERROR_GIVE_INVALIDOPTION", player)+giveWhat+"!");
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
                                    player.sendMessage(TranslationManager.t("ERROR_WORLDEDIT", player));
                                    return;
                                }
                                Region selection = worldEditPlugin.getSession(player).getSelection(BukkitAdapter.adapt(world));
                                BlockVector3 pos1 = selection.getBoundingBox().getPos1();
                                BlockVector3 pos2 = selection.getBoundingBox().getPos2();
                                Location bukkitPos1 = BukkitAdapter.adapt(world, pos1);
                                Location bukkitPos2 = BukkitAdapter.adapt(world, pos2);

                                bukkitSelection = BoundingBox.of(bukkitPos1, bukkitPos2);
                            } catch (IncompleteRegionException e) {
                                player.sendMessage(TranslationManager.t("ERROR_MAP_REGIONNOTSELECTED", player));
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
                                player.sendMessage(TranslationManager.t("ERROR_DUPLICATEMAP", player));
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

                            player.sendMessage(TranslationManager.t("COMMAND_MAP_CREATED_1", player)+displayName+TranslationManager.t("COMMAND_MAP_CREATED_2", player));
                        })
                )
                .withSubcommand(new CommandAPICommand("remove")
                        .withPermission("quake.admin")
                        .withArguments(new StringArgument("mapName"))
                        .executesPlayer((player, args) -> {
                            String name = (String) args.get("mapName");
                            QMap map = QuakePlugin.INSTANCE.getMap(name);
                            if (map == null) {
                                player.sendMessage(TranslationManager.t("ERROR_NOSUCHMAP", player));
                                return;
                            }
                            // Place spawnpoints
                            for (Spawnpoint spawnPoint : map.spawnPoints) {
                                QuakePlugin.placeSpawnpoint(spawnPoint);
                                map.world.spawnParticle(Particle.DUST, spawnPoint.pos, 64, 0.5, 0.5, 0.5, new Particle.DustOptions(Color.fromRGB(0xFF0000), 2));
                            }

                            // Do removal
                            QuakePlugin.INSTANCE.maps.remove(map);
                            player.sendMessage(TranslationManager.t("COMMAND_MAP_REMOVED_1", player)+name+TranslationManager.t("COMMAND_MAP_REMOVED_2", player));
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
                                player.sendMessage(TranslationManager.t("ERROR_WORLDEDIT", player));
                                return;
                            }

                            String name = (String) args.get("mapName");
                            QMap map = QuakePlugin.INSTANCE.getMap(name);
                            if (map == null) {
                                player.sendMessage(TranslationManager.t("ERROR_NOSUCHMAP", player));
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

                                player.sendMessage(TranslationManager.t("COMMAND_MAP_SELECT_RESULT", player));
//                            } catch (IncompleteRegionException e) {
//                                throw new RuntimeException(e);
//                            }
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
                                sender.sendMessage(TranslationManager.t("ERROR_NOSUCHMAP", player));
                                return;
                            }

                            for (Match match : QuakePlugin.INSTANCE.matchManager.matches) {
                                if (match.getMap().name.equals(mapName)) {
                                    sender.sendMessage("§cThere is already a match in progress on "+mapName+TranslationManager.t("ERROR_MATCH_MAPOCCUPIED_2", player));
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
                                    sender.sendMessage(TranslationManager.t("ERROR_INVALIDMODE", player)+mode);
                                else
                                    sender.sendMessage(TranslationManager.t("ERROR_INVALIDMODE", TranslationManager.FALLBACK)+mode);

                                return;
                            }

                            MatchManager matchManager = QuakePlugin.INSTANCE.matchManager;
                            Match match = matchManager.newMatch(matchFactory, map);
                            if (match == null) {
                                sender.sendMessage(TranslationManager.t("ERROR_GENERIC", player));
                                return;
                            }
                            match.setNeedPlayers(needPlayers);
                            sender.sendMessage("Made a new "+matchFactory.getName()+" match with index "+ matchManager.matches.indexOf(match));
                        })
                )
                .withSubcommand(new CommandAPICommand("join")
                        .withArguments(new IntegerArgument("index"))
                        .executesPlayer((player, args) -> {
                            MatchManager matchManager = QuakePlugin.INSTANCE.matchManager;
                            QuakeUserState userState = QuakePlugin.INSTANCE.userStates.get(player);
                            if (userState.currentMatch != null) {
                                player.sendMessage(TranslationManager.t("ERROR_MATCH_ALREADY", player));
                                return;
                            }

                            int index = (int) args.get("index");
                            if (index >= matchManager.matches.size() || index < 0) {
                                player.sendMessage(TranslationManager.t("ERROR_NOSUCHMATCH", player));
                                return;
                            }

                            matchManager.matches.get(index).join(player, null);
                        })
                )
                .withSubcommand(new CommandAPICommand("leave")
                        .executesPlayer((player, args) -> {
                            QuakeUserState userState = QuakePlugin.INSTANCE.userStates.get(player);
                            if (userState.currentMatch == null) {
                                player.sendMessage(TranslationManager.t("ERROR_MATCH_NOTINAMATCH", player));
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
                                    sender.sendMessage(TranslationManager.t("ERROR_NOMATCHES", player));
                                else
                                    sender.sendMessage(TranslationManager.t("ERROR_NOMATCHES", TranslationManager.FALLBACK));

                                return;
                            }

                            for (int i = 0; i < matchManager.matches.size(); i++) {
                                Match match = matchManager.matches.get(i);

                                sender.sendMessage(String.format("%d: %s on %s, %d players", i, match.getName(), match.getMap().name, match.getPlayers().size()));
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
                                player.sendMessage(TranslationManager.t("ERROR_PARTY_COMMAND_LEADERONLY", player));
                                return;
                            }

                            if (MatchmakingManager.INSTANCE.findPendingParty(player) != null) {
                                player.sendMessage(TranslationManager.t("ERROR_MATCHMAKING_ALREADYSEARCHING", player));
                                return;
                            }

                            if (userState.currentMatch != null) {
                                player.sendMessage(TranslationManager.t("ERROR_MATCHMAKING_SEARCH_INAMATCH", player));
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
                                player.sendMessage(TranslationManager.t("ERROR_MATCHMAKING_ONLYONEMAP", player));
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
                                player.sendMessage(TranslationManager.t("ERROR_PARTY_COMMAND_LEADERONLY", player));
                                return;
                            }

                            if (userState.mmState.currentPendingMatch != null) {
                                player.sendMessage(TranslationManager.t("ERROR_MATCHMAKING_NOTACCEPTED", player));
                                return;
                            } else if (userState.mmState.acceptedPendingMatch != null) {
                                player.sendMessage(TranslationManager.t("ERROR_MATCHMAKING_NOCANCEL", player));
                                return;
                            }

                            boolean wasSearching = MatchmakingManager.INSTANCE.stopSearching(player);
                            if (wasSearching)
                                player.sendMessage(TranslationManager.t("MATCHMAKING_SEARCH_CANCELED", player));
                            else
                                player.sendMessage(TranslationManager.t("COMMAND_GENERIC_CANCELLED_ALREADY", player));
                        })
                )
                .withSubcommand(new CommandAPICommand("accept")
                        .executesPlayer((player, args) -> {
                            QuakeUserState userState = QuakePlugin.INSTANCE.userStates.get(player);
                            if (userState.mmState.currentPendingMatch != null) {
                                userState.mmState.currentPendingMatch.accept(player);
                            } else {
                                player.sendMessage(TranslationManager.t("ERROR_MATCHMAKING_NOPENDINGMATCH", player));
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
                                player.sendMessage(TranslationManager.t("ERROR_PARTY_UNABLESELFINVITE", player));
                                return;
                            }

                            QuakeUserState inviteeState = QuakePlugin.INSTANCE.userStates.get(invitee);
                            QuakeUserState inviterState = QuakePlugin.INSTANCE.userStates.get(player);
                            if (inviterState.mmState.currentParty.getPlayers().contains(invitee)) {
                                player.sendMessage(TranslationManager.t("ERROR_PARTY_ALREADYTHERE", player));
                                return;
                            }
                            inviteeState.mmState.invitationParty = inviterState.mmState.currentParty;

                            invitee.sendMessage(player.getName()+TranslationManager.t("PARTY_INVITEDBY_1", player)+
                                    TranslationManager.t("PARTY_INVITEDBY_2", player));
                            inviterState.mmState.currentParty.sendMessage(Component.textOfChildren(
                                    MatchmakingManager.Party.localizedPrefix(inviterState.mmState.currentParty.leader.locale()), Component.text(player.getName()+TranslationManager.t("PARTY_INVITED", inviterState.mmState.currentParty.leader)+invitee.getName())
                            ));

                            new BukkitRunnable() {
                                @Override
                                public void run() {
                                    if (inviteeState.mmState.invitationParty == null) return;

                                    invitee.sendMessage(TranslationManager.t("ERROR_PARTY_INVITEEXPIRED", player));
                                    player.sendMessage("§c"+invitee.getName()+TranslationManager.t("ERROR_PARTY_INVITE_DIDNTACCEPT", player));

                                    inviteeState.mmState.invitationParty = null;
                                }
                            }.runTaskLater(QuakePlugin.INSTANCE, 30*20);
                        })
                )
                .withSubcommand(new CommandAPICommand("accept")
                        .executesPlayer((player, args) -> {
                            QuakeUserState inviteeState = QuakePlugin.INSTANCE.userStates.get(player);

                            if (inviteeState.mmState.invitationParty == null) {
                                player.sendMessage(TranslationManager.t("ERROR_PARTY_NOINVITE", player));
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
                            player.sendMessage("All players in your party:");
                            QuakeUserState userState = QuakePlugin.INSTANCE.userStates.get(player);
                            for (Player partyPlayer : userState.mmState.currentParty.getPlayers()) {
                                boolean isLeader = partyPlayer == userState.mmState.currentParty.leader;
                                player.sendMessage(Component.textOfChildren(
                                        partyPlayer.displayName(),
                                        isLeader?Component.text(TranslationManager.t("COMMAND_PARTY_LIST_LEADER", player)):Component.empty()
                                ));
                            }
                        })
                )
                .withSubcommand(new CommandAPICommand("kick")
                        .withArguments(new PlayerProfileArgument("player"))
                        .executesPlayer((player, args) -> {
                            QuakeUserState userState = QuakePlugin.INSTANCE.userStates.get(player);
                            if (userState.mmState.currentParty.leader != player) {
                                player.sendMessage(TranslationManager.t("ERROR_PARTY_COMMAND_LEADERONLY", player));
                                return;
                            }

                            Player annoyingPlayer = (Player) args.get("player");
                            assert annoyingPlayer != null;

                            if (player == annoyingPlayer) {
                                player.sendMessage(TranslationManager.t("ERROR_PARTY_UNABLESELFKICK", player));
                                return;
                            }

                            if (!userState.mmState.currentParty.getPlayers().contains(annoyingPlayer)) {
                                player.sendMessage("§c"+annoyingPlayer.getName()+TranslationManager.t("ERROR_PARTY_ABSENT", player));
                                return;
                            }
                            userState.mmState.currentParty.removePlayer(annoyingPlayer);

                            userState.mmState.currentParty.sendMessage(Component.textOfChildren(
                                    MatchmakingManager.Party.localizedPrefix(userState.mmState.currentParty.leader.locale()), player.displayName(), Component.text(TranslationManager.t("PARTY_LEADER_KICKED", userState.mmState.currentParty.leader)), annoyingPlayer.displayName()
                            ));
                            annoyingPlayer.sendMessage(TranslationManager.t("ERROR_PARTY_KICKED_1", annoyingPlayer)+player.getName()+TranslationManager.t("ERROR_PARTY_KICKED_2", annoyingPlayer));
                        })
                )
                .withSubcommand(new CommandAPICommand("leave")
                        .executesPlayer((player, args) -> {
                            QuakeUserState userState = QuakePlugin.INSTANCE.userStates.get(player);
                            MatchmakingManager.Party partyToLeave = userState.mmState.currentParty;
                            if (userState.mmState.currentParty.size() == 1) {
                                player.sendMessage(TranslationManager.t("ERROR_PARTY_SINGLE", player));
                                return;
                            }

                            if (userState.mmState.currentParty.leader == player)
                                player.sendMessage(TranslationManager.t("WARNING_PARTY_LEAVINGWHILELEADER", player));
                            userState.mmState.currentParty.removePlayer(player);
                            player.sendMessage(TranslationManager.t("PARTY_LEFT", player)+partyToLeave.leader.getName()+TranslationManager.t("ERROR_PARTY_KICKED_2", player));
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
                        player.sendMessage(TranslationManager.t("ERROR_CHATROOM_INVALID", player));
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

                    player.sendMessage(String.valueOf(QuakePlugin.INSTANCE.userStates.get(player).currentMatch.isTeamMatch()));
                });

        CommandAPICommand menuCmd = new CommandAPICommand("menu")
                .executesPlayer((player, args) -> {
                    MenuManager.INSTANCE.showMenu(MenuGenerators.mainMenu(player.locale()), player);
                });

        new CommandAPICommand("quake")
                .withAliases("dcquake")
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
