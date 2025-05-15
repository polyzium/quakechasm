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

package ru.darkchronics.quake.matchmaking;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.ForwardingAudience;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import ru.darkchronics.quake.QuakePlugin;
import ru.darkchronics.quake.QuakeUserState;
import ru.darkchronics.quake.matchmaking.factory.CTFMatchFactory;
import ru.darkchronics.quake.matchmaking.factory.FFAMatchFactory;
import ru.darkchronics.quake.matchmaking.factory.MatchFactory;
import ru.darkchronics.quake.matchmaking.factory.TDMMatchFactory;
import ru.darkchronics.quake.matchmaking.map.QMap;
import ru.darkchronics.quake.matchmaking.matches.Match;
import ru.darkchronics.quake.matchmaking.matches.MatchManager;
import ru.darkchronics.quake.matchmaking.matches.MatchMode;
import ru.darkchronics.quake.misc.TranslationManager;

import java.time.Duration;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MatchmakingManager {
    public MatchmakingManager() {
        INSTANCE = this;
    }

    public static class Party implements ForwardingAudience {
        public ArrayList<Player> getPlayers() {
            return players;
        }

        ArrayList<Player> players;
        public Player leader;

        public Party(Player leader) {
            this.players = new ArrayList<>();
            this.players.add(leader);
            this.leader = leader;
        }

        public static Component localizedPrefix(Locale locale) {
            return Component.text(TranslationManager.t("PARTY_PREFIX", locale)).decorate(TextDecoration.BOLD);
        }

        public void addPlayer(Player player) {
            this.players.add(player);
            QuakePlugin.INSTANCE.userStates.get(player)
                    .mmState.currentParty = this;
            this.sendMessage(Component.textOfChildren(localizedPrefix(this.leader.locale()), player.displayName(), Component.text(" joined")));
        }

        public void removePlayer(Player player) {
            this.players.remove(player);
            QuakePlugin.INSTANCE.userStates.get(player)
                    .mmState.currentParty = new Party(player);
            this.sendMessage(Component.textOfChildren(localizedPrefix(this.leader.locale()), player.displayName(), Component.text(" left")));
            if (player == this.leader && !this.players.isEmpty()) {
                leader = this.players.get(0);
                this.sendMessage(Component.textOfChildren(localizedPrefix(this.leader.locale()), this.leader.displayName(), Component.text(" is the new party leader")));
            }
        }

        public int size() {
            return this.players.size();
        }

        @Override
        public @NotNull Iterable<? extends Audience> audiences() {
            return this.players;
        }

        public void sendMessage(String s) {
            this.sendMessage(Component.text(s));
        }
    }

    public static class PendingParty {
        Party party;

        public MatchMode getMatchMode() {
            return matchMode;
        }

        MatchMode matchMode;

        public ArrayList<String> getSelectedMaps() {
            return selectedMaps;
        }

        ArrayList<String> selectedMaps;
        private boolean isAcceptingMatch;
        BukkitTask countdownTask;
        BukkitTask timeTask;
        protected BossBar statusBar;

        public PendingParty(List<String> selectedMaps, MatchMode matchMode, Party party) {
            this.selectedMaps = new ArrayList<>(selectedMaps);
            this.matchMode = matchMode;
            this.party = party;
            this.statusBar = BossBar.bossBar(Component.text(TranslationManager.t("MATCHMAKING_SEARCH_START_STATUSBAR_INIT", party.leader)), 0, BossBar.Color.BLUE, BossBar.Overlay.PROGRESS);
            this.timeTask = new BukkitRunnable() {
                int s = 0;
                @Override
                public void run() {
                    s++;
                    statusBar.name(Component.text(
                            TranslationManager.t("MATCHMAKING_SEARCH_START_STATUSBAR", party.leader)+String.format("%02d:%02d", (s % 3600) / 60, (s % 60))
                    ));
                    statusBar.progress((float) (s % 60)/60);
                }
            }.runTaskTimer(QuakePlugin.INSTANCE, 20, 20);
        }
    }

    public static class PendingMatch {
        QMap map;
        MatchMode matchMode;
        ArrayList<Player> acceptedPlayers;
        ArrayList<Player> team1;
        ArrayList<Player> team2;
        boolean canceled = false;

        public PendingMatch(QMap map, MatchMode matchMode, ArrayList<Player> team1, ArrayList<Player> team2) {
            this.matchMode = matchMode;
            this.map = map;
            this.acceptedPlayers = new ArrayList<>(map.neededPlayers);
            this.team1 = team1;
            this.team2 = team2;

            Logger logger = QuakePlugin.INSTANCE.getLogger();

            logger.info("Players in team1:");
            for (Player player : team1) {
                logger.info(player.getName());
            }

            logger.info("Players in team2:");
            for (Player player : team2) {
                logger.info(player.getName());
            }

        }

        public void accept(Player player) {
            Logger logger = QuakePlugin.INSTANCE.getLogger();

            acceptedPlayers.add(player);
            QuakeUserState userState = QuakePlugin.INSTANCE.userStates.get(player);
            userState.mmState.currentPendingMatch = null;
            userState.mmState.acceptedPendingMatch = this;
            player.sendMessage(TranslationManager.t("MATCHMAKING_MATCH_ACCEPT_WAITING", player));

            for (Player acceptedPlayer : acceptedPlayers) {
                acceptedPlayer.showTitle(Title.title(
                        Component.text(acceptedPlayers.size() +"/"+ map.neededPlayers),
                        Component.empty(),
                        Title.Times.times(Duration.ZERO, Duration.ofSeconds(5), Duration.ofMillis(1000))
                ));
            }

            if (acceptedPlayers.size() == map.neededPlayers) {
                logger.info("acceptedPlayers.size() == map.neededPlayers");
                MatchFactory matchFactory = switch (this.matchMode) {
                    case FFA -> new FFAMatchFactory();
                    case TDM -> new TDMMatchFactory();
                    case CTF -> new CTFMatchFactory();
                };

                logger.info("Creating match");
                Match match = MatchManager.INSTANCE.newMatch(matchFactory, this.map);
                if (match == null) {
                    QuakePlugin.INSTANCE.getLogger().severe("Unable to create a "+matchFactory.getName()+" match. Please contact the plugin developers.");
                    for (Player acceptedPlayer : acceptedPlayers) {
                        acceptedPlayer.sendMessage(TranslationManager.t("ERROR_MATCHMAKING_GENERIC", player));
                        INSTANCE.stopSearching(acceptedPlayer);
                        acceptedPlayer.sendMessage(TranslationManager.t("ERROR_MATCHMAKING_SEARCH_OHSHITITSFUCKED", player));
                    }
                    return;
                }

                logger.info("Setting needed players to "+map.neededPlayers);
                match.setNeedPlayers(map.neededPlayers);

                if (match.isTeamMatch()) {
                    logger.info("Joining team1");
                    for (Player team1Player : team1) {
                        INSTANCE.stopSearching(team1Player);
                        match.join(team1Player, Team.RED);

                        QuakeUserState team1PlayerUserState = QuakePlugin.INSTANCE.userStates.get(team1Player);
                        team1PlayerUserState.mmState.acceptedPendingMatch = null;
                    }

                    logger.info("Joining team2");
                    for (Player team2Player : team2) {
                        INSTANCE.stopSearching(team2Player);
                        match.join(team2Player, Team.BLUE);

                        QuakeUserState team2PlayerUserState = QuakePlugin.INSTANCE.userStates.get(team2Player);
                        team2PlayerUserState.mmState.acceptedPendingMatch = null;
                    }
                } else {
                    for (Player freePlayer : Stream.concat(team1.stream(), team2.stream()).toList()) {
                        INSTANCE.stopSearching(freePlayer);
                        match.join(freePlayer, Team.FREE);

                        QuakeUserState team1PlayerUserState = QuakePlugin.INSTANCE.userStates.get(freePlayer);
                        team1PlayerUserState.mmState.acceptedPendingMatch = null;
                    }
                }
            }
        }

        public void cancel() {
            if (this.canceled) return;

            for (Player acceptedPlayer : acceptedPlayers) {
                QuakeUserState userState = QuakePlugin.INSTANCE.userStates.get(acceptedPlayer);
                userState.mmState.acceptedPendingMatch = null;
                INSTANCE.findPendingParty(acceptedPlayer)
                        .isAcceptingMatch = false;

                acceptedPlayer.sendMessage(TranslationManager.t("WARNING_MATCHMAKING_CONTINUESEARCH", acceptedPlayer));
            }

            canceled = true;
        }
    }

    public static MatchmakingManager INSTANCE = null;
    private ArrayList<PendingParty> pendingParties = new ArrayList<>();

    private boolean hasMapOverlap(ArrayList<String> maps1, ArrayList<String> maps2) {
        for (String map : maps1) {
            if (maps2.contains(map)) {
                return true;
            }
        }
        return false;
    }

    public PendingParty findPendingParty(Player player) {
        Optional<PendingParty> pendingParty = this.pendingParties.stream()
                .filter(scanPendingParty -> scanPendingParty.party.players.contains(player))
                .findAny();

        return pendingParty.orElse(null);
    }

    public void startSearching(List<String> selectedMaps, MatchMode matchMode, Party party) {
        boolean mapsUnfit = false;
        for (String mapName : selectedMaps) {
            QMap map = QuakePlugin.INSTANCE.getMap(mapName);
            if (!map.recommendedModes.contains(matchMode)) {
                mapsUnfit = true;
                party.sendMessage("§cMap "+mapName+" is unfit for "+matchMode.getDisplayName());
            }
        }
        if (mapsUnfit) {
            for (Player player : party.players) {
                player.sendMessage(TranslationManager.t("ERROR_MATCHMAKING_MAP_UNFIT_3", player));
            }
            return;
        }

        QMap map = QuakePlugin.INSTANCE.getMap(selectedMaps.get(0));
        int playersPerTeam = map.neededPlayers / 2;
        if (playersPerTeam < party.size()) {
            for (Player player : party.players) {
                party.sendMessage(
                        TranslationManager.t("ERROR_PARTY_TOOBIG_1", player)+
                                party.size()+
                                TranslationManager.t("ERROR_PARTY_TOOBIG_2", player)+
                                playersPerTeam+
                                TranslationManager.t("ERROR_PARTY_TOOBIG_3", player)+
                                map.displayName+"\". "+
                                TranslationManager.t("ERROR_PARTY_TOOBIG_4", player)
                );
            }
            return;
        }

        PendingParty pendingParty = new PendingParty(selectedMaps, matchMode, party);
        pendingParty.party.showBossBar(pendingParty.statusBar);
        this.pendingParties.add(pendingParty);
        this.signal(pendingParty);

        for (Player partyPlayer : party.players) {
            if (partyPlayer == party.leader)
//                partyPlayer.sendMessage("Searching for matches... \nUse \"/quake matchmaking cancel\" to stop searching");
                partyPlayer.sendMessage(TranslationManager.t("MATCHMAKING_SEARCH_START", partyPlayer));
            else
//                partyPlayer.sendMessage(Component.textOfChildren(Party.localizedPrefix(), party.leader.displayName(),
//                        Component.text(" started search for matches: mode "+matchMode.toString()+", maps "+String.join(", ", selectedMaps)))
                partyPlayer.sendMessage(Component.textOfChildren(Party.localizedPrefix(partyPlayer.locale()), party.leader.displayName(),
                        Component.text(" started search for matches: mode "+matchMode.toString()+", maps "+String.join(", ", selectedMaps)))
                );
        }
    }

    public boolean stopSearching(Player player) {
        PendingParty pendingSelf = this.findPendingParty(player);
        if (pendingSelf == null) return false;
        for (Player partyPlayer : pendingSelf.party.players) {
            QuakeUserState userState = QuakePlugin.INSTANCE.userStates.get(partyPlayer);
            userState.mmState.currentPendingMatch = null;
        }

        if (pendingSelf.countdownTask != null) pendingSelf.countdownTask.cancel();
        pendingSelf.timeTask.cancel();
        pendingSelf.party.hideBossBar(pendingSelf.statusBar);

        for (Player partyPlayer : pendingSelf.party.players) {
            if (partyPlayer == pendingSelf.party.leader) continue;
            partyPlayer.sendMessage(Component.textOfChildren(Party.localizedPrefix(partyPlayer.locale()), pendingSelf.party.leader.displayName(),
                    Component.text(TranslationManager.t("PARTY_MATCHMAKING_CANCELED", partyPlayer))
            ));
        }

        return this.pendingParties.removeIf(pendingParty -> pendingParty.party.players.contains(player));
    }

    private void signal(PendingParty signaller) {
        Logger logger = QuakePlugin.INSTANCE.getLogger();
        QMap queuedMap = QuakePlugin.INSTANCE.getMap(signaller.selectedMaps.get(0));
        int playersPerTeam = queuedMap.neededPlayers/2;

        ArrayList<PendingParty> matchingParties = new ArrayList<>();
        ArrayList<String> commonMaps = new ArrayList<>(signaller.selectedMaps);

        for (PendingParty pendingParty : this.pendingParties) {
            if (signaller == pendingParty) continue;
            logger.info("signaller != pendingParty, continuing");

            if (
                signaller.matchMode == pendingParty.matchMode &&
                        hasMapOverlap(signaller.selectedMaps, pendingParty.selectedMaps) &&
                        !pendingParty.isAcceptingMatch
            ) {
                logger.info("Found matching party");
                matchingParties.add(pendingParty);
                commonMaps.retainAll(pendingParty.selectedMaps);
            }

            int totalPlayers = signaller.party.size();
            for (PendingParty matchingParty : matchingParties) {
                totalPlayers += matchingParty.party.size();
                logger.info("totalPlayers = "+totalPlayers);
            }

            logger.info("playersPerTeam*2 = "+playersPerTeam*2);
            if (totalPlayers == playersPerTeam*2) {
                logger.info("Enough players found, initiating match");
                ArrayList<Party> usedParties = new ArrayList<>(5);
                // sort in ascending order
                matchingParties.sort((lhs, rhs) -> {
                    if (lhs.party.size() < rhs.party.size()) return -1;
                    if (lhs.party.size() == rhs.party.size()) return 0;
                    if (lhs.party.size() > rhs.party.size()) return 1;
                    // unreachable
                    return 0;
                });
                logger.info("Sorted matchingParties in ascending order");

                // Construct teams
                logger.info("Constructing team1");
                ArrayList<Player> team1 = new ArrayList<>(playersPerTeam);
                team1.addAll(signaller.party.players);
                usedParties.add(signaller.party);
                for (PendingParty matchingParty : matchingParties) {
                    if (team1.size() < playersPerTeam && team1.size()+matchingParty.party.size() <= playersPerTeam) {
                        logger.info("team1.size() < playersPerTeam && team1.size()+matchingParty.party.size() <= playersPerTeam");
                        team1.addAll(matchingParty.party.players);
                        usedParties.add(matchingParty.party);
                    }
                    if (team1.size() == playersPerTeam) break;
                }
                logger.info("team1 constructed");

                logger.info("Constructing team2");
                ArrayList<Player> team2 = new ArrayList<>(playersPerTeam);
                for (PendingParty matchingParty : matchingParties) {
                    if (usedParties.contains(matchingParty.party)) continue;
                    logger.info("team2: Not a used party, continuing");

                    if (team2.size() < playersPerTeam && team2.size()+matchingParty.party.size() <= playersPerTeam) {
                        logger.info("team1.size() < playersPerTeam && team1.size()+matchingParty.party.size() <= playersPerTeam");
                        team2.addAll(matchingParty.party.players);
                        usedParties.add(matchingParty.party);
                    }
                    if (team2.size() == playersPerTeam) break;
                }
                logger.info("team2 constructed");

                logger.info("Selecting map");
                List<String> freeMaps = commonMaps.stream()
                        .filter(s -> QuakePlugin.INSTANCE.getMap(s).getMatch() == null)
                        .toList();
                if (freeMaps.isEmpty()) return;
                int mapIndex = (int) Math.round(Math.random()*(freeMaps.size()-1));
                QMap map = QuakePlugin.INSTANCE.getMap(freeMaps.get(mapIndex));

                logger.info("Making pendingMatch and sending onMatchFound to matching parties");
                PendingMatch pendingMatch = new PendingMatch(map, signaller.matchMode, team1, team2);
                onMatchFound(signaller, pendingMatch);
                for (PendingParty matchingParty : matchingParties) {
                    onMatchFound(matchingParty, pendingMatch);
                }

                break;
            }
        }

    }

    private void onMatchFound(PendingParty pendingParty, PendingMatch pendingMatch) {
        pendingParty.party.sendMessage(TranslationManager.t("MATCHMAKING_MATCH_ACCEPT_CHAT_1", pendingParty.party.leader));
        pendingParty.party.sendMessage("§b"+pendingMatch.map.displayName+" | "+pendingMatch.matchMode.getDisplayName());
        pendingParty.party.sendMessage("Type \"/quake matchmaking accept\" to accept the match.");
        pendingParty.isAcceptingMatch = true;
        for (Player partyPlayer : pendingParty.party.players) {
            QuakeUserState userState = QuakePlugin.INSTANCE.userStates.get(partyPlayer);
            userState.mmState.currentPendingMatch = pendingMatch;
        }

        pendingParty.countdownTask = new BukkitRunnable() {
            int secs = 20;
            @Override
            public void run() {
                secs--;

                for (Player partyPlayer : pendingParty.party.players) {
                    if (pendingMatch.acceptedPlayers.contains(partyPlayer)) continue;

                    partyPlayer.showTitle(Title.title(
                            Component.text(TranslationManager.t("MATCHMAKING_MATCH_ACCEPT_1", partyPlayer)),
                            Component.text(TranslationManager.t("MATCHMAKING_MATCH_ACCEPT_2", partyPlayer) + secs + TranslationManager.t("MATCHMAKING_MATCH_ACCEPT_3", partyPlayer)),
                            Title.Times.times(Duration.ZERO, Duration.ofMillis(1200), Duration.ZERO)
                    ));
                }

                if (secs == 0) {
                    pendingMatch.cancel();
                    for (Player partyPlayer : pendingParty.party.players) {
                        if (pendingMatch.acceptedPlayers.contains(partyPlayer)) continue;
                        partyPlayer.sendMessage(TranslationManager.t("ERROR_MATCHMAKING_CANCELEDMATCHNOACCEPT", pendingParty.party.leader));
                    }
                    INSTANCE.stopSearching(pendingParty.party.leader);
                    pendingParty.party.sendMessage(Component.textOfChildren(
                            Party.localizedPrefix(pendingParty.party.leader.locale()), Component.text(TranslationManager.t("PARTY_MATCHMAKING_SEARCH_LEADERDIDNTACCEPT", pendingParty.party.leader))
                    ));

                    cancel();
                }
            }
        }.runTaskTimer(QuakePlugin.INSTANCE, 0, 20);
    }
}
