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

import com.github.polyzium.quakechasm.misc.TableBuilder;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.title.Title;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import com.github.polyzium.quakechasm.QuakePlugin;
import com.github.polyzium.quakechasm.QuakeUserState;
import com.github.polyzium.quakechasm.game.combat.DamageCause;
import com.github.polyzium.quakechasm.game.entities.Trigger;
import com.github.polyzium.quakechasm.game.entities.pickups.CTFFlag;
import com.github.polyzium.quakechasm.hud.Icons;
import com.github.polyzium.quakechasm.matchmaking.Team;
import com.github.polyzium.quakechasm.matchmaking.map.QMap;
import com.github.polyzium.quakechasm.misc.TranslationManager;

import java.time.Duration;
import java.util.*;

public class CTFMatch extends Match {
    private int capturelimit = 10;
    private int needPlayers = 2;
    @SuppressWarnings("FieldMayBeFinal")
    private HashMap<Player, Integer> scores = new HashMap<>();
    @SuppressWarnings("FieldMayBeFinal")
    private int[] captures = {0,0};
    private Player[] flagCarriers = {null, null};
    private boolean started = false;
    private BukkitTask warmupTask = null;
    private BossBar infoBar;
    public CTFMatch(QMap map) {
        super(map);

        getRedFlag().prepareForMatch(this);
        getBlueFlag().prepareForMatch(this);

        this.infoBar = BossBar.bossBar(Component.empty(), 0, BossBar.Color.WHITE, BossBar.Overlay.PROGRESS);
        this.updateInfo();
    }

    public static String getNameKeyStatic() {
        return "match.ctf.name";
    }
    public String getNameKey() {
        return getNameKeyStatic();
    }

    @Override
    public void setScoreLimit(int scoreLimit) {
        this.capturelimit = scoreLimit;
    }

    @Override
    public void setNeedPlayers(int needPlayers) {
        this.needPlayers = needPlayers;
    }

    @Override
    public void join(Player player, Team team) {
        super.join(player, team);
        scores.put(player, 0);
        player.showBossBar(this.infoBar);

        Team playerTeam = this.players.get(player);
        this.sendMessage(
                player.displayName()
                        .append(Component.text(" has joined the "))
                        .append(Component.text(playerTeam.name()).color(TextColor.color(Team.Colors.get(playerTeam))))
                        .append(Component.text(" team."))
        );

        this.updateScoreboard();

        if (players.size() >= needPlayers && this.warmupTask == null && !started) {
            warmup();
        }
    }

