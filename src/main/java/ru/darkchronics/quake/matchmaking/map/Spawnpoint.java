package ru.darkchronics.quake.matchmaking.map;

import org.bukkit.Location;
import org.bukkit.util.Vector;
import ru.darkchronics.quake.matchmaking.Team;

import java.util.ArrayList;

public class Spawnpoint {
    public Location pos;
    public ArrayList<Team> allowedTeams;

    public Spawnpoint(Location pos, ArrayList<Team> allowedTeams) {
        this.pos = pos;
        this.allowedTeams = allowedTeams;
    }
}
