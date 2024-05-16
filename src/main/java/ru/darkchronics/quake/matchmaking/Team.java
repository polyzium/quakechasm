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
