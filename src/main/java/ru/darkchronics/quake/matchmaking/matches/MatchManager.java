package ru.darkchronics.quake.matchmaking.matches;

import org.apache.commons.lang.exception.ExceptionUtils;
import ru.darkchronics.quake.QuakePlugin;
import ru.darkchronics.quake.matchmaking.factory.MatchFactory;
import ru.darkchronics.quake.matchmaking.map.QMap;

import java.util.ArrayList;

public class MatchManager {
    public static MatchManager INSTANCE;
    public ArrayList<Match> matches;

    public MatchManager() {
        this.matches = new ArrayList<>(10);

        // singleton
        INSTANCE = this;
    }

    public Match newMatch(MatchFactory matchFactory, QMap map) {
        Match match = null;
        try {
            match = matchFactory.createMatch(map);
        } catch (Exception e) {
            QuakePlugin.INSTANCE.getLogger().severe(
                    "Caught an exception from "+matchFactory.getClass().getSimpleName()+":\n"+
                            ExceptionUtils.getStackTrace(e)
            );
        }
        if (match == null) {
            QuakePlugin.INSTANCE.getLogger().severe("Failed to make a "+matchFactory.getName()+" match");
            return null;
        }

        matches.add(match);
        return match;
    }
}
