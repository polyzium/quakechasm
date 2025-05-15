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

package ru.darkchronics.quake.matchmaking.map;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.util.BoundingBox;
import ru.darkchronics.quake.QuakePlugin;
import ru.darkchronics.quake.game.entities.Trigger;
import ru.darkchronics.quake.game.entities.pickups.CTFFlag;
import ru.darkchronics.quake.game.entities.pickups.PowerupSpawner;
import ru.darkchronics.quake.game.entities.pickups.Spawner;
import ru.darkchronics.quake.matchmaking.matches.MatchMode;
import ru.darkchronics.quake.matchmaking.matches.CTFMatch;
import ru.darkchronics.quake.matchmaking.matches.Match;
import ru.darkchronics.quake.matchmaking.matches.MatchManager;
import ru.darkchronics.quake.matchmaking.Team;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

import static ru.darkchronics.quake.misc.MiscUtil.chunkIntersectsBoundingBox;

public class QMap {
    public String name;
    public String displayName;
    public World world;
    public BoundingBox bounds;
    public ArrayList<Spawnpoint> spawnPoints;
    public ArrayList<MatchMode> recommendedModes;
    public int neededPlayers;

    public QMap() {}

    public QMap(String name, String displayName, World world, BoundingBox bounds, ArrayList<Spawnpoint> spawnPoints, ArrayList<MatchMode> recommendedModes, int neededPlayers) {
        this.name = name;
        this.displayName = displayName;
        this.world = world;
        this.bounds = bounds;
        this.spawnPoints = spawnPoints;
        this.recommendedModes = recommendedModes;
        this.neededPlayers = neededPlayers;
    }

    // Call this ONLY WHEN STARTING A MATCH!!!
    public void chunkLoad() {
        for (Chunk chunk : this.getChunks()) {
            chunk.setForceLoaded(true);
            chunk.load();
        }
    }

    public List<Chunk> getChunks() {
        List<Chunk> chunks = new ArrayList<>();

        int minX = (int) Math.floor(this.bounds.getMinX()) >> 4;
        int minZ = (int) Math.floor(this.bounds.getMinZ()) >> 4;
        int maxX = (int) Math.floor(this.bounds.getMaxX()) >> 4;
        int maxZ = (int) Math.floor(this.bounds.getMaxZ()) >> 4;

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                Chunk chunk = world.getChunkAt(x, z);
                if (chunkIntersectsBoundingBox(chunk, this.bounds)) {
                    chunks.add(chunk);
                }
            }
        }

        return chunks;
    }

    public Location getRandomSpawnpoint(Team team) {
        Predicate<Spawnpoint> belongsToTeam = spawnpoint -> spawnpoint.allowedTeams.contains(team);
        Predicate<Spawnpoint> freeIfTeam = spawnpoint -> ((team == Team.RED || team == Team.BLUE) && spawnpoint.allowedTeams.contains(Team.FREE));
        Predicate<Spawnpoint> teamIfFree = spawnpoint -> (team == Team.FREE && (spawnpoint.allowedTeams.contains(Team.RED) || spawnpoint.allowedTeams.contains(Team.BLUE)));

        List<Spawnpoint> allowedSpawnpoints = this.spawnPoints.stream()
                .filter(
                        sp -> belongsToTeam.test(sp) || freeIfTeam.test(sp) || teamIfFree.test(sp)
                )
                .toList();

        Spawnpoint spawnPoint = allowedSpawnpoints.get(
                (int) (Math.random() * (allowedSpawnpoints.size() - 1))
        );

        Location spLoc = spawnPoint.pos;

        return spLoc;
    }

    public void despawnPowerups(Match match) {
        Collection<Entity> entities = this.world.getNearbyEntities(this.bounds);
        for (Trigger trigger : QuakePlugin.INSTANCE.triggers) {
            if (entities.contains(trigger.getEntity()) && trigger instanceof PowerupSpawner powerupSpawner) {
                powerupSpawner.despawn(match);
            }
        }
    }

    public void respawnItems() {
        Collection<Entity> entities = this.world.getNearbyEntities(this.bounds);
        for (Trigger trigger : QuakePlugin.INSTANCE.triggers) {
            if (entities.contains(trigger.getEntity()) && trigger instanceof Spawner spawner) {
                spawner.respawn();
            }
        }
    }

    public void prepareForMatch(Match match) {
        this.respawnItems();
        this.despawnPowerups(match);

        if (match instanceof CTFMatch ctf) {
            Collection<Entity> entities = this.world.getNearbyEntities(this.bounds);
            for (Trigger trigger : QuakePlugin.INSTANCE.triggers) {
                if (entities.contains(trigger.getEntity()) && trigger instanceof CTFFlag flag) {
                    flag.prepareForMatch(ctf);
                }
            }
        }
    }

    public Match getMatch() {
        for (Match match : MatchManager.INSTANCE.matches) {
            if (match.getMap() == this)
                return match;
        }

        return null;
    }

    public void cleanup() {
        Collection<Entity> entities = this.world.getNearbyEntities(this.bounds);
        ArrayList<PowerupSpawner> powerupsForCleanup = new ArrayList<>();
        for (Trigger trigger : QuakePlugin.INSTANCE.triggers) {
            if (entities.contains(trigger.getEntity())) {
                if (trigger instanceof PowerupSpawner powerupSpawner) {
                    powerupsForCleanup.add(powerupSpawner);
                } else if (trigger instanceof CTFFlag flag) {
                    flag.cleanup();
                } else if (trigger instanceof Spawner spawner)
                    spawner.respawn();
            }
        }

        for (PowerupSpawner powerupSpawner : powerupsForCleanup) {
            powerupSpawner.matchCleanup();
        }

        for (Chunk chunk : this.getChunks()) {
            chunk.setForceLoaded(false);
            chunk.unload();
        }
    }
}
