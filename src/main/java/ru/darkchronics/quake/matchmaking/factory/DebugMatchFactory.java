package ru.darkchronics.quake.matchmaking.factory;

import ru.darkchronics.quake.matchmaking.matches.DebugMatch;
import ru.darkchronics.quake.matchmaking.matches.Match;
import ru.darkchronics.quake.matchmaking.map.QMap;

public class DebugMatchFactory implements MatchFactory {
    @Override
    public Match createMatch(QMap map) {
        return new DebugMatch(map);
    }

    @Override
    public String getName() {
        return DebugMatch.getNameStatic();
    }
}
