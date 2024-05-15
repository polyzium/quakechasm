package ru.darkchronics.quake.matchmaking.factory;

import ru.darkchronics.quake.matchmaking.matches.Match;
import ru.darkchronics.quake.matchmaking.map.QMap;

public interface MatchFactory {
    Match createMatch(QMap map);
    String getName();
}
