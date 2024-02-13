package ru.darkchronics.quake.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.DoubleArgument;
import dev.jorel.commandapi.arguments.LocationArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.util.Vector;
import ru.darkchronics.quake.QuakePlugin;
import ru.darkchronics.quake.QuakeUserState;
import ru.darkchronics.quake.game.combat.WeaponUtil;
import ru.darkchronics.quake.game.entities.QEntityUtil;
import ru.darkchronics.quake.game.entities.Trigger;
import ru.darkchronics.quake.game.entities.pickups.AmmoSpawner;
import ru.darkchronics.quake.game.entities.pickups.HealthSpawner;
import ru.darkchronics.quake.game.entities.pickups.ItemSpawner;
import ru.darkchronics.quake.game.entities.triggers.Jumppad;
import ru.darkchronics.quake.game.entities.triggers.Portal;
import ru.darkchronics.quake.misc.MiscUtil;
import ru.darkchronics.quake.misc.ParticleUtil;

import java.util.ArrayList;
import java.util.Arrays;

public abstract class Commands {
   
    public static void initQuakeCommand(QuakePlugin plugin) {
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
                                    player.getEyeLocation().getDirection().multiply((double) args.get("power")),
                                    plugin
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

                            for (Trigger trigger : plugin.triggers) {
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
                                            String.format("<click:run_command:/quake jumppad fromTry %d %d %d %f %f %f><hover:show_text:'Click here to create a jumppad based on the results.'><green>[Create]</green>",
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
                                    launchVector,
                                    plugin
                            );
                            player.sendMessage(String.format("Made a Jumppad at %.1f %.1f %.1f", jpLocation.x(), jpLocation.y(), jpLocation.z()));
                        })
                );

        CommandAPICommand portalCmd = new CommandAPICommand("portal")
                .withSubcommand(new CommandAPICommand("create")
                        .executesPlayer((player, args) -> {
                            QuakeUserState state = plugin.userStates.get(player);
                            if (state.portalLoc == null) {
                                state.portalLoc = player.getLocation();
                                player.sendMessage("Portal location stored. Go to the teleport target location and run the command again to create the portal.\nOtherwise, do /quake portal cancel");
                                return;
                            }

                            Location ploc = player.getLocation();
                            new Portal(state.portalLoc, ploc, plugin);
                            player.sendMessage(String.format("Made a Portal at %.1f %.1f %.1f, linked to %.1f %.1f %.1f",
                                    state.portalLoc.x(), state.portalLoc.y(), state.portalLoc.z(),
                                    ploc.x(), ploc.y(), ploc.z()
                            ));
                            state.portalLoc = null;
                        })
                )
                .withSubcommand(new CommandAPICommand("cancel")
                        .executesPlayer((player, args) -> {
                            QuakeUserState state = plugin.userStates.get(player);
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
                                    loc,
                                    plugin
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
                                    loc,
                                    plugin
                            );
                            player.sendMessage(String.format("Made an AmmoSpawner at %.1f %.1f %.1f", loc.x(), loc.y(), loc.z()));
                        })
                );

        CommandAPICommand itemSpawnerCmd = new CommandAPICommand("itemspawner")
                .withSubcommand(new CommandAPICommand("create"))
                .executesPlayer((player, args) -> {
                    if (player.getInventory().getItemInMainHand().isEmpty()) {
                        player.sendMessage("§cSelect an item first!");
                        return;
                    }
                    Location loc = player.getLocation();
                    loc.set(
                            Math.floor(loc.x())+0.5,
                            Math.floor(loc.y()) + 1,
                            Math.floor(loc.z())+0.5
                    );
                    new ItemSpawner(
                            player.getInventory().getItemInMainHand(),
                            player.getWorld(),
                            loc,
                            plugin
                    );
                    player.sendMessage(String.format("Made an ItemSpawner at %.1f %.1f %.1f", loc.x(), loc.y(), loc.z()));
                });

        CommandAPICommand reloadSpawnsCmd = new CommandAPICommand("reloadSpawns")
                .executes((sender, args) -> {
                    plugin.scanSpawnpoints();
                    sender.sendMessage("Spawnpoints reloaded");
                });

        CommandAPICommand reloadCmd = new CommandAPICommand("reload")
                .executes((sender, args) -> {
                    plugin.onDisable();
                    plugin.onEnable();
                    sender.sendMessage("Plugin reloaded");
                    Bukkit.getServer().broadcast(
                            Component.text("[DarkChronics-Quake]").color(TextColor.color(0x8e60e0)).append(
                                    Component.text(" Plugin reloaded").color(TextColor.color(0xffffff)))
                    );
                });

        new CommandAPICommand("quake")
                .withAliases("dcquake")
                .withSubcommands(
                        itemSpawnerCmd,
                        healthSpawnerCmd,
                        ammoSpawnerCmd,
                        jumppadCmd,
                        portalCmd,
                        reloadCmd,
                        reloadSpawnsCmd
                )
                .register();
    }
}
