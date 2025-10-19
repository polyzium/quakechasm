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

package ru.darkchronics.quake;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import ru.darkchronics.quake.game.combat.*;
import ru.darkchronics.quake.game.combat.powerup.Powerup;
import ru.darkchronics.quake.game.combat.powerup.PowerupType;
import ru.darkchronics.quake.hud.Hud;
import ru.darkchronics.quake.matchmaking.MatchmakingManager;
import ru.darkchronics.quake.matchmaking.MatchmakingState;
import ru.darkchronics.quake.matchmaking.Team;
import ru.darkchronics.quake.matchmaking.matches.Match;
import ru.darkchronics.quake.misc.Chatroom;
import ru.darkchronics.quake.misc.MiscUtil;
import ru.darkchronics.quake.misc.TranslationManager;

import java.util.ArrayList;
import java.util.Arrays;

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
    public int dashCooldown;
    public Chatroom currentChat = Chatroom.GLOBAL;

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
                Component.text(TranslationManager.t("PICKUP_WEAPON_MACHINEGUN", this.player)).
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
            player.sendMessage(TranslationManager.t("ERROR_CHAT_SWITCH_NOMATCH_1", player)+chatroom.name()+TranslationManager.t("ERROR_CHAT_SWITCH_NOMATCH_2", player));
            return;
        }

        if (
                this.currentMatch != null &&
                        this.currentMatch.allowedTeams().stream().allMatch(team -> team == Team.FREE) &&
                        chatroom == Chatroom.TEAM
        ) {
            player.sendMessage(TranslationManager.t("ERROR_MATCH_NOTTEAM", player));
            return;
        }

        this.currentChat = chatroom;
        player.sendMessage(Component.textOfChildren(
            Component.text(TranslationManager.t("COMMAND_CHAT_SWITCH_1", player)), this.currentChat.getPrefix().decoration(TextDecoration.BOLD, false), Component.text(TranslationManager.t("COMMAND_CHAT_SWITCH_2", player))
        ));
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
}
