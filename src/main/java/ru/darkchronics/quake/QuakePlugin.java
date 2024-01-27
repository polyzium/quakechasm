package ru.darkchronics.quake;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.joml.Matrix4d;
import org.joml.Matrix4f;
import ru.darkchronics.quake.commands.QuakeMasterCmd;
import ru.darkchronics.quake.events.TriggerListener;
import ru.darkchronics.quake.events.CombatListener;
import ru.darkchronics.quake.game.entities.*;
import ru.darkchronics.quake.events.MiscListener;
import ru.darkchronics.quake.game.entities.pickups.HealthSpawner;
import ru.darkchronics.quake.game.entities.pickups.ItemSpawner;
import ru.darkchronics.quake.game.entities.triggers.Jumppad;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class QuakePlugin extends JavaPlugin {

    public ArrayList<Trigger> triggers;
    public Map<Player,QuakeUserState> userStates;
    private float rotatorAngle;

    public void startRotatingPickups() {
        this.rotatorAngle = 0;
        BukkitRunnable rotator = new BukkitRunnable() {
            @Override
            public void run() {
                rotatorAngle += Math.PI/2;
                for (int i = 0; i < triggers.size(); i++) {
                    if (!(triggers.get(i) instanceof DisplayPickup)) continue;

                    ItemDisplay pickupDisplay = ((DisplayPickup) triggers.get(i)).getDisplay();
                    if (pickupDisplay.isDead() && pickupDisplay.isEmpty()) {
                        Location loc = pickupDisplay.getLocation();
                        getLogger().warning(String.format("An ItemSpawner at %.1f %.1f %.1f has been removed manually", loc.x(), loc.y(), loc.z()));
                        triggers.remove(i);
                        return;
                    }
                    pickupDisplay.setInterpolationDelay(-1);
                    pickupDisplay.setTransformationMatrix(new Matrix4f(new Matrix4d(
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

    public void loadTrigger(Entity entity) {
        String entityType = QEntityUtil.getEntityType(entity);
        switch (entityType) {
            case "item_spawner":
                this.triggers.add(new ItemSpawner((ItemDisplay) entity, this));
                break;
            case "health_spawner":
                this.triggers.add(new HealthSpawner((ItemDisplay) entity, this));
                break;
            case "jumppad":
                this.triggers.add(new Jumppad((Marker) entity, this));
                break;
            default:
                getLogger().warning("Unknown entity type "+entityType+", ignoring.");
                break;
        }
    }

    public void loadTriggers() {
        this.triggers = new ArrayList<>();
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                String entityType = QEntityUtil.getEntityType(entity);
                if (entityType == null) continue;
                this.loadTrigger(entity);
                getLogger().info(String.format("Found %s in %s at %.1f %.1f %.1f", QEntityUtil.getEntityType(entity), world.getName(), entity.getX(), entity.getY(), entity.getZ()));
            }
        }
    }

    public void instantiateStates() {
        this.userStates = new HashMap<>(32);
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.setWalkSpeed(0.4f);
            player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(20);
            this.userStates.put(player, new QuakeUserState(this));
        }

    }

    @Override
    public void onEnable() {
        getLogger().info("DarkChronics Quake initializing");
        getLogger().severe("!!! FOR INTERNAL USE ONLY !!!");
        getLogger().severe("!!! IF YOU LEAK THIS, YOU ARE A PIECE OF SHIT !!!");

        // commands
        this.getCommand("quake").setExecutor(new QuakeMasterCmd(this));

        // events
        getServer().getPluginManager().registerEvents(new MiscListener(this), this);
        getServer().getPluginManager().registerEvents(new TriggerListener(this), this);
        getServer().getPluginManager().registerEvents(new CombatListener(this), this);

        // other stuff
        getLogger().info("Instantiating states for current players");
        this.instantiateStates();
        getLogger().info("Loading triggers");
        this.loadTriggers();
        this.startRotatingPickups();
    }

    @Override
    public void onDisable() {
        getLogger().info("DarkChronics Quake is shutting down");
    }
}
