package ru.darkchronics.quake.matchmaking.factory;

import ru.darkchronics.quake.matchmaking.CTFMatch;
import ru.darkchronics.quake.matchmaking.Match;
import ru.darkchronics.quake.matchmaking.TDMMatch;
import ru.darkchronics.quake.matchmaking.map.QMap;

public class CTFMatchFactory implements MatchFactory {
    @Override
    public Match createMatch(QMap map) {
        return new CTFMatch(map);
    }

    @Override
    public String getName() {
        return CTFMatch.getNameStatic();
    }
}
