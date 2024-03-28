package ru.darkchronics.quake.matchmaking;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import ru.darkchronics.quake.QuakePlugin;
import ru.darkchronics.quake.QuakeUserState;
import ru.darkchronics.quake.game.combat.DamageCause;
import ru.darkchronics.quake.game.entities.Trigger;
import ru.darkchronics.quake.game.entities.pickups.CTFFlag;
import ru.darkchronics.quake.hud.Icons;
import ru.darkchronics.quake.matchmaking.map.QMap;

import java.time.Duration;
import java.util.*;

public class CTFMatch extends Match {
    private static final int CAPTURELIMIT = 5;
    private static final int NEED_PLAYERS = 2;
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

    public static String getNameStatic() {
        return "Capture The Flag";
    }
    public String getName() {
        return getNameStatic();
    }

    @Override
    public void join(Player player) {
        super.join(player);
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

        if (players.size() >= NEED_PLAYERS && this.warmupTask == null && !started) {
            warmup();
        }
    }

    @Override
    public void leave(Player player) {
        super.leave(player);
        scores.remove(player);
        player.hideBossBar(this.infoBar);

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
        CTFMatch self = this;
        this.warmupTask = new BukkitRunnable() {
            int count = 10;
            @Override
            public void run() {
                self.showTitle(Title.title(
                        Component.text(self.getName()),
                        Component.text("Match starts in: "+count),
                        Title.Times.times(Duration.ZERO, Duration.ofMillis(1200), Duration.ZERO)
                ));

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
        this.showTitle(Title.title(
                Component.text("Fight!"),
                Component.text("Capture the enemy flag "+ CAPTURELIMIT +" times").color(TextColor.color(0xff0000)),
                Title.Times.times(Duration.ZERO, Duration.ofSeconds(2), Duration.ofMillis(500))
        ));
    }

    public void end() {
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

        this.sendMessage(
                player.displayName()
                        .append(Component.text(" got the "))
                        .append(Component.text(belongingFlagTeam.name()).color(TextColor.color(Team.Colors.get(belongingFlagTeam))))
                        .append(Component.text(" flag!"))
        );

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
                if (returningPlayer != null)
                    this.sendMessage(
                        returningPlayer.displayName()
                                .append(Component.text(" returned the "))
                                .append(Component.text(team.name()).color(TextColor.color(Team.Colors.get(team))))
                                .append(Component.text(" flag!"))
                    );
                else
                    this.sendMessage(
                            Component.text("The ")
                                    .append(Component.text(team.name()).color(TextColor.color(Team.Colors.get(team))))
                                    .append(Component.text(" flag has returned!"))
                    );

                flag.respawn();
                return;
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

        this.sendMessage(
                capturer.displayName()
                        .append(Component.text(" captured the "))
                        .append(Component.text(capturerTeam.oppositeTeam().name()).color(TextColor.color(Team.Colors.get(capturerTeam.oppositeTeam()))))
                        .append(Component.text(" flag!"))
        );

        // End match if capturelimit is reached
        Component winningTeam = Component.text("(unknown)");
        if (captures[0] > captures[1]) // red is leading
            winningTeam = Component.text("RED").color(TextColor.color(Team.Colors.get(Team.RED)));
        else if (captures[1] > captures[0]) // blue is leading
            winningTeam = Component.text("BLUE").color(TextColor.color(Team.Colors.get(Team.BLUE)));

        if (captures[0] == CAPTURELIMIT || captures[1] == CAPTURELIMIT) { // red or blue
            this.showTitle(Title.title(
                    Component.text("Team ")
                            .append(winningTeam)
                            .append(Component.text(" wins")),
                    Component.empty(),
                    Title.Times.times(Duration.ZERO, Duration.ofSeconds(3), Duration.ofMillis(500))
            ));
            this.sendMessage("Scores for this match:");
            this.sendMessage(this.getScoreboard());
            this.end();
        }
    }

    private Component getScoreboard() {
        Component scoreboard = Component.empty();

        List<Map.Entry<Player, Integer>> sortedScores = new ArrayList<>(scores.entrySet());
        sortedScores.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));

        // Red team
        scoreboard = scoreboard.append(Component.text("Red team: " + captures[0]).color(TextColor.color(Team.Colors.get(Team.RED)))).appendNewline();
        for (Map.Entry<Player, Integer> scoreEntry : sortedScores) {
            if (players.get(scoreEntry.getKey()) != Team.RED) continue;
            scoreboard = scoreboard.append(
                    Component.text(scoreEntry.getKey().getName()+": "+scoreEntry.getValue())
                            .color(TextColor.color(0xffffff))
            ).appendNewline();
        }

        scoreboard = scoreboard.appendNewline();

        // Blue team
        scoreboard = scoreboard.append(Component.text("Blue team: " + captures[1]).color(TextColor.color(Team.Colors.get(Team.BLUE)))).appendNewline();
        for (Map.Entry<Player, Integer> killEntry : sortedScores) {
            if (players.get(killEntry.getKey()) != Team.BLUE) continue;
            scoreboard = scoreboard.append(
                    Component.text(killEntry.getKey().getName()+": "+killEntry.getValue())
                            .color(TextColor.color(0xffffff))
            ).appendNewline();
        }

        return scoreboard;
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

    private void updateScoreboard() {
        this.sendPlayerListHeader(
                Component.text("\n\n\n\n\n")
                .append(this.getScoreboard())
        );
    }

    @Override
    public void onDeath(Player victim, Entity attacker, DamageCause cause) {
        this.sendMessage(getDeathMessage(victim, attacker, cause));

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
                    Component.text("You fragged "+victim.getName()),
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
