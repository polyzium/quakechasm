package ru.darkchronics.quake.commands;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.darkchronics.quake.QuakePlugin;
import ru.darkchronics.quake.game.entities.pickups.HealthSpawner;
import ru.darkchronics.quake.game.entities.pickups.ItemSpawner;
import ru.darkchronics.quake.game.entities.triggers.Jumppad;

public class QuakeMasterCmd implements CommandExecutor {
    private QuakePlugin plugin;

    public QuakeMasterCmd(QuakePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command is for players only");
            return false;
        }

        // TODO rewrite this fucking mess
        if (args[0].equals("itemspawner")) {
            if (args.length == 2 && args[1].equals("create")) {
                Player player = (Player) sender;
                if (player.getInventory().getItemInMainHand().isEmpty()) {
                    sender.sendMessage("§cSelect an item first!");
                    return false;
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
                        this.plugin
                );
                sender.sendMessage(String.format("Made an ItemSpawner at %.1f %.1f %.1f", loc.x(), loc.y(), loc.z()));
            }
        } else if (args[0].equals("healthspawner")) {
            if (args.length == 3 && args[1].equals("create")) {
                Player player = (Player) sender;
                Location loc = player.getLocation();
                loc.set(
                        Math.floor(loc.x())+0.5,
                        loc.y() + 1,
                        Math.floor(loc.z())+0.5
                );
                int health = 0;
                switch (args[2]) {
                    case "small":
                        health = 1;
                        break;
                    case "medium":
                        health = 5;
                        break;
                    case "large":
                        health = 10;
                        break;
                    default:
                        sender.sendMessage("§cWrong health type, use either of: small, medium, large");
                        return false;
                }
                new HealthSpawner(
                        health,
                        player.getWorld(),
                        loc,
                        this.plugin
                );
                sender.sendMessage(String.format("Made a HealthSpawner at %.1f %.1f %.1f", loc.x(), loc.y(), loc.z()));
            }
        } else if (args[0].equals("jumppad")) {
        if (args.length == 3 && args[1].equals("create")) {
            Player player = (Player) sender;
            Location loc = player.getLocation();
            loc.set(
                    Math.floor(loc.x())+0.5,
                    Math.floor(loc.y()),
                    Math.floor(loc.z())+0.5
            );

            new Jumppad(
                    loc,
                    player.getEyeLocation().getDirection().multiply(Double.parseDouble(args[2])),
                    plugin
            );
            sender.sendMessage(String.format("Made a Jumppad at %.1f %.1f %.1f", loc.x(), loc.y(), loc.z()));
        }
    }

        return true;
    }
}
