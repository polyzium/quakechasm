package ru.darkchronics.quake.matchmaking;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import ru.darkchronics.quake.game.combat.DamageCause;
import ru.darkchronics.quake.matchmaking.map.QMap;

import java.util.List;

public class DebugMatch extends Match {
    public DebugMatch(QMap map) {
        super(map);
    }

    @Override
    public void join(Player player) {
        super.join(player);

        player.sendMessage(Component.text("This match is for testing purposes only!").color(TextColor.color(0xff0000)));
    }

    @Override
    public Team assignTeam(Player player) {
        return Team.FREE;
    }

    public static String getNameStatic() {
        return "Debug";
    }
    public String getName() {
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