    @Override
    public void leave(Player player) {
        super.leave(player);
        scores.remove(player);
        player.hideBossBar(this.infoBar);
        this.updateScoreboard();

        if (players.isEmpty()) {
            QuakePlugin.INSTANCE.getLogger().warning("Last player of match "+this.getNameKey()+", "+map.name+" has left. Ending match.");
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
                            TranslationManager.t(getNameKey(), player),
                            TranslationManager.t("match.countdown", player, Placeholder.unparsed("count", String.valueOf(count))),
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

        captures = new int[]{0, 0};
        flagCarriers = new Player[]{null, null};

        started = true;

        this.updateScoreboard();
        for (Player player : this.players.keySet()) {
            player.showTitle(Title.title(
                    TranslationManager.t("match.start", player),
                    TranslationManager.t("match.ctf.startMessage", player, Placeholder.unparsed("capturelimit", String.valueOf(capturelimit))).color(TextColor.color(0xff0000)),
                    Title.Times.times(Duration.ZERO, Duration.ofSeconds(2), Duration.ofMillis(500))
            ));
        }
    }

    public void end() {
        for (Player player : this.players.keySet()) {
            player.hideBossBar(this.infoBar);
        }

        this.map.cleanup();
        super.end();
    }

    public CTFFlag getRedFlag() {
        Collection<Entity> entities = map.world.getNearbyEntities(map.bounds);
        for (Trigger trigger : QuakePlugin.INSTANCE.triggers) {
            if (entities.contains(trigger.getEntity())) {
                if (trigger instanceof CTFFlag flag && flag.getTeam() == Team.RED && !flag.isDrop()) {
                    return flag;
                }
            }
        }

        return null;
    }

    public CTFFlag getBlueFlag() {
        Collection<Entity> entities = map.world.getNearbyEntities(map.bounds);
        for (Trigger trigger : QuakePlugin.INSTANCE.triggers) {
            if (entities.contains(trigger.getEntity())) {
                if (trigger instanceof CTFFlag flag && flag.getTeam() == Team.BLUE && !flag.isDrop()) {
                    return flag;
                }
            }
        }

        return null;
    }

    public void grabFlag(Team belongingFlagTeam, Player player) {
        PlayerInventory inv = player.getInventory();

        switch (belongingFlagTeam) {
            case RED -> {
                // Player picks up red flag
                // Assign red flag carrier
                flagCarriers[0] = player;
                inv.setItemInOffHand(new ItemStack(Material.RED_BANNER));
            }
            case BLUE -> {
                // Player picks up blue flag
                // Assign blue flag carrier
                flagCarriers[1] = player;
                inv.setItemInOffHand(new ItemStack(Material.BLUE_BANNER));
            }
            default -> throw new IllegalArgumentException("Non-red/non-blue attempted to pick up the flag");
        }

        for (Player matchPlayer : this.players.keySet()) {
            matchPlayer.sendMessage(
                    TranslationManager.t("match.ctf.flag.taken", matchPlayer,
                            Placeholder.unparsed("player_name", player.getName()),
                            Placeholder.component("flag_color", Component.text(belongingFlagTeam.name()).color(TextColor.color(Team.Colors.get(belongingFlagTeam)))))
            );
            
            // Play sound based on whether the flag taken is opponent's or player's team
            Team playerTeam = this.players.get(matchPlayer);
            if (playerTeam == belongingFlagTeam) {
                // Player's team flag was taken by opponent
                matchPlayer.playSound(matchPlayer, "quake.feedback.teamplay.flagtaken.yourteam", SoundCategory.NEUTRAL, 1, 1);
            } else {
                // Opponent's flag was taken by player's team
                matchPlayer.playSound(matchPlayer, "quake.feedback.teamplay.flagtaken.opponent", SoundCategory.NEUTRAL, 1, 1);
            }
        }

        this.updateInfo();
    }

    public Team getCarryingFlagTeam(Player carrier) {
        if (flagCarriers[0] == carrier)
            return Team.RED;
        else if (flagCarriers[1] == carrier)
            return Team.BLUE;

        return null;
    }

    public void returnFlag(Team team, Player returningPlayer) {
        Collection<Entity> entities = map.world.getNearbyEntities(map.bounds);
        for (Trigger trigger : QuakePlugin.INSTANCE.triggers) {
            if (entities.contains(trigger.getEntity()) && trigger instanceof CTFFlag flag && !flag.isDrop() && flag.getTeam() == team) {
                for (Player player : this.players.keySet()) {
                    if (returningPlayer != null)
                        player.sendMessage(
                                TranslationManager.t("match.ctf.flag.returnedByPlayer", player,
                                        Placeholder.unparsed("player_name", returningPlayer.getName()),
                                        Placeholder.component("flag_color", Component.text(team.name()).color(TextColor.color(Team.Colors.get(team)))))
                        );
                    else
                        player.sendMessage(
                                TranslationManager.t("match.ctf.flag.returnedAuto", player,
                                        Placeholder.component("flag_color", Component.text(team.name()).color(TextColor.color(Team.Colors.get(team)))))
                        );
                    
                    // Play sound based on whether the returned flag is opponent's or player's team
                    Team playerTeam = this.players.get(player);
                    if (playerTeam == team) {
                        // Player's team flag was returned
                        player.playSound(player, "quake.feedback.teamplay.flagreturn.yourteam", SoundCategory.NEUTRAL, 1, 1);
                    } else {
                        // Opponent's flag was returned
                        player.playSound(player, "quake.feedback.teamplay.flagreturn.opponent", SoundCategory.NEUTRAL, 1, 1);
                    }
                }

                flag.respawn();
                break;
            }
        }

        this.updateInfo();
    }

    public void captureFlag(Team belongingFlagTeam, Player enemyFlagCarrier) {
        PlayerInventory inv = enemyFlagCarrier.getInventory();

        switch (belongingFlagTeam) {
            case RED -> {
                // Player carrying a blue flag triggers the red flag pickup
                flagCarriers[1] = null;
                getBlueFlag().respawn();
            }
            case BLUE -> {
                // Player carrying a red flag triggers the blue flag pickup
                flagCarriers[0] = null;
                getRedFlag().respawn();
            }
            default -> throw new IllegalArgumentException("Non-red/non-blue attempted to capture the flag");
        }

        inv.setItemInOffHand(null);
        scoreCapture(belongingFlagTeam, enemyFlagCarrier);

        this.updateInfo();
    }

    public void scoreCapture(Team capturerTeam, Player capturer) {
        if (started) {
            switch (capturerTeam) {
                case RED -> captures[0] += 1;
                case BLUE -> captures[1] += 1;
                default -> throw new IllegalArgumentException("scoreCapture called on non-red/non-blue team");
            }

            Integer oldScore = scores.putIfAbsent(capturer, 0);
            if (oldScore == null)
                oldScore = 0;
            scores.put(capturer, oldScore+5);

            this.updateScoreboard();
        }

        for (Player player : this.players.keySet()) {
            player.sendMessage(
                    TranslationManager.t("match.ctf.flag.capture", player,
                            Placeholder.unparsed("player_name", capturer.getName()),
                            Placeholder.component("flag_color", Component.text(capturerTeam.oppositeTeam().name()).color(TextColor.color(Team.Colors.get(capturerTeam.oppositeTeam())))))
            );
            
            // Build subtitle with current score
            Component subtitle;
            if (captures[0] > captures[1]) {
                // Red leads
                subtitle = Component.text("Red leads " + captures[0] + " to " + captures[1])
                    .color(TextColor.color(Team.Colors.get(Team.RED)));
            } else if (captures[1] > captures[0]) {
                // Blue leads
                subtitle = Component.text("Blue leads " + captures[1] + " to " + captures[0])
                    .color(TextColor.color(Team.Colors.get(Team.BLUE)));
            } else {
                // Teams are tied
                subtitle = Component.text("Teams are tied " + captures[0] + " to " + captures[1])
                    .color(TextColor.color(0xFFFF00));
            }
            
            // Show title with capture message and score
            player.showTitle(Title.title(
                    TranslationManager.t("match.ctf.flag.capture", player,
                            Placeholder.unparsed("player_name", capturer.getName()),
                            Placeholder.component("flag_color", Component.text(capturerTeam.oppositeTeam().name()).color(TextColor.color(Team.Colors.get(capturerTeam.oppositeTeam()))))),
                    subtitle,
                    Title.Times.times(Duration.ZERO, Duration.ofSeconds(3), Duration.ofMillis(500))
            ));
            
            // Play sound based on whether the capturing team is player's team or opponent
            Team playerTeam = this.players.get(player);
            if (playerTeam == capturerTeam) {
                // Player's team captured the flag
                player.playSound(player, "quake.feedback.teamplay.flagcapture.yourteam", SoundCategory.NEUTRAL, 1, 1);
            } else {
                // Opponent team captured the flag
                player.playSound(player, "quake.feedback.teamplay.flagcapture.opponent", SoundCategory.NEUTRAL, 1, 1);
            }
        }

        // End match if capturelimit is reached
        Component winningTeam = Component.text("(unknown)");
        if (captures[0] > captures[1]) // red is leading
            winningTeam = Component.text("RED").color(TextColor.color(Team.Colors.get(Team.RED)));
        else if (captures[1] > captures[0]) // blue is leading
            winningTeam = Component.text("BLUE").color(TextColor.color(Team.Colors.get(Team.BLUE)));

        if (captures[0] == capturelimit || captures[1] == capturelimit) { // red or blue
            for (Player player : this.players.keySet()) {

                player.showTitle(Title.title(
                        TranslationManager.t("match.team.winsBegin", player)
                                .append(winningTeam)
                                .append(TranslationManager.t("match.team.winsEnd", player)),
                        Component.empty(),
                        Title.Times.times(Duration.ZERO, Duration.ofSeconds(3), Duration.ofMillis(500))
                ));
                player.sendMessage(TranslationManager.t("match.aftermath.scoreboardBegin", player));
                player.sendMessage(this.getScoreboard());
            }
            this.end();
        }
    }

    private Component getScoreboard() {
        List<Map.Entry<Player, Integer>> sortedScores = new ArrayList<>(scores.entrySet());
        sortedScores.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));

