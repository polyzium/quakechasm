package ru.darkchronics.quake;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.joml.Matrix4d;
import org.joml.Matrix4f;
import ru.darkchronics.quake.commands.Commands;
import ru.darkchronics.quake.events.TriggerListener;
import ru.darkchronics.quake.events.CombatListener;
import ru.darkchronics.quake.game.entities.*;
import ru.darkchronics.quake.events.MiscListener;
import ru.darkchronics.quake.game.entities.pickups.*;
import ru.darkchronics.quake.game.entities.triggers.Jumppad;
import ru.darkchronics.quake.game.entities.triggers.Portal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class QuakePlugin extends JavaPlugin {
    public static QuakePlugin INSTANCE;
    public ArrayList<Trigger> triggers;
    public Map<Player,QuakeUserState> userStates;
    public ArrayList<Location> spawnpoints;
    private float rotatorAngle;

    public void startRotatingPickups() {
        this.rotatorAngle = 0;
        BukkitRunnable rotator = new BukkitRunnable() {
            @Override
            public void run() {
                rotatorAngle += Math.PI/2;
                for (Trigger trigger : triggers) {
                    if (!(trigger instanceof DisplayPickup)) continue;

                    ItemDisplay pickupDisplay = ((DisplayPickup) trigger).getDisplay();
                    if (pickupDisplay.isDead() && pickupDisplay.isEmpty()) {
                        Location loc = pickupDisplay.getLocation();
                        getLogger().warning(String.format("A DisplayPickup at %.1f %.1f %.1f has been removed manually", loc.x(), loc.y(), loc.z()));
                        triggers.remove(trigger);
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

    public void startHudUpdater() {
       new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : userStates.keySet()) {
                    QuakeUserState userState = userStates.get(player);
                    userState.hud.draw(userState);
                }
            }
        }.runTaskTimer(this, 0, 1);
    }

    public void loadTrigger(Entity entity) {
        String entityType = QEntityUtil.getEntityType(entity);
        switch (entityType) {
            case "item_spawner":
                new ItemSpawner((ItemDisplay) entity, this);
                break;
            case "health_spawner":
                new HealthSpawner((ItemDisplay) entity, this);
                break;
            case "armor_spawner":
                new ArmorSpawner((ItemDisplay) entity, this);
                break;
            case "ammo_spawner":
                new AmmoSpawner((ItemDisplay) entity, this);
                break;
            case "powerup_spawner":
                new PowerupSpawner((ItemDisplay) entity, this);
                break;
            case "jumppad":
                new Jumppad((Marker) entity, this);
                break;
            case "portal":
                new Portal((Marker) entity, this);
                break;
            default:
                getLogger().warning("Unknown entity type "+entityType+", ignoring.");
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

    public void initPlayer(Player player) {
        player.setWalkSpeed(0.4f);
        player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(20);
        this.userStates.put(player, new QuakeUserState(player));
    }

    public void instantiateStates() {
        this.userStates = new HashMap<>(32);
        for (Player player : Bukkit.getOnlinePlayers()) {
            this.initPlayer(player);
        }
    }

    public void scanSpawnpoints() {
        // TODO scan spawnpoints per map; to avoid lags due to very expensive scanning, use marker entities
        World world = Bukkit.getWorld("flat");
        this.spawnpoints = new ArrayList<>(16);
        for (Chunk chunk : world.getLoadedChunks()) {
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    for (int y = 0; y < 256; y++) {
                        Block block = chunk.getBlock(x, y, z);
                        if (block.getType() == Material.RED_CARPET) {
                            this.spawnpoints.add(block.getLocation());
                        }
                    }
                }
            }
        }
    }

    @Override
    public void onEnable() {
        getLogger().info("DarkChronics Quake initializing");
        getLogger().severe("!!! FOR INTERNAL USE ONLY !!!");
        getLogger().severe("!!! IF YOU LEAK THIS, YOU ARE A PIECE OF SHIT !!!");

        // commands
        Commands.initQuakeCommand(this);

        // events
        getServer().getPluginManager().registerEvents(new MiscListener(this), this);
        getServer().getPluginManager().registerEvents(new TriggerListener(this), this);
        getServer().getPluginManager().registerEvents(new CombatListener(this), this);

        // other stuff
        getLogger().info("Instantiating states for current players");
        this.instantiateStates();
        getLogger().info("Scanning spawnpoints in loaded chunks");
        this.scanSpawnpoints();
        getLogger().info("Loading triggers");
        this.loadTriggers();
        this.startRotatingPickups();
        this.startHudUpdater();
        
        // singleton pattern
        INSTANCE = this;
    }

    @Override
    public void onDisable() {
        getLogger().info("DarkChronics Quake is shutting down");

        HandlerList.unregisterAll(this);
        getServer().getScheduler().cancelTasks(this);

        getLogger().info("Respawning all empty spawners");
        for (Trigger trigger : this.triggers) {
            if (!(trigger instanceof Spawner)) continue;
            ((Spawner) trigger).respawn();
        }

        getLogger().info("Untracking triggers");
        this.triggers.clear();

        getLogger().info("Clearing spawnpoints");
        this.spawnpoints.clear();

        getLogger().info("DarkChronics Quake disabled. Goodbye!");
    }
}
