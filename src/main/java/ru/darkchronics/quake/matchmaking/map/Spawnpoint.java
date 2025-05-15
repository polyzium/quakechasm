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
