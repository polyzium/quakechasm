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

package com.github.polyzium.quakechasm;

import com.github.polyzium.quakechasm.game.combat.DamageData;
import com.github.polyzium.quakechasm.game.combat.MedalType;
import com.github.polyzium.quakechasm.game.combat.WeaponType;
import com.github.polyzium.quakechasm.game.combat.WeaponUserState;
import com.github.polyzium.quakechasm.game.combat.WeaponUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.title.Title;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import com.github.polyzium.quakechasm.game.combat.*;
import com.github.polyzium.quakechasm.game.combat.powerup.Powerup;
import com.github.polyzium.quakechasm.game.combat.powerup.PowerupType;
import com.github.polyzium.quakechasm.hud.Hud;
import com.github.polyzium.quakechasm.matchmaking.MatchmakingManager;
import com.github.polyzium.quakechasm.matchmaking.MatchmakingState;
import com.github.polyzium.quakechasm.matchmaking.Team;
import com.github.polyzium.quakechasm.matchmaking.matches.Match;
import com.github.polyzium.quakechasm.misc.Chatroom;
import com.github.polyzium.quakechasm.misc.MiscUtil;
import com.github.polyzium.quakechasm.misc.TranslationManager;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class QuakeUserState {
    private Player player;
    public WeaponUserState weaponState;
    public MatchmakingState mmState;
    public Location portalLoc = null;
    public BukkitRunnable healthDecreaser;
    public BukkitRunnable armorDecreaser;
    public int armor = 0;
    public ArrayList<Powerup> activePowerups = new ArrayList<>(3);
    public Hud hud;
    public Match currentMatch;
    public DamageData lastDamage;
    public Chatroom currentChat = Chatroom.GLOBAL;

    // Strafe jump state
    public int strafeJumpTicks = 0;

    // Medal tracking
    public HashMap<MedalType, Integer> medals = new HashMap<>();
    public long lastKillTime = 0;
    public int consecutiveRailgunHits = 0;

    public QuakeUserState(Player player) {
        this.player = player;
        this.weaponState = new WeaponUserState();
        this.mmState = new MatchmakingState();
        this.mmState.currentParty = new MatchmakingManager.Party(player);
        this.hud = new Hud(this);
    }

    public Player getPlayer() {
        return player;
    }

    public void reset() {
        this.weaponState = new WeaponUserState();
        this.armor = 0;
        for (Powerup activePowerup : this.activePowerups) {
            activePowerup.timer.cancel();
        }
        this.activePowerups.clear();
        this.hud.powerupBoard.update();
        this.player.setHealth(20);
        this.player.getAttribute(Attribute.MAX_HEALTH).setBaseValue(20);
        this.player.getInventory().clear();
        
        // Reset medal tracking
        this.medals.clear();
        this.lastKillTime = 0;
        this.consecutiveRailgunHits = 0;
    }

    public void initForMatch() {
        this.reset();
        this.initRespawn();
    }

    public void initRespawn() {
        // Clear all ammo
        Arrays.fill(weaponState.ammo, 0);

        ItemStack machinegun = new ItemStack(Material.CARROT_ON_A_STICK);
        ItemMeta mgMeta = machinegun.getItemMeta();
        mgMeta.setCustomModelData(WeaponType.MACHINEGUN);
        mgMeta.displayName(
                TranslationManager.t("pickup.weapon.machinegun", this.player).
                        decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.byBoolean(false))
        );
        machinegun.setItemMeta(mgMeta);

        weaponState.ammo[WeaponType.MACHINEGUN] = WeaponUtil.DEFAULT_AMMO[WeaponType.MACHINEGUN];

        player.getInventory().addItem(machinegun);
        player.getInventory().setHeldItemSlot(0);

        // Set health to 125 Quake HP
        player.getAttribute(Attribute.MAX_HEALTH).setBaseValue(25);
        player.setHealth(25);
        this.startHealthDecreaser();
    }
    
    public Location prepareRespawn() {
        this.initRespawn();
        return this.currentMatch.getMap().getRandomSpawnpoint(this.currentMatch.getTeamOfPlayer(this.player));
    }

    public void respawn() {
        Location spawnpoint = this.prepareRespawn();
        player.teleport(spawnpoint);
        MiscUtil.teleEffect(spawnpoint, false);

        if (this.currentMatch.isTeamMatch())
            Match.setArmor(this.player, this.currentMatch.getTeamOfPlayer(this.player));
    }

    public void switchChat(Chatroom chatroom) {
        if (
                this.currentMatch == null &&
                (chatroom == Chatroom.MATCH || chatroom == Chatroom.TEAM)
        ) {
            player.sendMessage(TranslationManager.t("error.chat.switchNoMatch", player,
                    Placeholder.unparsed("chatroom", chatroom.name())));
            return;
        }

        if (
                this.currentMatch != null &&
                        this.currentMatch.allowedTeams().stream().allMatch(team -> team == Team.FREE) &&
                        chatroom == Chatroom.TEAM
        ) {
            player.sendMessage(TranslationManager.t("error.match.notTeam", player));
            return;
        }

        this.currentChat = chatroom;
        player.sendMessage(TranslationManager.t("command.chat.switch", player,
                Placeholder.component("chatroom", this.currentChat.getPrefix().decoration(TextDecoration.BOLD, false))));
    }

    public void startArmorDecreaser() {
        if (this.armorDecreaser != null) return;

        this.armorDecreaser = new BukkitRunnable() {
            @Override
            public void run() {
                if (armor <= 100) {
                    this.cancel();
                    armorDecreaser = null;
                    return;
                }
                armor -= 1;
            }
        };
        armorDecreaser.runTaskTimer(QuakePlugin.INSTANCE, 20, 20);
    }

    public void startHealthDecreaser() {
        if (this.healthDecreaser != null) return;

        this.healthDecreaser = new BukkitRunnable() {
            @Override
            public void run() {
                // Fix NullPointerException on logout
                try {
                    if (Powerup.hasPowerup(player, PowerupType.REGENERATION)) return;
                } catch (NullPointerException e) {
                    cancel();
                }

                double currentHealth = player.getHealth();
                if (currentHealth <= 20) {
                    player.getAttribute(Attribute.MAX_HEALTH).setBaseValue(20);
                    this.cancel();
                    healthDecreaser = null;
                    return;
                }
                float newHealth = Math.round((currentHealth * 5) - 1) / 5f;
                player.setHealth(newHealth);
                player.getAttribute(Attribute.MAX_HEALTH).setBaseValue(newHealth);
            }
        };
        healthDecreaser.runTaskTimer(QuakePlugin.INSTANCE, 20, 20);
    }

    public void awardMedal(MedalType medalType) {
        int count = medals.getOrDefault(medalType, 0) + 1;
        medals.put(medalType, count);

        String medalText = medalType.getDisplayName() + " x" + count;

        player.sendMessage(Component.text("Medal awarded: " + medalText).color(TextColor.color(0xFFD700)));
        
//        player.showTitle(Title.title(
//                Component.text(medalText).color(TextColor.color(0xFFD700)),
//                Component.empty(),
//                Title.Times.times(Duration.ZERO, Duration.ofSeconds(2), Duration.ofMillis(500))
//        ));
    }

    public void checkExcellentMedal() {
        long currentTime = System.currentTimeMillis();
        
        if (lastKillTime != 0 && (currentTime - lastKillTime) <= 2000) {
            awardMedal(MedalType.EXCELLENT);
        }
        
        lastKillTime = currentTime;
    }

    public void checkImpressiveMedal() {
        if (consecutiveRailgunHits >= 2)
            awardMedal(MedalType.IMPRESSIVE);
    }
}
