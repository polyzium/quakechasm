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

package com.github.polyzium.quakechasm;

import com.github.polyzium.quakechasm.events.listeners.*;
import com.github.polyzium.quakechasm.game.entities.DisplayPickup;
import com.github.polyzium.quakechasm.game.entities.QEntityUtil;
import com.github.polyzium.quakechasm.game.entities.Trigger;
import com.github.polyzium.quakechasm.game.entities.pickups.*;
import com.google.gson.reflect.TypeToken;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.joml.Matrix4d;
import org.joml.Matrix4f;
import com.github.polyzium.quakechasm.commands.Commands;
import com.github.polyzium.quakechasm.events.listeners.*;
import com.github.polyzium.quakechasm.game.entities.*;
import com.github.polyzium.quakechasm.game.entities.pickups.*;
import com.github.polyzium.quakechasm.game.entities.triggers.Jumppad;
import com.github.polyzium.quakechasm.game.entities.triggers.Portal;
import com.github.polyzium.quakechasm.matchmaking.MatchmakingManager;
import com.github.polyzium.quakechasm.matchmaking.matches.MatchManager;
import com.github.polyzium.quakechasm.matchmaking.Team;
import com.github.polyzium.quakechasm.matchmaking.map.QMap;
import com.github.polyzium.quakechasm.matchmaking.map.Spawnpoint;
import com.github.polyzium.quakechasm.menus.MenuManager;
import com.github.polyzium.quakechasm.misc.MiscUtil;
import com.github.polyzium.quakechasm.misc.TranslationManager;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QuakePlugin extends JavaPlugin {
    public static QuakePlugin INSTANCE;
    public static Location LOBBY;

    public final ArrayList<Trigger> triggers = new ArrayList<>();
    public Map<Player,QuakeUserState> userStates;
    private float rotatorAngle;
    public ArrayList<QMap> maps;
    public MatchManager matchManager;
    public MatchmakingManager matchmakingManager;
    public MenuManager menuManager;
    public TranslationManager translationManager;

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
//                        Location loc = pickupDisplay.getLocation();
//                        getLogger().warning(String.format("A DisplayPickup at %.1f %.1f %.1f has been removed manually", loc.x(), loc.y(), loc.z()));
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
        for (Trigger trigger : this.triggers) {
            if (entity == trigger.getEntity()) {
                QuakePlugin.INSTANCE.getLogger().warning("Attempted to add same trigger twice!");
                return;
            }
        }

        String entityType = QEntityUtil.getEntityType(entity);
        switch (entityType) {
            case "weapon_spawner":
                new WeaponSpawner((ItemDisplay) entity);
                break;
            case "item_spawner":
                getLogger().warning("ItemSpawner is deprecated, converting to WeaponSpawner");
                WeaponSpawner.convert((ItemDisplay) entity);
                break;
            case "health_spawner":
                new HealthSpawner((ItemDisplay) entity);
                break;
            case "armor_spawner":
                new ArmorSpawner((ItemDisplay) entity);
                break;
            case "ammo_spawner":
                new AmmoSpawner((ItemDisplay) entity);
                break;
            case "powerup_spawner":
                new PowerupSpawner((ItemDisplay) entity);
                break;
            case "ctf_flag":
                new CTFFlag((ItemDisplay) entity);
                break;
            case "jumppad":
                new Jumppad((Marker) entity);
                break;
            case "portal":
                new Portal((Marker) entity);
                break;
            default:
                getLogger().warning("Unknown entity type "+entityType+", ignoring.");
        }
    }

    public void loadTriggers() {
        this.triggers.clear();
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
        player.getAttribute(Attribute.MAX_HEALTH).setBaseValue(20);
        this.userStates.put(player, new QuakeUserState(player));
    }

    public void instantiateStates() {
        this.userStates = new HashMap<>(32);
        for (Player player : Bukkit.getOnlinePlayers()) {
            this.initPlayer(player);
        }
    }

    public static ArrayList<Spawnpoint> scanSpawnpointsIn(World world, BoundingBox bounds) {
        ArrayList<Spawnpoint> spawnpoints = new ArrayList<>();

        for (Entity entity : world.getNearbyEntities(bounds)) {
            if (!(entity instanceof ArmorStand armorStand)) continue;

            EntityEquipment equipment = armorStand.getEquipment();
            ItemStack chestplate = equipment.getChestplate();
            if (chestplate.getType() != Material.LEATHER_CHESTPLATE) continue;

            LeatherArmorMeta chestplateMeta = (LeatherArmorMeta) chestplate.getItemMeta();
            Team allowedTeam = switch (chestplateMeta.getColor().asRGB()) {
                case 0xb02e26 -> Team.RED;
                case 0x3c44aa -> Team.BLUE;
                case 0xfed83d -> Team.FREE;
                case 0x8932b8 -> Team.SPECTATOR;
                default -> null;
            };
            if (allowedTeam == null) continue;

            spawnpoints.add(new Spawnpoint(
                    armorStand.getLocation(),
                    new ArrayList<>(List.of(allowedTeam))
            ));
        }

        return spawnpoints;
    }

    public static void placeSpawnpoint(Spawnpoint spawnpoint) {
        Location location = spawnpoint.pos;
        Team team = spawnpoint.allowedTeams.get(0);

        ArmorStand armorStand = location.getWorld().spawn(location, ArmorStand.class);
        armorStand.setGravity(false);

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
    }

    public QMap getMap(String name) {
        for (QMap map : this.maps) {
            if (map.name.equals(name)) {
                return map;
            }
        }
        return null;
    }

    public void saveMaps() {
        File dataFolder = this.getDataFolder();
        if (!dataFolder.exists()) {
            if (!dataFolder.mkdir()) {
                getLogger().severe("Unable to create data folder "+dataFolder+". Maps won't be saved.");
                return;
            }
        }
        File mapFile = new File(dataFolder, "maps.json");
        try {
            if (!mapFile.exists()) {
                if (!mapFile.createNewFile()) {
                    getLogger().severe("Unable to create map file "+mapFile+". Maps won't be saved.");
                    return;
                };
            };
        } catch (IOException e) {
            getLogger().severe(e.getMessage());
            getLogger().severe("Unable to create map file "+mapFile+". Maps won't be saved.");
            return;
        }

//        ArrayList<QMap.QMapPersistent> persistentMaps = new ArrayList<>(8);
//        for (QMap map : this.maps) {
//            QMap.QMapPersistent persistentMap = map.toPersistent();
//            persistentMaps.add(persistentMap);
//        }

        String json = MiscUtil.getEnhancedGson().toJson(this.maps);
        try {
            FileWriter writer = new FileWriter(mapFile);
            writer.write(json);
            writer.close();
        } catch (IOException e) {
            getLogger().severe(e.getMessage());
            getLogger().severe("Unable to create map file "+mapFile+". Maps won't be saved.");
            return;
        }
    }

    public void loadMaps() {
        this.maps = new ArrayList<>(8);

        File dataFolder = this.getDataFolder();
        if (!dataFolder.exists()) {
            getLogger().severe("Data folder doesn't exist, maps won't be loaded.");
            return;
        }
        File mapFile = new File(dataFolder, "maps.json");
        if (!mapFile.exists()) {
            getLogger().severe("Map file doesn't exist, maps won't be loaded.");
            return;
        }

//        ArrayList<QMap.QMapPersistent> persistentMaps;
//        try {
//            FileReader reader = new FileReader(mapFile);
//            persistentMaps = MiscUtil.getEnhancedGson().fromJson(reader, new TypeToken<ArrayList<QMap.QMapPersistent>>(){}.getType());
//        } catch (IOException e) {
//            getLogger().severe(e.getMessage());
//            getLogger().severe("Unable to read map data from "+mapFile+". Maps won't be loaded.");
//            return;
//        }
//
//        this.maps = new ArrayList<>(8);
//        for (QMap.QMapPersistent persistentMap : persistentMaps) {
//            QMap qMap = QMap.fromPersistent(persistentMap);
//            this.maps.add(qMap);
//        }

        ArrayList<QMap> maps;
        try {
            FileReader reader = new FileReader(mapFile);
            maps = MiscUtil.getEnhancedGson().fromJson(reader, new TypeToken<ArrayList<QMap>>(){}.getType());
        } catch (IOException e) {
            getLogger().severe(e.getMessage());
            getLogger().severe("Unable to read map data from "+mapFile+". Maps won't be loaded.");
            return;
        }

        this.maps = maps;
    }

    @Override
    public void onEnable() {
        getLogger().info("Quakechasm initializing");

        // singleton pattern
        INSTANCE = this;

        // events
        getServer().getPluginManager().registerEvents(new MiscListener(), this);
        getServer().getPluginManager().registerEvents(new ChatListener(), this);
        getServer().getPluginManager().registerEvents(new TriggerListener(), this);
        getServer().getPluginManager().registerEvents(new CombatListener(), this);
        getServer().getPluginManager().registerEvents(new MenuListener(), this);
        getServer().getPluginManager().registerEvents(new MapperToolListener(), this);

        // lobby
        getLogger().info("Initializing lobby world");
        World lobbyWorld = new WorldCreator("flat")
                .createWorld();
        LOBBY = new Location(
                lobbyWorld,
                2977, 89, 2992
        );

        // other stuff
        getLogger().info("Initializing match manager");
        this.matchManager = new MatchManager();
        getLogger().info("Initializing matchmaking manager");
        this.matchmakingManager = new MatchmakingManager();
        getLogger().info("Initializing menu manager");
        this.menuManager = new MenuManager();
        getLogger().info("Initializing translation manager");
        try {
            this.translationManager = new TranslationManager();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        getLogger().info("Instantiating states for current players");
        this.instantiateStates();
        getLogger().info("Loading maps");
        this.loadMaps();
        getLogger().info("Loading triggers");
        this.loadTriggers();
        this.startRotatingPickups();
        this.startHudUpdater();

        // commands
        Commands.initQuakeCommand();
    }

    @Override
    public void onDisable() {
        getLogger().info("Quakechasm is shutting down");

        HandlerList.unregisterAll(this);
        getServer().getScheduler().cancelTasks(this);

        getLogger().info("Respawning all empty spawners");
        for (Trigger trigger : this.triggers) {
            if (!(trigger instanceof Spawner)) continue;
            ((Spawner) trigger).respawn();
        }

        getLogger().info("Untracking triggers");
        this.triggers.clear();

        getLogger().info("Saving maps");
        if (this.maps != null) {
            this.saveMaps();
            this.maps.clear();
        } else {
            getLogger().warning("Maps list is null, skipping save");
        }

        getLogger().info("Quakechasm disabled. Goodbye!");
    }
}
