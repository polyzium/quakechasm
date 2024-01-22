package ru.darkchronics.quake.commands;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import ru.darkchronics.quake.QuakePlugin;
import ru.darkchronics.quake.game.entities.ItemSpawner;

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
                        loc.y() + 1,
                        Math.floor(loc.z())+0.5
                );
                new ItemSpawner(
                        player.getInventory().getItemInMainHand(),
                        100,
                        player.getWorld(),
                        loc,
                        this.plugin
                );
                sender.sendMessage(String.format("Made an ItemSpawner at %.1f %.1f %.1f", loc.x(), loc.y(), loc.z()));
            }
        }

        return true;
    }
}
