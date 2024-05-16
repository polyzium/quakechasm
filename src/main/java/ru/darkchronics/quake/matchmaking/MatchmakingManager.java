package ru.darkchronics.quake.matchmaking;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
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

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MatchmakingManager {
    public MatchmakingManager() {
        INSTANCE = this;
    }

    public static class PendingPlayer {
        Player player;
        MatchMode matchMode;
        ArrayList<String> selectedMaps;
        private boolean isAcceptingMatch;
        BukkitTask countdownTask;

        public PendingPlayer(List<String> selectedMaps, MatchMode matchMode, Player player) {
            this.selectedMaps = new ArrayList<>(selectedMaps);
            this.matchMode = matchMode;
            this.player = player;
        }
    }

    public static class PendingMatch {
        QMap map;
        MatchMode matchMode;
        ArrayList<Player> acceptedPlayers = new ArrayList<>(PLAYERS_REQUIRED);

        public PendingMatch(QMap map, MatchMode matchMode) {
            this.matchMode = matchMode;
            this.map = map;
        }

        public void accept(Player player) {
            acceptedPlayers.add(player);
            INSTANCE.findPendingPlayer(player).countdownTask.cancel();
            QuakeUserState userState = QuakePlugin.INSTANCE.userStates.get(player);
            userState.mmState.currentPendingMatch = null;
            userState.mmState.acceptedPendingMatch = this;
            player.sendMessage("Waiting for other players...");

            for (Player acceptedPlayer : acceptedPlayers) {
                acceptedPlayer.showTitle(Title.title(
                        Component.text(acceptedPlayers.size() +"/"+PLAYERS_REQUIRED),
                        Component.empty(),
                        Title.Times.times(Duration.ZERO, Duration.ofMillis(500), Duration.ofMillis(500))
                ));
            }

            if (acceptedPlayers.size() == PLAYERS_REQUIRED) {
                MatchFactory matchFactory = switch (this.matchMode) {
                    case FFA -> new FFAMatchFactory();
                    case TDM -> new TDMMatchFactory();
                    case CTF -> new CTFMatchFactory();
                };

                Match match = MatchManager.INSTANCE.newMatch(matchFactory, this.map);
                if (match == null) {
                    QuakePlugin.INSTANCE.getLogger().severe("Unable to create a "+matchFactory.getName()+" match. Please contact the plugin developers.");
                    for (Player acceptedPlayer : acceptedPlayers) {
                        acceptedPlayer.sendMessage("§cWe are sorry, something has gone terribly wrong on the matchmaking side. Please contact the server admins.");
                        INSTANCE.stopSearching(acceptedPlayer);
                        acceptedPlayer.sendMessage("§cYour match search has been canceled to prevent further errors.");
                    }
                    return;
                }

                match.setNeedPlayers(PLAYERS_REQUIRED);
                for (Player acceptedPlayer : acceptedPlayers) {
                    INSTANCE.stopSearching(acceptedPlayer);
                    match.join(acceptedPlayer);

                    QuakeUserState acceptedPlayerUserState = QuakePlugin.INSTANCE.userStates.get(acceptedPlayer);
                    acceptedPlayerUserState.mmState.acceptedPendingMatch = null;
                }
            }
        }

        public void cancel() {
            for (Player acceptedPlayer : acceptedPlayers) {
                QuakeUserState userState = QuakePlugin.INSTANCE.userStates.get(acceptedPlayer);
                userState.mmState.acceptedPendingMatch = null;
                INSTANCE.findPendingPlayer(acceptedPlayer)
                        .isAcceptingMatch = false;

                acceptedPlayer.sendMessage("§eOne or more of the players did not accept the match. Search continues.");
            }
        }
    }

    public static MatchmakingManager INSTANCE = null;
    public static final int PLAYERS_REQUIRED = 2;
    private ArrayList<PendingPlayer> pendingPlayers = new ArrayList<>();

    private boolean hasMapOverlap(ArrayList<String> maps1, ArrayList<String> maps2) {
        for (String map : maps1) {
            if (maps2.contains(map)) {
                return true;
            }
        }
        return false;
    }

    public PendingPlayer findPendingPlayer(Player player) {
        Optional<PendingPlayer> pendingPlayer = this.pendingPlayers.stream()
                .filter(scanPendingPlayer -> scanPendingPlayer.player == player)
                .findAny();

        return pendingPlayer.orElse(null);
    }

    public void startSearching(List<String> selectedMaps, MatchMode matchMode, Player player) {
        boolean mapsUnfit = false;
        for (String mapName : selectedMaps) {
            QMap map = QuakePlugin.INSTANCE.getMap(mapName);
            if (!map.recommendedModes.contains(matchMode)) {
                mapsUnfit = true;
                player.sendMessage("§cMap "+mapName+" is unfit for "+matchMode.getDisplayName());
            }
        }
        if (mapsUnfit) {
            player.sendMessage("§cTry choosing a different map pool or mode.");
            return;
        }


        PendingPlayer pendingPlayer = new PendingPlayer(selectedMaps, matchMode, player);
        this.pendingPlayers.add(pendingPlayer);
        this.signal(pendingPlayer);

        player.sendMessage("Searching for matches... \nUse \"/quake matchmaking cancel\" to stop searching");
    }

    public boolean stopSearching(Player player) {
        QuakeUserState userState = QuakePlugin.INSTANCE.userStates.get(player);
        userState.mmState.currentPendingMatch = null;

        PendingPlayer pendingSelf = this.findPendingPlayer(player);
        if (pendingSelf != null && pendingSelf.countdownTask != null)
            pendingSelf.countdownTask.cancel();

        return this.pendingPlayers.removeIf(pendingPlayer -> pendingPlayer.player == player);
    }

    private void signal(PendingPlayer signaller) {
        ArrayList<PendingPlayer> matchingPlayers = new ArrayList<>(PLAYERS_REQUIRED);
        ArrayList<String> commonMaps = new ArrayList<>(signaller.selectedMaps);

        for (PendingPlayer pendingPlayer : this.pendingPlayers) {
            if (signaller == pendingPlayer) continue;

            if (
                signaller.matchMode == pendingPlayer.matchMode &&
                        hasMapOverlap(signaller.selectedMaps, pendingPlayer.selectedMaps) &&
                        !pendingPlayer.isAcceptingMatch
            ) {
                matchingPlayers.add(pendingPlayer);
                commonMaps.retainAll(pendingPlayer.selectedMaps);

            }

            // -1 because we don't count the signaller as a matching player.
            if (matchingPlayers.size() == PLAYERS_REQUIRED-1) {
                int mapIndex = (int) Math.round(Math.random()*(commonMaps.size()-1));
                QMap map = QuakePlugin.INSTANCE.getMap(commonMaps.get(mapIndex));

                PendingMatch pendingMatch = new PendingMatch(map, signaller.matchMode);
                onMatchFound(signaller, pendingMatch);
                for (PendingPlayer matchingPlayer : matchingPlayers) {
                    onMatchFound(matchingPlayer, pendingMatch);
                }

                break;
            }
        }

    }

    private void onMatchFound(PendingPlayer pendingPlayer, PendingMatch pendingMatch) {
        pendingPlayer.player.sendMessage("Your match has been found:");
        pendingPlayer.player.sendMessage("§b"+pendingMatch.map.name+" | "+pendingMatch.matchMode.getDisplayName());
        pendingPlayer.player.sendMessage("Type \"/quake matchmaking accept\" to accept the match.");
        pendingPlayer.isAcceptingMatch = true;
        QuakeUserState userState = QuakePlugin.INSTANCE.userStates.get(pendingPlayer.player);
        userState.mmState.currentPendingMatch = pendingMatch;

        pendingPlayer.countdownTask = new BukkitRunnable() {
            int secs = 20;
            @Override
            public void run() {
                secs--;

                pendingPlayer.player.showTitle(Title.title(
                        Component.text("Your game is ready!"),
                        Component.text("You have " + secs + " seconds to accept"),
                        Title.Times.times(Duration.ZERO, Duration.ofMillis(1200), Duration.ZERO)
                ));

                if (secs == 0) {
                    pendingMatch.cancel();
                    pendingPlayer.player.sendMessage("§cYour game was canceled because you did not accept.");
                    INSTANCE.stopSearching(pendingPlayer.player);

                    cancel();
                }
            }
        }.runTaskTimer(QuakePlugin.INSTANCE, 0, 20);
    }
}
