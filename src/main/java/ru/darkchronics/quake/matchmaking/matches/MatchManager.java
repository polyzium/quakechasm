package ru.darkchronics.quake.matchmaking.matches;

import org.bukkit.Bukkit;
import ru.darkchronics.quake.matchmaking.factory.MatchFactory;
import ru.darkchronics.quake.matchmaking.map.QMap;

import java.util.ArrayList;

public class MatchManager {
    public static MatchManager INSTANCE;
    public ArrayList<Match> matches;

    public MatchManager() {
        this.matches = new ArrayList<>(2);

        // singleton
        INSTANCE = this;
    }

    public Match newMatch(MatchFactory matchFactory, QMap map) {
        Match match = matchFactory.createMatch(map);
        if (match == null) {
            QuakePlugin.INSTANCE.getLogger().severe("Failed to make a "+matchFactory.getName()+" match");
            return null;
        }

        matches.add(match);
        return match;
    }
}
