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

package ru.darkchronics.quake.matchmaking.matches;

import org.apache.commons.lang3.exception.ExceptionUtils;
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
