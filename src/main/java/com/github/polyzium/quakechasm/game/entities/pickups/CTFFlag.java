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

package com.github.polyzium.quakechasm.game.entities.pickups;

import net.kyori.adventure.text.Component;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.BoundingBox;
import com.github.polyzium.quakechasm.QuakePlugin;
import com.github.polyzium.quakechasm.QuakeUserState;
import com.github.polyzium.quakechasm.game.entities.DisplayPickup;
import com.github.polyzium.quakechasm.game.entities.QEntityUtil;
import com.github.polyzium.quakechasm.hud.Hud;
import com.github.polyzium.quakechasm.matchmaking.matches.CTFMatch;
import com.github.polyzium.quakechasm.matchmaking.Team;
import com.github.polyzium.quakechasm.matchmaking.map.QMap;

public class CTFFlag implements DisplayPickup {
    private ItemDisplay display;
    private Team team;
    private CTFMatch match;
    private boolean isDrop;
    static BoundingBox boundingBox = new BoundingBox(-1, -1, -1, 1, 0, 1);
    public CTFFlag(Team team, boolean isDrop, CTFMatch match, Location location) {
        ItemStack item = displayItem(team);

        this.display = (ItemDisplay) location.getWorld().spawnEntity(location, EntityType.ITEM_DISPLAY);
        this.display.setItemStack(item);
        this.team = team;
        this.isDrop = isDrop;
        this.match = match;

        PersistentDataContainer displayData = display.getPersistentDataContainer();
        NamespacedKey teamKey = new NamespacedKey(QuakePlugin.INSTANCE, "team");
        displayData.set(teamKey, PersistentDataType.STRING, team.name());
        NamespacedKey isDropKey = new NamespacedKey(QuakePlugin.INSTANCE, "is_drop");
        displayData.set(isDropKey, PersistentDataType.BOOLEAN, isDrop);

        QEntityUtil.setEntityType(this.display, "ctf_flag");

        // Init rotator
        this.display.setInterpolationDuration(20);
        this.display.setTeleportDuration(20);
        this.display.setRotation(0, 0);

        QuakePlugin.INSTANCE.triggers.add(this);
    }

    public CTFFlag(ItemDisplay display) {
        assert display != null;

        PersistentDataContainer displayData = display.getPersistentDataContainer();
        NamespacedKey teamKey = new NamespacedKey(QuakePlugin.INSTANCE, "team");
        String teamString = displayData.get(teamKey, PersistentDataType.STRING);
        NamespacedKey isDropKey = new NamespacedKey(QuakePlugin.INSTANCE, "is_drop");
        boolean isDrop = Boolean.TRUE.equals(
                displayData.get(isDropKey, PersistentDataType.BOOLEAN)
        );

        this.display = display;
        this.team = Team.valueOf(teamString);
        this.isDrop = isDrop;

        // Init rotator
        this.display.setInterpolationDuration(20);
        this.display.setTeleportDuration(20);
        this.display.setRotation(0, 0);

        for (QMap map : QuakePlugin.INSTANCE.maps) {
            if (display.getWorld() == map.world && map.bounds.contains(display.getLocation().toVector()))
                if (map.getMatch() instanceof CTFMatch match)
                    this.match = match;
        }


        QuakePlugin.INSTANCE.triggers.add(this);
    }

    private ItemStack displayItem(Team team) {
        ItemStack item = switch (team) {
            case RED -> new ItemStack(Material.RED_BANNER);
            case BLUE -> new ItemStack(Material.BLUE_BANNER);
            default -> throw new IllegalArgumentException("Only RED or BLUE team allowed for CTFFlag!");
        };

        return item;
    }

    public void prepareForMatch(CTFMatch match) {
        this.match = match;
        this.respawn();
    }

    public void cleanup() {
        this.respawn();
        this.match = null;
    }

    public void respawn() {
        if (!this.display.getItemStack().isEmpty() || this.isDrop) return;

        display.setItemStack(displayItem(this.team));
        display.getWorld().spawnParticle(Particle.INSTANT_EFFECT, display.getLocation(), 16, 0.5, 0.5, 0.5);
        display.getWorld().playSound(display, "quake.items.respawn", 0.5f, 1f);
    }

    public ItemDisplay getDisplay() {
        return this.display;
    }

    public boolean isDrop() {
        return isDrop;
    }

    public void onPickup(Player player) {
        ItemStack item = display.getItemStack();
        if (item.isEmpty()) return;

        QuakeUserState userState = QuakePlugin.INSTANCE.userStates.get(player);
        if (!(userState.currentMatch instanceof CTFMatch))
            return;

        if (this.isDrop && match.getPlayersInTeam(this.team).contains(player)) {
            match.returnFlag(this.team, player);
            this.remove();
            return;
        }

        // Base flag, same team
        if (!this.isDrop) {
            if (match.getCarryingFlagTeam(player) == this.team.oppositeTeam()) { // if the player is the flag carrier
                match.captureFlag(this.team, player);
                return;
            } else if (match.getPlayersInTeam(this.team).contains(player)) {
                // Do not pick up
                return;
            }
        }

        // Grab flag
        if (!isDrop)
            this.display.setItemStack(null);
        else {
            this.remove();
            QuakePlugin.INSTANCE.triggers.remove(this);
        }

        match.grabFlag(this.team, player);

        Hud.pickupMessage(player, Component.text(StringUtils.capitalize(this.team.name().toLowerCase()) + " Flag"));
    }

    public Team getTeam() {
        return this.team;
    }

    public void onTrigger(Entity entity) {
        if (!(entity instanceof Player)) return;
        this.onPickup((Player) entity);
    }

    public void onUnload() {
        // noop
    }

    public Location getLocation() {
        return this.display.getLocation();
    }

    public Entity getEntity() {
        return this.display;
    }

    public BoundingBox getOffsetBoundingBox() {
        return boundingBox;
    }

    public void remove() {
        this.display.remove();
    }
}
