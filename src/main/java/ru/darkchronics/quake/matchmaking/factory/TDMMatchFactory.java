package ru.darkchronics.quake.matchmaking.factory;

import ru.darkchronics.quake.matchmaking.FFAMatch;
import ru.darkchronics.quake.matchmaking.Match;
import ru.darkchronics.quake.matchmaking.TDMMatch;
import ru.darkchronics.quake.matchmaking.map.QMap;

public class TDMMatchFactory implements MatchFactory {
    @Override
    public Match createMatch(QMap map) {
        return new TDMMatch(map);
    }

    @Override
    public String getName() {
        return TDMMatch.getNameStatic();
    }
}
