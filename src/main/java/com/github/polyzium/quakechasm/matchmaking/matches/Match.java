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

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.ForwardingAudience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Scoreboard;
import org.jetbrains.annotations.NotNull;
import com.github.polyzium.quakechasm.QuakePlugin;
import com.github.polyzium.quakechasm.QuakeUserState;
import com.github.polyzium.quakechasm.game.combat.DamageCause;
import com.github.polyzium.quakechasm.game.combat.DeathMessages;
import com.github.polyzium.quakechasm.matchmaking.Team;
import com.github.polyzium.quakechasm.matchmaking.map.QMap;
import com.github.polyzium.quakechasm.misc.Chatroom;
import com.github.polyzium.quakechasm.misc.MiscUtil;
import com.github.polyzium.quakechasm.misc.Pair;
import com.github.polyzium.quakechasm.misc.TranslationManager;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public abstract class Match implements ForwardingAudience {
    protected QMap map;
    protected HashMap<Player, Team> players = new HashMap<>();
    protected Scoreboard vanillaScoreboard;
    protected org.bukkit.scoreboard.Team vanillaRedTeam;
    protected org.bukkit.scoreboard.Team vanillaBlueTeam;
    public boolean matchEnding = false;
    public Match(QMap map) {
        this.map = map;
        this.map.chunkLoad();

        // This is needed to hide nametags
        if (this.isTeamMatch()) {
            this.vanillaScoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
            this.vanillaRedTeam = this.vanillaScoreboard.registerNewTeam("red");
            this.vanillaBlueTeam = this.vanillaScoreboard.registerNewTeam("blue");

            this.vanillaRedTeam.setOption(org.bukkit.scoreboard.Team.Option.NAME_TAG_VISIBILITY, org.bukkit.scoreboard.Team.OptionStatus.FOR_OTHER_TEAMS);
            this.vanillaBlueTeam.setOption(org.bukkit.scoreboard.Team.Option.NAME_TAG_VISIBILITY, org.bukkit.scoreboard.Team.OptionStatus.FOR_OTHER_TEAMS);
        }
    }
    public List<Player> getPlayers() {
        return this.players.keySet().stream().toList();
    }
    public List<Player> getPlayersInTeam(Team team) {
        return this.players.keySet().stream()
                .filter(player -> this.players.get(player) == team)
                .toList();
    }
    public QMap getMap() {
        return this.map;
    }
    public static String getNameStatic() {
        return "MATCH_BASE";
    }
    public String getNameKey() {
        return getNameStatic();
    }
    public void sendMessage(String message) {
        this.sendMessage(Component.text(message));
    }
    public Team getTeamOfPlayer(Player player) {
          return this.players.get(player);
    }
    // Implementation-dependent methods, use at your own discretion
    public abstract void setScoreLimit(int scoreLimit);
    public abstract void setNeedPlayers(int needPlayers);

    public void join(Player player, Team team) {
        Team resolvedTeam;
        if (team == null)
            resolvedTeam = this.assignTeam(player);
        else
            resolvedTeam = team;

        if (!this.allowedTeams().contains(resolvedTeam)) {
            QuakePlugin.INSTANCE.getLogger().warning("Player attempted to join disallowed team, ignoring");
            return;
        }

        players.put(player, resolvedTeam);
        QuakeUserState userState = QuakePlugin.INSTANCE.userStates.get(player);
        userState.currentMatch = this;

        Location spawn = map.getRandomSpawnpoint(resolvedTeam);
        player.teleport(spawn);
        MiscUtil.teleEffect(spawn, false);
        userState.initForMatch();

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            player.unlistPlayer(onlinePlayer);
        }

        // This is needed to hide nametags
        if (this.isTeamMatch()) {
            switch (this.players.get(player)) {
                case RED -> this.vanillaRedTeam.addPlayer(player);
                case BLUE -> this.vanillaBlueTeam.addPlayer(player);
                default -> throw new IllegalArgumentException("Attempt to add player of disallowed team to vanilla team");
            }
            player.setScoreboard(this.vanillaScoreboard);
            setArmor(player, resolvedTeam);
        }

        userState.switchChat(Chatroom.MATCH);
        this.sendMessage(player.getName()+" entered the match");
    }

    public void leave(Player player) {
        players.remove(player);
        cleanup(player);

        this.sendMessage(player.getName()+" left the match");
    }
    public void end() {
        matchEnding = true;

        for (Player player : players.keySet()) {
            player.playSound(player, "quake.feedback.match_end", SoundCategory.NEUTRAL, 1, 1);
        }

        Match that = this;
        new BukkitRunnable() {
            int endTimer = 10;
            @Override
            public void run() {
                endTimer--;
                if (endTimer <= 5)
                    that.showTitle(Title.title(Component.empty(), Component.text("Teleporting to lobby in "+endTimer), Title.Times.times(Duration.ZERO, Duration.ofSeconds(1), Duration.ofSeconds(1))));

                if (endTimer == 0) {
                    for (Player player : players.keySet()) {
                        cleanup(player);
                    }

                    if (that.isTeamMatch()) {
                        that.vanillaRedTeam.unregister();
                        that.vanillaBlueTeam.unregister();
                    }

                    QuakePlugin.INSTANCE.matchManager.matches.remove(that);

                    cancel();
                }
            }
        }.runTaskTimer(QuakePlugin.INSTANCE, 20, 20);
    }
    public abstract Team assignTeam(Player player);
    public static void setArmor(Player player, Team team) {
        ItemStack torso = new ItemStack(Material.LEATHER_CHESTPLATE);
        ItemStack pants = new ItemStack(Material.LEATHER_LEGGINGS);
        ItemStack boots = new ItemStack(Material.LEATHER_BOOTS);
        LeatherArmorMeta torsoMeta = (LeatherArmorMeta) torso.getItemMeta();
        LeatherArmorMeta pantsMeta = (LeatherArmorMeta) pants.getItemMeta();
        LeatherArmorMeta bootsMeta = (LeatherArmorMeta) boots.getItemMeta();
        torsoMeta.setColor(Color.fromRGB(Team.Colors.get(team)));
        pantsMeta.setColor(Color.fromRGB(Team.Colors.get(team)));
        bootsMeta.setColor(Color.fromRGB(Team.Colors.get(team)));
        torso.setItemMeta(torsoMeta);
        pants.setItemMeta(pantsMeta);
        boots.setItemMeta(bootsMeta);
        player.getInventory().setChestplate(torso);
        player.getInventory().setLeggings(pants);
        player.getInventory().setBoots(boots);
    }
    public void cleanup(Player player) {
        QuakeUserState userState = QuakePlugin.INSTANCE.userStates.get(player);
        userState.currentMatch = null;
        userState.switchChat(Chatroom.GLOBAL);
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());

        // Restore normal movement speeds
        player.setWalkSpeed(0.4f);

        MiscUtil.teleEffect(player.getLocation(), true);
        player.teleport(QuakePlugin.LOBBY);
        userState.reset();

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            player.listPlayer(onlinePlayer);
        }

        player.sendPlayerListHeaderAndFooter(Component.empty(), Component.empty());
    }
    public abstract void onDeath(Player victim, Entity attacker, DamageCause cause);
    public abstract List<Team> allowedTeams();
    public boolean isTeamMatch() {
        List<Team> allowedTeams = this.allowedTeams();
        return allowedTeams.contains(Team.RED) && allowedTeams.contains(Team.BLUE);
    };
    public static Component getDeathMessage(Player victim, Entity attacker, DamageCause cause, Locale locale) {
        // TODO Vault API for prefixes and shit
        Component component;
        if (attacker == null || victim == attacker) {
            String deathMsgKey = DeathMessages.SUICIDE.get(cause);
            if (deathMsgKey == null) {
                deathMsgKey = "obituary.suicide.unknown";
            }
            component = TranslationManager.t(deathMsgKey, locale, Placeholder.parsed("victim_name", victim.getName()), Placeholder.parsed("death_cause", cause.name()));
        } else {
            String deathMsgKey = DeathMessages.FRAG.get(cause);
            if (deathMsgKey == null) {
                deathMsgKey = "obituary.unknown";
            }
            component = TranslationManager.t(deathMsgKey, locale, Placeholder.parsed("victim_name", victim.getName()), Placeholder.parsed("attacker_name", attacker.getName()), Placeholder.parsed("death_cause", cause.name()));
        }

        return component.color(TextColor.color(0xff3f3f));
    }

    public Audience getTeamAudience(Team team) {
        return Audience.audience(this.getPlayersInTeam(team));
    }

    public @NotNull Iterable<? extends Audience> audiences() {
        return this.players.keySet();
    }
}
