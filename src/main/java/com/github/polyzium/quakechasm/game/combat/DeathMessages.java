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

package com.github.polyzium.quakechasm.game.combat;

import com.github.polyzium.quakechasm.misc.Pair;

import java.util.EnumMap;

public abstract class DeathMessages {
    public static EnumMap<DamageCause, String> SINGLE = new EnumMap<>(DamageCause.class);
    public static EnumMap<DamageCause, Pair<String, String>> POLAR = new EnumMap<>(DamageCause.class);
    static {
        SINGLE.put(DamageCause.UNKNOWN, "OB_UNKNOWN");
        SINGLE.put(DamageCause.SUICIDE, "OB_SUICIDE_GENERIC");
        SINGLE.put(DamageCause.FALLING, "OB_FALL");
        SINGLE.put(DamageCause.CRUSH, "OB_CRUSH");
        SINGLE.put(DamageCause.WATER, "OB_SUICIDE_DROWNING");
        SINGLE.put(DamageCause.LAVA, "OB_SUICIDE_LAVA");
        SINGLE.put(DamageCause.TARGET_LASER, "OB_SUICIDE_LASER");
        SINGLE.put(DamageCause.TRIGGER_HURT, "OB_SUICIDE_VOID");
        SINGLE.put(DamageCause.ROCKET_SPLASH, "OB_SUICIDE_EXPLOSION");
        SINGLE.put(DamageCause.BFG_SPLASH, "OB_SUICIDE_BFG");

        POLAR.put(DamageCause.UNKNOWN, new Pair<>("OB_GENERIC", ""));
//        POLAR.put(DamageCause.GRAPPLE, new Pair<>("OB_GRAPPLE", ""));
//        POLAR.put(DamageCause.GAUNTLET, new Pair<>("OB_GAUNTLET", ""));
        POLAR.put(DamageCause.MACHINEGUN, new Pair<>("OB_MACHINEGUN", ""));
        POLAR.put(DamageCause.SHOTGUN, new Pair<>("OB_SHOTGUN", ""));
//        POLAR.put(DamageCause.GRENADE, new Pair<>("OB_ROCKET_1", "'s grenade"));
//        POLAR.put(DamageCause.GRENADE_SPLASH, new Pair<>("OB_GRENADE", "'s shrapnel"));
        POLAR.put(DamageCause.ROCKET, new Pair<>("OB_ROCKET_1", "OB_ROCKET_2"));
        POLAR.put(DamageCause.ROCKET_SPLASH, new Pair<>("OB_ROCKET_SPLASH_1", "OB_ROCKET_SPLASH_2"));
        POLAR.put(DamageCause.PLASMA, new Pair<>("OB_PLASMAGUN_1", "OB_PLASMAGUN_2"));
//        POLAR.put(DamageCause.PLASMA_SPLASH, new Pair<>("OB_PLASMAGUN", "'s plasmagun"));
        POLAR.put(DamageCause.RAILGUN, new Pair<>("OB_RAILGUN", ""));
        POLAR.put(DamageCause.LIGHTNING, new Pair<>("OB_LIGHTNING", ""));
        POLAR.put(DamageCause.BFG, new Pair<>("OB_BFG_1", "OB_BFG_2"));
        POLAR.put(DamageCause.BFG_SPLASH, new Pair<>("OB_BFG_1", "OB_BFG_2"));
        POLAR.put(DamageCause.BFG_RAY, new Pair<>("OB_LASER", "OB_BFG_2"));
    }
}
