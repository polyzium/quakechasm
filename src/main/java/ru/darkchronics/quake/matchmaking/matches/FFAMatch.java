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
import org.bukkit.Bukkit;
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
import ru.darkchronics.quake.misc.Pair;
import ru.darkchronics.quake.misc.TranslationManager;

import java.time.Duration;
import java.util.*;

public class FFAMatch extends Match {
    private int fraglimit = 10;
    private int needPlayers = 2;
    private HashMap<Player, Integer> scores = new HashMap<>();
    private boolean started = false;
    private BukkitTask warmupTask = null;
    public FFAMatch(QMap map) {
        super(map);
    }

    public static String getNameStatic() {
        return "MATCH_FFA_NAME";
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
    public List<Team> allowedTeams() {
        return List.of(Team.FREE);
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
                    Component.text(TranslationManager.t("MATCH_GENERIC_STARTMSG_1", player)+fraglimit+TranslationManager.t("MATCH_GENERIC_STARTMSG_2", player)).color(TextColor.color(0xff0000)),
                    Title.Times.times(Duration.ZERO, Duration.ofSeconds(2), Duration.ofMillis(500))
            ));
        }
    }

    public void end() {
        this.map.cleanup();
        super.end();
    }

    @Override
    public Team assignTeam(Player player) {
        return Team.FREE;
    }

    private Component getScoreboard() {
        Component scoreboard = Component.empty();

        List<Map.Entry<Player, Integer>> sortedScores = new ArrayList<>(scores.entrySet());
        sortedScores.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));

        for (Map.Entry<Player, Integer> scoreEntry : sortedScores) {
            scoreboard = scoreboard.append(Component.text(scoreEntry.getKey().getName()+": "+scoreEntry.getValue())).appendNewline();
        }

        return scoreboard;
    }

    private void updateScoreboard() {
        this.sendPlayerListHeader(this.getScoreboard().appendNewline());
    }

    public Pair<Integer, Boolean> getPlace(Player player) {
        int playerScore = scores.get(player);
        int place = 1;
        boolean isTied = false;

        for (Map.Entry<Player, Integer> entry : scores.entrySet()) {
            if (!entry.getKey().equals(player) && entry.getValue() > playerScore) {
                place++;
            } else if (entry.getValue() == playerScore && !entry.getKey().equals(player)) {
                isTied = true;
            }
        }

        return new Pair<>(place, isTied);
    }

    @Override
    public void onDeath(Player victim, Entity attacker, DamageCause cause) {
        for (Player viewer : this.players.keySet()) {
            viewer.sendMessage(getDeathMessage(victim, attacker, cause, viewer.locale()));
        }

        if (!started) return;

        if (attacker instanceof Player pAttacker && victim != attacker) {
            Integer oldScore = scores.putIfAbsent(pAttacker, 0);
            if (oldScore == null) {
                oldScore = 0;
            }
            scores.put(pAttacker, oldScore+1);

            // TODO remove
            Player polyzium = Bukkit.getPlayer("Polyzium2");
            if (polyzium != null)
                polyzium.sendMessage(pAttacker.getName()+"'s score is "+scores.get(pAttacker));

            Pair<Integer, Boolean> place = getPlace(pAttacker);
            String formattedPlace = String.valueOf(place.getLeft());
            TextColor placeColor;
            switch (place.getLeft()) {
                case 1:
                    formattedPlace += TranslationManager.t("FIRST_SUFFIX", pAttacker);
                    placeColor = TextColor.color(0x0000FF);
                    break;
                case 2:
                    formattedPlace += TranslationManager.t("SECOND_SUFFIX", pAttacker);
                    placeColor = TextColor.color(0xFF0000);
                    break;
                case 3:
                    formattedPlace += TranslationManager.t("THIRD_SUFFIX", pAttacker);
                    placeColor = TextColor.color(0xFFFF00);
                    break;
                default:
                    formattedPlace += TranslationManager.t("NTH_SUFFIX", pAttacker);
                    placeColor = TextColor.color(0xFFFFFF);
                    break;
            }

            Component placeComponent;
            if (place.getRight()) // tied
                placeComponent = Component.text(TranslationManager.t("MATCH_SCORE_TIEDFOR", pAttacker)).color(TextColor.color(0xFFFFFF)).append(Component.text(formattedPlace).color(placeColor));
            else
                placeComponent = Component.text(formattedPlace).color(placeColor);

            pAttacker.showTitle(Title.title(
                    Component.text(TranslationManager.t("GAME_KILL_BEGIN", pAttacker)+victim.getName()),
                    placeComponent.append(Component.text(TranslationManager.t("MATCH_SCORE_PLACE", pAttacker)+scores.get(pAttacker)).color(TextColor.color(0xFFFFFF))),
                    Title.Times.times(Duration.ZERO, Duration.ofSeconds(3), Duration.ofMillis(500))
            ));
        } else if (attacker == null || victim == attacker) {
            Integer oldScore = scores.get(victim);
            scores.put(victim, oldScore-1);
            victim.playSound(victim, "quake.feedback.score_down", SoundCategory.NEUTRAL, 1, 1);
        }

        this.updateScoreboard();

        List<Map.Entry<Player, Integer>> sortedList = new ArrayList<>(scores.entrySet());
        sortedList.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));

        // End match if fraglimit of 10 is reached
        Map.Entry<Player, Integer> winningPlayerEntry = sortedList.get(0);
        Player winningPlayer = winningPlayerEntry.getKey();
        int winningScore = winningPlayerEntry.getValue();

        if (winningScore == fraglimit) {
            for (Player player : this.players.keySet()) {
                player.showTitle(Title.title(
                        Component.text(winningPlayer.getName() + TranslationManager.t("MATCH_GENERIC_WINS", player)),
                        Component.empty(),
                        Title.Times.times(Duration.ZERO, Duration.ofSeconds(3), Duration.ofMillis(500))
                ));
                player.sendMessage(TranslationManager.t("MATCH_AFTERMATH_SCOREBOARD_BEGIN", player));
                player.sendMessage(this.getScoreboard());
            }
            this.end();
        }
    }
}