        // Build Red team table
        TableBuilder redTableBuilder = new TableBuilder();
        redTableBuilder.addRow("Score", "Ping", "Name");
        redTableBuilder.addRow("", "", "");

        for (Map.Entry<Player, Integer> scoreEntry : sortedScores) {
            if (players.get(scoreEntry.getKey()) != Team.RED) continue;
            redTableBuilder.addRow(
                scoreEntry.getValue().toString(),
                String.valueOf(scoreEntry.getKey().getPing()),
                scoreEntry.getKey().getName()
            );
        }

        // Build Blue team table
        TableBuilder blueTableBuilder = new TableBuilder();
        blueTableBuilder.addRow("Score", "Ping", "Name");
        blueTableBuilder.addRow("", "", "");

        for (Map.Entry<Player, Integer> scoreEntry : sortedScores) {
            if (players.get(scoreEntry.getKey()) != Team.BLUE) continue;
            blueTableBuilder.addRow(
                scoreEntry.getValue().toString(),
                String.valueOf(scoreEntry.getKey().getPing()),
                scoreEntry.getKey().getName()
            );
        }

        Component redTable = Component.text(redTableBuilder.build())
            .font(Key.key("mono"))
            .color(TextColor.color(Team.Colors.get(Team.RED)));

        Component blueTable = Component.text(blueTableBuilder.build())
            .font(Key.key("mono"))
            .color(TextColor.color(Team.Colors.get(Team.BLUE)));

