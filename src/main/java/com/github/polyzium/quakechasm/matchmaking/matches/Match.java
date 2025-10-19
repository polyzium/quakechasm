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
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
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

import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public abstract class Match implements ForwardingAudience {
    protected QMap map;
    protected HashMap<Player, Team> players = new HashMap<>();
    protected Scoreboard vanillaScoreboard;
    protected org.bukkit.scoreboard.Team vanillaRedTeam;
    protected org.bukkit.scoreboard.Team vanillaBlueTeam;
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
    public String getName() {
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
        for (Player player : players.keySet()) {
            cleanup(player);
        }

        if (this.isTeamMatch()) {
            this.vanillaRedTeam.unregister();
            this.vanillaBlueTeam.unregister();
        }

        QuakePlugin.INSTANCE.matchManager.matches.remove(this);
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

        MiscUtil.teleEffect(player.getLocation(), true);
        player.teleport(QuakePlugin.LOBBY);
        userState.reset();

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            player.listPlayer(onlinePlayer);
        }

        player.sendPlayerListHeader(Component.empty());
    }
    public abstract void onDeath(Player victim, Entity attacker, DamageCause cause);
    public abstract List<Team> allowedTeams();
    public boolean isTeamMatch() {
        List<Team> allowedTeams = this.allowedTeams();
        return allowedTeams.contains(Team.RED) && allowedTeams.contains(Team.BLUE);
    };
    public static Component getDeathMessage(Player victim, Entity attacker, DamageCause cause, Locale locale) {
        // TODO Vault API for prefixes and shit
        TextComponent component;
        if (attacker == null || victim == attacker) {
            String deathMsg = DeathMessages.SINGLE.get(cause);
            if (deathMsg == null) {
                deathMsg = cause.name();
            }
            component = Component.text(victim.getName()+" "+deathMsg);
        } else {
            Pair<String, String> deathMsg = DeathMessages.POLAR.get(cause);
            if (deathMsg == null) {
                deathMsg = new Pair<>(cause.name(), "");
            }
            component = Component.text(victim.getName()+" "+ TranslationManager.t(deathMsg.getLeft(), locale)+" "+attacker.getName()+TranslationManager.t(deathMsg.getRight(), locale));
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
