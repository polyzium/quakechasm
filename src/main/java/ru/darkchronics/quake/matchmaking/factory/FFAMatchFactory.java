package ru.darkchronics.quake.matchmaking.factory;

import ru.darkchronics.quake.matchmaking.matches.FFAMatch;
import ru.darkchronics.quake.matchmaking.matches.Match;
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
