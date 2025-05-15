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

package ru.darkchronics.quake.matchmaking;

import java.util.EnumMap;

public enum Team {
    RED,
    BLUE,
    FREE,
    SPECTATOR;

    public static class Colors {
        private static EnumMap<Team, Integer> COLORS;

        static {
            COLORS = new EnumMap<>(Team.class);

            COLORS.put(RED, 0xff3f3f);
            COLORS.put(BLUE, 0x3f3fff);
            COLORS.put(FREE, 0xffff00);
            COLORS.put(SPECTATOR, 0x00ff00);
        }

        public static int get(Team team) {
            return COLORS.get(team);
        }
    }

    public Team oppositeTeam() {
        switch (this) {
            case RED -> {
                return Team.BLUE;
            }
            case BLUE -> {
                return Team.RED;
            }
            default -> throw new IllegalArgumentException("Cannot find opposite of a non-red/non-blue team");
        }
    }
}
