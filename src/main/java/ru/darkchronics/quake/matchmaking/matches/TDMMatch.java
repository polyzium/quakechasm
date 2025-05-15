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

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import ru.darkchronics.quake.QuakePlugin;
import ru.darkchronics.quake.QuakeUserState;
import ru.darkchronics.quake.game.combat.DamageCause;
import ru.darkchronics.quake.matchmaking.Team;
import ru.darkchronics.quake.matchmaking.map.QMap;
import ru.darkchronics.quake.misc.TranslationManager;

import java.time.Duration;
import java.util.*;

public class TDMMatch extends Match {
    private int fraglimit = 10;
    private int needPlayers = 2;
    @SuppressWarnings("FieldMayBeFinal")
    private HashMap<Player, Integer> scores = new HashMap<>();
    @SuppressWarnings("FieldMayBeFinal")
    private int[] teamScores = {0,0};
    private boolean started = false;
    private BukkitTask warmupTask = null;
    public TDMMatch(QMap map) {
        super(map);
    }

    public static String getNameStatic() {
        return "MATCH_TDM_NAME";
    }
    public String getName() {
        return getNameStatic();
    }

    @Override
    public void setScoreLimit(int scoreLimit) {
        this.fraglimit = scoreLimit;
    }

    @Override
    public void setNeedPlayers(int needPlayers) {
        this.needPlayers = needPlayers;
    }

    @Override
    public void join(Player player, Team team) {
        super.join(player, team);
        scores.put(player, 0);

        Team playerTeam = this.players.get(player);
        this.sendMessage(
                player.displayName()
                        .append(Component.text(" has joined the "))
                        .append(Component.text(playerTeam.name()).color(TextColor.color(Team.Colors.get(playerTeam))))
                        .append(Component.text(" team."))
        );

        this.updateScoreboard();

        //noinspection SizeReplaceableByIsEmpty
        if (players.size() >= needPlayers && this.warmupTask == null && !started) {
            warmup();
        }
    }

    @Override
    public void leave(Player player) {
        super.leave(player);
        scores.remove(player);
        this.updateScoreboard();
        
        if (players.isEmpty()) {
            QuakePlugin.INSTANCE.getLogger().warning("Last player of match "+this.getName()+", "+map.name+" has left. Ending match.");
            this.end();
        }
    }

    @Override
    public Team assignTeam(Player player) {
        int redAmount = this.getPlayersInTeam(Team.RED).size();
        int blueAmount = this.getPlayersInTeam(Team.BLUE).size();
        if (redAmount >= blueAmount)
            return Team.BLUE;
        else
            return Team.RED;
    }

    public void warmup() {
        Set<Player> players = this.players.keySet();
        this.warmupTask = new BukkitRunnable() {
            int count = 10;
            @Override
            public void run() {
                for (Player player : players) {
                    player.showTitle(Title.title(
                            Component.text(TranslationManager.t(getName(), player)),
                            Component.text(TranslationManager.t("MATCH_COUNTDOWN", player) + count),
                            Title.Times.times(Duration.ZERO, Duration.ofMillis(1200), Duration.ZERO)
                    ));
                }

                count--;

                if (count == -1) {
                    start();
                    cancel();
                }
            }
        }.runTaskTimer(QuakePlugin.INSTANCE, 0, 20);
    }

    public void start() {
        map.prepareForMatch(this);

        for (Player player : players.keySet()) {
            scores.put(player, 0);

            QuakeUserState userState = QuakePlugin.INSTANCE.userStates.get(player);
            userState.reset();
            userState.respawn();
        }

        started = true;

        this.updateScoreboard();
        for (Player player : this.players.keySet()) {
            player.showTitle(Title.title(
                    Component.text(TranslationManager.t("MATCH_START", player)),
                    Component.text("Get "+fraglimit+" frags").color(TextColor.color(0xff0000)),
                    Title.Times.times(Duration.ZERO, Duration.ofSeconds(2), Duration.ofMillis(500))
            ));
        }
    }

    public void end() {
        this.map.cleanup();
        super.end();
    }

