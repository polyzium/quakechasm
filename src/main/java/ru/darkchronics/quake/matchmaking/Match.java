package ru.darkchronics.quake.matchmaking;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.ForwardingAudience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import ru.darkchronics.quake.QuakePlugin;
import ru.darkchronics.quake.QuakeUserState;
import ru.darkchronics.quake.game.combat.DamageCause;
import ru.darkchronics.quake.game.combat.DeathMessages;
import ru.darkchronics.quake.matchmaking.map.QMap;
import ru.darkchronics.quake.misc.MiscUtil;
import ru.darkchronics.quake.misc.Pair;

import java.util.HashMap;
import java.util.List;

public abstract class Match implements ForwardingAudience {
    protected QMap map;
    protected HashMap<Player, Team> players = new HashMap<>();
    public Match(QMap map) {
        this.map = map;
        this.map.chunkLoad();
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
        return "Base match";
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

    public void join(Player player) {
        Team team = this.assignTeam(player);

        players.put(player, team);
        QuakeUserState userState = QuakePlugin.INSTANCE.userStates.get(player);
        userState.currentMatch = this;

        Location spawn = map.getRandomSpawnpoint(team);
        player.teleport(spawn);
        MiscUtil.teleEffect(spawn, false);
        userState.initForMatch();

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            player.unlistPlayer(onlinePlayer);
        }

        this.sendMessage(player.getName()+" entered the game");
    }

    public void leave(Player player) {
        players.remove(player);
        cleanup(player);

        this.sendMessage(player.getName()+" disconnected");
    }
    public void end() {
        for (Player player : players.keySet()) {
            cleanup(player);
        }
        QuakePlugin.INSTANCE.matchManager.matches.remove(this);
    }
    public abstract Team assignTeam(Player player);
    public void cleanup(Player player) {
        QuakeUserState userState = QuakePlugin.INSTANCE.userStates.get(player);
        userState.currentMatch = null;

        // TODO teleport to lobby
        MiscUtil.teleEffect(player.getLocation(), true);
//        player.teleport(new Location(player.getWorld(), 0, 64, 0));
        userState.reset();

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            player.listPlayer(onlinePlayer);
        }

        player.sendPlayerListHeader(Component.empty());
    }
    public abstract void onDeath(Player victim, Entity attacker, DamageCause cause);
    public abstract List<Team> allowedTeams();
    public static Component getDeathMessage(Player victim, Entity attacker, DamageCause cause) {
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
            component = Component.text(victim.getName()+" "+deathMsg.getLeft()+" "+attacker.getName()+deathMsg.getRight());
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
