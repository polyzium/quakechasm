package ru.darkchronics.quake;

import org.bukkit.Location;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.joml.Matrix4d;
import org.joml.Matrix4f;
import ru.darkchronics.quake.commands.QuakeMasterCmd;
import ru.darkchronics.quake.events.MoveListener;
import ru.darkchronics.quake.events.WeaponsListener;
import ru.darkchronics.quake.game.entities.ItemSpawner;

import java.util.ArrayList;

public class QuakePlugin extends JavaPlugin {
    public ArrayList<ItemSpawner> itemSpawners;
    private float rotatorAngle;

    public void startRotatingItemSpawners() {
        this.itemSpawners = new ArrayList<>();
        this.rotatorAngle = 0;
        BukkitRunnable rotator = new BukkitRunnable() {
            @Override
            public void run() {
                rotatorAngle += Math.PI/2;
                for (int i = 0; i < itemSpawners.size(); i++) {
                    ItemDisplay spawnerDisplay = itemSpawners.get(i).display;
                    if (spawnerDisplay.isDead() && spawnerDisplay.isEmpty()) {
                        Location loc = spawnerDisplay.getLocation();
                        getLogger().warning(String.format("An ItemSpawner at %.1f %.1f %.1f has been removed manually", loc.x(), loc.y(), loc.z()));
                        itemSpawners.remove(i);
                        return;
                    }
                    spawnerDisplay.setInterpolationDelay(-1);
                    spawnerDisplay.setTransformationMatrix(new Matrix4f(new Matrix4d(
                            Math.cos(rotatorAngle),0,Math.sin(rotatorAngle),0,
                            0,1,0,0,
                            -Math.sin(rotatorAngle),0,Math.cos(rotatorAngle),0,
                            0,0,0,1
                    )));
                }
            }
        };
        rotator.runTaskTimer(this, 0, 20);
    }

    @Override
    public void onEnable() {
        getLogger().info("DarkChronics Quake initializing");
        getLogger().severe("!!! FOR INTERNAL USE ONLY !!!");
        getLogger().severe("!!! IF YOU LEAK THIS, YOU ARE A PIECE OF SHIT !!!");

        // commands
        this.getCommand("quake").setExecutor(new QuakeMasterCmd(this));

        // events
        getServer().getPluginManager().registerEvents(new MoveListener(this), this);
        getServer().getPluginManager().registerEvents(new WeaponsListener(), this);

        // other stuff
        this.startRotatingItemSpawners();
    }

    @Override
    public void onDisable() {
        getLogger().info("DarkChronics Quake is shutting down");
    }
}