        return Component.empty()
            .append(redTable)
            .appendNewline()
            .appendNewline()
            .append(blueTable);
    }

    private void updateInfo() {
        char redFlagIcon;
        if (flagCarriers[0] != null)
            redFlagIcon = Icons.RED_FLAG_TAKEN;
        else if (getRedFlag().getDisplay().getItemStack().isEmpty())
            redFlagIcon = Icons.RED_FLAG_LOST;
        else
            redFlagIcon = Icons.RED_FLAG;

        char blueFlagIcon;
        if (flagCarriers[1] != null)
            blueFlagIcon = Icons.BLUE_FLAG_TAKEN;
        else if (getBlueFlag().getDisplay().getItemStack().isEmpty())
            blueFlagIcon = Icons.BLUE_FLAG_LOST;
        else
            blueFlagIcon = Icons.BLUE_FLAG;

        Component redStatus = Component.text(captures[0]).color(TextColor.color(Team.Colors.get(Team.RED)))
                .append(Component.text(redFlagIcon).color(TextColor.color(0xffffff)));
        Component blueStatus = Component.text(captures[1]).color(TextColor.color(Team.Colors.get(Team.BLUE)))
                .append(Component.text(blueFlagIcon).color(TextColor.color(0xffffff)));

        this.infoBar.name(Component.empty().append(redStatus).append(Component.text(" ")).append(blueStatus).font(Key.key("hud_bossbar")));
    }

    private Component getHeaderComponent() {
        Component header;
        if (captures[1] > captures[0]) {
            // Blue leads
            header = Component.text("Blue leads " + captures[1] + " to " + captures[0])
                .color(TextColor.color(Team.Colors.get(Team.BLUE)));
        } else if (captures[0] > captures[1]) {
            // Red leads
            header = Component.text("Red leads " + captures[0] + " to " + captures[1])
                .color(TextColor.color(Team.Colors.get(Team.RED)));
        } else {
            // Teams are tied
            header = Component.text("Teams are tied")
                .color(TextColor.color(0xFFFF00));
        }
        return header.appendNewline();
    }

    private void updateScoreboard() {
        for (Player player : players.keySet()) {
            Component header = getHeaderComponent();
            player.sendPlayerListHeaderAndFooter(header, this.getScoreboard());
        }
    }

    @Override
    public void onDeath(Player victim, Entity attacker, DamageCause cause) {
        super.onDeath(victim, attacker, cause);
        for (Player viewer : this.players.keySet()) {
            viewer.sendMessage(getDeathMessage(victim, attacker, cause, viewer.locale()));
        }

        if (attacker instanceof Player pAttacker && victim != attacker) {
            boolean sameTeam = getPlayersInTeam(players.get(pAttacker)).contains(victim);

            Integer oldScores = scores.putIfAbsent(pAttacker, 0);
            if (oldScores == null) {
                oldScores = 0;
            }

            if (!sameTeam && started)
                scores.put(pAttacker, oldScores + 1);
            else
                scores.put(pAttacker, oldScores - 1);

            pAttacker.showTitle(Title.title(
                    TranslationManager.t("game.kill.message", pAttacker, Placeholder.unparsed("victim", victim.getName())),
                    Component.empty(),
                    Title.Times.times(Duration.ZERO, Duration.ofSeconds(3), Duration.ofMillis(500))
            ));
        } else if ((attacker == null || victim == attacker) && started) {
            Integer oldKills = scores.get(victim);
            scores.put(victim, oldKills-1);
        }

        Location loc = victim.getLocation();
        loc.setY(loc.y()+0.5);
        Team carryingFlagTeam = this.getCarryingFlagTeam(victim);
        if (carryingFlagTeam != null) {
            if (victim.getLocation().y() > -64) { // If victim is the carrier and is above minimum level
                // Drop flag
                new CTFFlag(carryingFlagTeam, true, this, loc);
            } else if (victim.getLocation().y() < -64) {
                returnFlag(carryingFlagTeam, null);
            }

            // Reset carrier data
            switch (carryingFlagTeam) {
                case RED -> flagCarriers[0] = null;
                case BLUE -> flagCarriers[1] = null;
                default ->
                        throw new IllegalArgumentException("Attempted to drop flag belonging to non-red/non-blue team");
            }

            this.updateInfo();
        }

        this.updateScoreboard();
    }

    @Override
    public List<Team> allowedTeams() {
        return List.of(Team.RED, Team.BLUE);
    }
}
