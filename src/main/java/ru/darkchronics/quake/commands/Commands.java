package ru.darkchronics.quake.commands;

import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.*;
import net.kyori.adventure.key.Key;
import org.bukkit.entity.Player;
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
import ru.darkchronics.quake.misc.Chatroom;
import ru.darkchronics.quake.misc.MiscUtil;
import ru.darkchronics.quake.misc.ParticleUtil;
import ru.darkchronics.quake.misc.TableBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
                                player.sendMessage("§cNo jumppad has been found nearby");
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
                                        world.spawnParticle(Particle.REDSTONE, loc, 1, 0, 0, 0, 0, new Particle.DustOptions(Color.fromRGB(0x0000FF), 3));
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
                                    world.spawnParticle(Particle.REDSTONE, loc, 1, 0, 0, 0, 0, new Particle.DustOptions(Color.fromRGB(0x0000FF), 3));
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
                                player.sendMessage("Portal location stored. Go to the teleport target location and run the command again to create the portal.\nOtherwise, do /quake portal cancel");
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
                                player.sendMessage("§cAlready cancelled");
                                return;
                            }
                            state.portalLoc = null;
                            player.sendMessage("Portal creation cancelled");
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
                                    player.sendMessage("§cWrong health type, use either of: small, medium, large, mega");
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
                                    player.sendMessage("§cWrong armor type, use either of: shard, light, heavy");
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
                                player.sendMessage("§cWrong team, use either of: red, blue");
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
                .executes((sender, args) -> {
                    QuakePlugin.INSTANCE.onDisable();
                    QuakePlugin.INSTANCE.onEnable();
                    sender.sendMessage("Plugin reloaded");
                    Bukkit.getServer().broadcast(
                            Component.text("[DarkChronics-Quake]").color(TextColor.color(0x8e60e0)).append(
                                    Component.text(" Plugin reloaded").color(TextColor.color(0xffffff)))
                    );
                });

        CommandAPICommand giveCmd = new CommandAPICommand("give")
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
                            player.sendMessage("§cI don't know what is \""+giveWhat+ "\"!");
                            break;
                    }
                });

        ListArgument<MatchMode> recommendedModesArg = new ListArgumentBuilder<MatchMode>("recommendedModes")
                .withList(List.of(MatchMode.values()))
                .withMapper(mode -> mode.name().toLowerCase())
                .buildGreedy();
        CommandAPICommand mapCmd = new CommandAPICommand("map")
                .withSubcommand(new CommandAPICommand("create")
                        .withArguments(new StringArgument("name"), new IntegerArgument("neededPlayers"), recommendedModesArg)
                        .executesPlayer((player, args) -> {
                            World world = player.getWorld();
                            BoundingBox bukkitSelection;
                            try {
                                WorldEditPlugin worldEditPlugin = (WorldEditPlugin) Bukkit.getPluginManager().getPlugin("WorldEdit");
                                if (worldEditPlugin == null) {
                                    player.sendMessage("§cWorldEdit is either not installed or loaded");
                                    return;
                                }
                                Region selection = worldEditPlugin.getSession(player).getSelection(BukkitAdapter.adapt(world));
                                BlockVector3 pos1 = selection.getBoundingBox().getPos1();
                                BlockVector3 pos2 = selection.getBoundingBox().getPos2();
                                Location bukkitPos1 = BukkitAdapter.adapt(world, pos1);
                                Location bukkitPos2 = BukkitAdapter.adapt(world, pos2);

                                bukkitSelection = BoundingBox.of(bukkitPos1, bukkitPos2);
                            } catch (IncompleteRegionException e) {
                                player.sendMessage("§cPlease select a WorldEdit region first");
                                return;
                            }

                            ArrayList<Spawnpoint> spawnPoints = QuakePlugin.scanSpawnpointsIn(player.getWorld(), bukkitSelection);
                            List<MatchMode> recommendedModes = (List<MatchMode>) args.get("recommendedModes");

                            String name = (String) args.get("name");
                            Integer neededPlayers = (Integer) args.get("neededPlayers");
                            assert recommendedModes != null;
                            assert neededPlayers != null;
                            QMap qMap = new QMap(name, world, bukkitSelection, new ArrayList<>(spawnPoints), new ArrayList<>(recommendedModes), neededPlayers);
                            if (QuakePlugin.INSTANCE.maps == null)
                                QuakePlugin.INSTANCE.maps = new ArrayList<>(8);

                            if (QuakePlugin.INSTANCE.getMap(name) != null) {
                                player.sendMessage("§cThere is already a map with that name");
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
                                world.spawnParticle(Particle.SPELL_INSTANT, spawnPoint.pos, 64, 0.5, 0.5, 0.5);
                                world.setBlockData(spawnPoint.pos, Material.AIR.createBlockData());
                            }

                            player.sendMessage("Map \""+name+"\" has been created");
                        })
                )
                .withSubcommand(new CommandAPICommand("remove")
                        .withArguments(new StringArgument("mapName"))
                        .executesPlayer((player, args) -> {
                            String name = (String) args.get("mapName");
                            QMap map = QuakePlugin.INSTANCE.getMap(name);
                            if (map == null) {
                                player.sendMessage("§cNo such map");
                                return;
                            }
                            // Place spawnpoints
                            for (Spawnpoint spawnPoint : map.spawnPoints) {
                                QuakePlugin.placeSpawnpoint(spawnPoint);
                                map.world.spawnParticle(Particle.REDSTONE, spawnPoint.pos, 64, 0.5, 0.5, 0.5, new Particle.DustOptions(Color.fromRGB(0xFF0000), 2));
                            }

                            // Do removal
                            QuakePlugin.INSTANCE.maps.remove(map);
                            player.sendMessage("Map \""+name+"\" removed");
                        })
                )
                .withSubcommand(new CommandAPICommand("list")
                        .executes((sender, args) -> {
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
                                player.sendMessage("§cWorldEdit is either not installed or loaded");
                                return;
                            }

                            String name = (String) args.get("mapName");
                            QMap map = QuakePlugin.INSTANCE.getMap(name);
                            if (map == null) {
                                player.sendMessage("§cNo such map");
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

                                player.sendMessage("Selection set to map bounds.");
//                            } catch (IncompleteRegionException e) {
//                                throw new RuntimeException(e);
//                            }
                        })
                );


        StringArgument matchModeArg = (StringArgument) new StringArgument("mode")
                .includeSuggestions(ArgumentSuggestions.strings("debug", "ffa", "tdm", "ctf"));
        IntegerArgument needPlayersArg = new IntegerArgument("needPlayers");
        CommandAPICommand matchCmd = new CommandAPICommand("match")
                .withSubcommand(new CommandAPICommand("create")
                        .withArguments(matchModeArg, needPlayersArg, new StringArgument("mapName"))
                        .executes((sender, args) -> {
                            String mode = (String) args.get("mode");
                            String mapName = (String) args.get("mapName");
                            Integer needPlayers = (Integer) args.get("needPlayers");

                            QMap map = QuakePlugin.INSTANCE.getMap(mapName);
                            if (map == null) {
                                sender.sendMessage("§cNo such map");
                                return;
                            }

                            for (Match match : QuakePlugin.INSTANCE.matchManager.matches) {
                                if (match.getMap().name.equals(mapName)) {
                                    sender.sendMessage("§cThere is already a match in progress on "+mapName+". Please wait for the match to finish.");
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
                                sender.sendMessage("§cInvalid mode \""+mode+"\"");
                                return;
                            }

                            MatchManager matchManager = QuakePlugin.INSTANCE.matchManager;
                            Match match = matchManager.newMatch(matchFactory, map);
                            if (match == null) {
                                sender.sendMessage("§cAn error has occurred, please contact the server administrators.");
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
                                player.sendMessage("§cYou are in a match, please leave if you wish to join another");
                                return;
                            }

                            int index = (int) args.get("index");
                            if (index >= matchManager.matches.size() || index < 0) {
                                player.sendMessage("§cNo such match");
                                return;
                            }

                            matchManager.matches.get(index).join(player, null);
                        })
                )
                .withSubcommand(new CommandAPICommand("leave")
                        .executesPlayer((player, args) -> {
                            QuakeUserState userState = QuakePlugin.INSTANCE.userStates.get(player);
                            if (userState.currentMatch == null) {
                                player.sendMessage("§cYou aren't in a match");
                                return;
                            }
                            userState.currentMatch.leave(player);
                        })
                )
                .withSubcommand(new CommandAPICommand("list")
                        .executes((sender, args) -> {
                            MatchManager matchManager = QuakePlugin.INSTANCE.matchManager;
                            if (matchManager.matches.isEmpty()) {
                                sender.sendMessage("§cThere are no matches");
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
                                player.sendMessage("§cOnly party leader has access to this command");
                                return;
                            }

                            if (MatchmakingManager.INSTANCE.findPendingParty(player) != null) {
                                player.sendMessage("§cYou are already searching for a match!");
                                return;
                            }

                            if (userState.currentMatch != null) {
                                player.sendMessage("§cPlease leave the match before starting a search");
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
                                player.sendMessage("§cQueuing for more than one map is currently unsupported");
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
                                player.sendMessage("§cOnly party leader has access to this command");
                                return;
                            }

                            if (userState.mmState.currentPendingMatch != null) {
                                player.sendMessage("§cPlease accept the match first");
                                return;
                            } else if (userState.mmState.acceptedPendingMatch != null) {
                                player.sendMessage("§cYou can't cancel after accepting the match!");
                                return;
                            }

                            boolean wasSearching = MatchmakingManager.INSTANCE.stopSearching(player);
                            if (wasSearching)
                                player.sendMessage("Search cancelled");
                            else
                                player.sendMessage("§cAlready cancelled");
                        })
                )
                .withSubcommand(new CommandAPICommand("accept")
                        .executesPlayer((player, args) -> {
                            QuakeUserState userState = QuakePlugin.INSTANCE.userStates.get(player);
                            if (userState.mmState.currentPendingMatch != null) {
                                userState.mmState.currentPendingMatch.accept(player);
                            } else {
                                player.sendMessage("§cYou have no pending match to accept!");
                                return;
                            }
                        })
                );

        CommandAPICommand partyCmd = new CommandAPICommand("party")
                .withSubcommand(new CommandAPICommand("invite")
                        .withArguments(new PlayerArgument("player"))
                        .executesPlayer((player, args) -> {
                            Player invitee = (Player) args.get("player");
                            assert invitee != null;

                            if (player == invitee) {
                                player.sendMessage("§cYou can't invite yourself!");
                                return;
                            }

                            QuakeUserState inviteeState = QuakePlugin.INSTANCE.userStates.get(invitee);
                            QuakeUserState inviterState = QuakePlugin.INSTANCE.userStates.get(player);
                            if (inviterState.mmState.currentParty.getPlayers().contains(invitee)) {
                                player.sendMessage("§cThis player is already in your party");
                                return;
                            }
                            inviteeState.mmState.invitationParty = inviterState.mmState.currentParty;

                            invitee.sendMessage(player.getName()+" has invited you to their party. Type \"/quake party accept\" to accept the invite.\n" +
                                    "This invite is valid for 30 seconds, if you wish to deny, do not accept.");
                            inviterState.mmState.currentParty.sendMessage(Component.textOfChildren(
                                    MatchmakingManager.Party.PREFIX, Component.text(player.getName()+" invited "+invitee.getName())
                            ));

                            new BukkitRunnable() {
                                @Override
                                public void run() {
                                    if (inviteeState.mmState.invitationParty == null) return;

                                    invitee.sendMessage("§cYour party invite has expired");
                                    player.sendMessage("§c"+invitee.getName()+" did not accept your party invite");

                                    inviteeState.mmState.invitationParty = null;
                                }
                            }.runTaskLater(QuakePlugin.INSTANCE, 30*20);
                        })
                )
                .withSubcommand(new CommandAPICommand("accept")
                        .executesPlayer((player, args) -> {
                            QuakeUserState inviteeState = QuakePlugin.INSTANCE.userStates.get(player);

                            if (inviteeState.mmState.invitationParty == null) {
                                player.sendMessage("§cYou have no pending party invite to accept!");
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
                                        isLeader?Component.text(" (leader)"):Component.empty()
                                ));
                            }
                        })
                )
                .withSubcommand(new CommandAPICommand("kick")
                        .withArguments(new PlayerArgument("player"))
                        .executesPlayer((player, args) -> {
                            QuakeUserState userState = QuakePlugin.INSTANCE.userStates.get(player);
                            if (userState.mmState.currentParty.leader != player) {
                                player.sendMessage("§cOnly party leader has access to this command");
                                return;
                            }

                            Player annoyingPlayer = (Player) args.get("player");
                            assert annoyingPlayer != null;

                            if (player == annoyingPlayer) {
                                player.sendMessage("§cYou can't kick yourself!");
                                return;
                            }

                            if (!userState.mmState.currentParty.getPlayers().contains(annoyingPlayer)) {
                                player.sendMessage("§c"+annoyingPlayer.getName()+" is not in your party");
                                return;
                            }
                            userState.mmState.currentParty.removePlayer(annoyingPlayer);

                            userState.mmState.currentParty.sendMessage(Component.textOfChildren(
                                    MatchmakingManager.Party.PREFIX, player.displayName(), Component.text(" kicked "), annoyingPlayer.displayName()
                            ));
                            annoyingPlayer.sendMessage("§cYou've been kicked from "+player.getName()+"'s party");
                        })
                )
                .withSubcommand(new CommandAPICommand("leave")
                        .executesPlayer((player, args) -> {
                            QuakeUserState userState = QuakePlugin.INSTANCE.userStates.get(player);
                            MatchmakingManager.Party partyToLeave = userState.mmState.currentParty;
                            if (userState.mmState.currentParty.size() == 1) {
                                player.sendMessage("§cYou are the only one in this party");
                                return;
                            }

                            if (userState.mmState.currentParty.leader == player)
                                player.sendMessage("§eYou are leaving your own party. The party will have someone else selected as the leader.");
                            userState.mmState.currentParty.removePlayer(player);
                            player.sendMessage("§aYou left "+partyToLeave.leader.getName()+"'s party");
                        })
                );

        CommandAPICommand entityCmd = new CommandAPICommand("entity")
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
                        player.sendMessage("§cThis is a bug! This chatroom is invalid!");
                        player.sendMessage("§c"+e.getMessage());
                        return;
                    }

                    QuakeUserState userState = QuakePlugin.INSTANCE.userStates.get(player);
                    userState.switchChat(chatroom);
                });

        CommandAPICommand test = new CommandAPICommand("test")
                .executesPlayer((player, args) -> {
                    TableBuilder table = new TableBuilder();

                    table.addRow("Name", "Score");
                    table.addRow("Polyzium7", "1");

                    player.sendMessage(Component.text(table.build()).font(Key.key("mono")));
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
                        test
                )
                .register();
    }
}
