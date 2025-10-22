/*
 * Quakechasm, a Quake minigame plugin for Minecraft servers running PaperMC
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

package com.github.polyzium.quakechasm.matchmaking.matches;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import com.github.polyzium.quakechasm.game.combat.DamageCause;
import com.github.polyzium.quakechasm.matchmaking.Team;
import com.github.polyzium.quakechasm.matchmaking.map.QMap;
import com.github.polyzium.quakechasm.misc.TranslationManager;

import java.util.List;

public class DebugMatch extends Match {
    public DebugMatch(QMap map) {
        super(map);
    }

    @Override
    public void join(Player player, Team team) {
        super.join(player, team);

        player.sendMessage(TranslationManager.t("match.debug.disclaimer", player).color(TextColor.color(0xff0000)));
    }

    @Override
    public Team assignTeam(Player player) {
        return Team.FREE;
    }

    public static String getNameStatic() {
        return "GENERIC_DEBUG";
    }
    public String getNameKey() {
        return getNameStatic();
    }

    @Override
    public void setScoreLimit(int scoreLimit) {
        // no-op
    }

    @Override
    public void setNeedPlayers(int needPlayers) {
        // no-op
    }

    @Override
    public void onDeath(Player victim, Entity attacker, DamageCause cause) {
        this.sendMessage(Component.text("onDeath(...) called"));
        this.sendMessage(String.valueOf(victim));
        this.sendMessage(String.valueOf(attacker));
        this.sendMessage(String.valueOf(cause));
    }

    @Override
    public List<Team> allowedTeams() {
        return List.of(Team.FREE);
    }
}