    private Component getScoreboard() {
        Component scoreboard = Component.empty();

        List<Map.Entry<Player, Integer>> sortedScores = new ArrayList<>(scores.entrySet());
        sortedScores.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));

        // Red team
        scoreboard = scoreboard.append(Component.text("Red team: " + teamScores[0]).color(TextColor.color(Team.Colors.get(Team.RED)))).appendNewline();
        for (Map.Entry<Player, Integer> scoreEntry : sortedScores) {
            if (players.get(scoreEntry.getKey()) != Team.RED) continue;
            scoreboard = scoreboard.append(
                    Component.text(scoreEntry.getKey().getName()+": "+scoreEntry.getValue())
                            .color(TextColor.color(0xffffff))
            ).appendNewline();
        }

        scoreboard = scoreboard.appendNewline();

        // Blue team
        scoreboard = scoreboard.append(Component.text("Blue team: " + teamScores[1]).color(TextColor.color(Team.Colors.get(Team.BLUE)))).appendNewline();
        for (Map.Entry<Player, Integer> scoreEntry : sortedScores) {
            if (players.get(scoreEntry.getKey()) != Team.BLUE) continue;
            scoreboard = scoreboard.append(
                    Component.text(scoreEntry.getKey().getName()+": "+scoreEntry.getValue())
                            .color(TextColor.color(0xffffff))
            ).appendNewline();
        }

        return scoreboard;
    }

    private void updateScoreboard() {
        this.sendPlayerListHeader(this.getScoreboard().appendNewline());
    }

    @Override
    public void onDeath(Player victim, Entity attacker, DamageCause cause) {
        for (Player viewer : this.players.keySet()) {
            viewer.sendMessage(getDeathMessage(victim, attacker, cause, viewer.locale()));
        }

        if (!started) return;

        if (attacker instanceof Player pAttacker && victim != attacker) {
            boolean sameTeam = getPlayersInTeam(players.get(pAttacker)).contains(victim);

            Integer oldScore = scores.putIfAbsent(pAttacker, 0);
            if (oldScore == null) {
                oldScore = 0;
            }

            if (!sameTeam) {
                scores.put(pAttacker, oldScore+1);
                switch (players.get(pAttacker)) {
                    case RED -> teamScores[0] += 1;
                    case BLUE -> teamScores[1] += 1;
                    default -> throw new IllegalArgumentException("Got a kill not from red or blue teams");
                }
            } else {
                scores.put(pAttacker, oldScore-1);
                switch (players.get(pAttacker)) {
                    case RED -> teamScores[0] -= 1;
                    case BLUE -> teamScores[1] -= 1;
                    default -> throw new IllegalArgumentException("Got a teamkill not from red or blue teams");
                }
            }

            pAttacker.showTitle(Title.title(
                    Component.text(TranslationManager.t("GAME_KILL_BEGIN", pAttacker)+victim.getName()),
                    Component.empty(),
                    Title.Times.times(Duration.ZERO, Duration.ofSeconds(3), Duration.ofMillis(500))
            ));
        } else if (attacker == null || victim == attacker) {
            Integer oldScore = scores.get(victim);
            scores.put(victim, oldScore-1);
            victim.playSound(victim, "quake.feedback.score_down", SoundCategory.NEUTRAL, 1, 1);
        }

        this.updateScoreboard();

        // End match if fraglimit is reached
        Component winningTeam = Component.text("(unknown)");
        if (teamScores[0] > teamScores[1]) // red leads
            winningTeam = Component.text("RED").color(TextColor.color(Team.Colors.get(Team.RED)));
        else if (teamScores[1] > teamScores[0]) // blue leads
            winningTeam = Component.text("BLUE").color(TextColor.color(Team.Colors.get(Team.BLUE)));

        if (teamScores[0] == fraglimit || teamScores[1] == fraglimit) { // red or blue hits the fraglimit
            for (Player player : this.players.keySet()) {
                player.showTitle(Title.title(
                        Component.text(TranslationManager.t("MATCH_TEAM_WINS_BEGIN", player))
                                .append(winningTeam)
                                .append(Component.text(TranslationManager.t("MATCH_GENERIC_WINS", player))),
                        Component.empty(),
                        Title.Times.times(Duration.ZERO, Duration.ofSeconds(3), Duration.ofMillis(500))
                ));
                player.sendMessage(TranslationManager.t("MATCH_AFTERMATH_SCOREBOARD_BEGIN", player));
                player.sendMessage(this.getScoreboard());
            }
            this.end();
        }
    }

    @Override
    public List<Team> allowedTeams() {
        return List.of(Team.RED, Team.BLUE);
    }
}
