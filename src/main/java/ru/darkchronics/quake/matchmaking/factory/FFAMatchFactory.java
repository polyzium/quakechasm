package ru.darkchronics.quake.matchmaking.factory;

import ru.darkchronics.quake.matchmaking.DebugMatch;
import ru.darkchronics.quake.matchmaking.FFAMatch;
import ru.darkchronics.quake.matchmaking.Match;
import ru.darkchronics.quake.matchmaking.map.QMap;

public class FFAMatchFactory implements MatchFactory {
    @Override
    public Match createMatch(QMap map) {
        return new FFAMatch(map);
    }

    @Override
    public String getName() {
        return FFAMatch.getNameStatic();
    }
}
