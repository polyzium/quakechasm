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
    public static EnumMap<DamageCause, String> SUICIDE = new EnumMap<>(DamageCause.class);
    public static EnumMap<DamageCause, String> FRAG = new EnumMap<>(DamageCause.class);
    static {
        SUICIDE.put(DamageCause.UNKNOWN, "obituary.unknown");
        SUICIDE.put(DamageCause.SUICIDE, "obituary.suicide.generic");
        SUICIDE.put(DamageCause.FALLING, "obituary.fall");
        SUICIDE.put(DamageCause.CRUSH, "obituary.crush");
        SUICIDE.put(DamageCause.WATER, "obituary.suicide.drowning");
        SUICIDE.put(DamageCause.LAVA, "obituary.suicide.lava");
        SUICIDE.put(DamageCause.TARGET_LASER, "obituary.suicide.laser");
        SUICIDE.put(DamageCause.TRIGGER_HURT, "obituary.suicide.void");
        SUICIDE.put(DamageCause.ROCKET_SPLASH, "obituary.suicide.explosion");
        SUICIDE.put(DamageCause.PLASMA_SPLASH, "obituary.suicide.plasmagun");
        SUICIDE.put(DamageCause.BFG_SPLASH, "obituary.suicide.bfg");

        FRAG.put(DamageCause.UNKNOWN, "obituary.generic");
//        FRAG.put(DamageCause.GRAPPLE, "obituary.grapple");
//        FRAG.put(DamageCause.GAUNTLET, "obituary.gauntlet");
        FRAG.put(DamageCause.MACHINEGUN, "obituary.machinegun");
        FRAG.put(DamageCause.SHOTGUN, "obituary.shotgun");
//        FRAG.put(DamageCause.GRENADE, "obituary.rocket");
//        FRAG.put(DamageCause.GRENADE_SPLASH, "obituary.grenade");
        FRAG.put(DamageCause.ROCKET, "obituary.rocket");
        FRAG.put(DamageCause.ROCKET_SPLASH, "obituary.rocketSplash");
        FRAG.put(DamageCause.PLASMA, "obituary.plasmagun");
        FRAG.put(DamageCause.PLASMA_SPLASH, "obituary.plasmagun");
        FRAG.put(DamageCause.RAILGUN, "obituary.railgun");
        FRAG.put(DamageCause.LIGHTNING, "obituary.lightning");
        FRAG.put(DamageCause.BFG, "obituary.bfg");
        FRAG.put(DamageCause.BFG_SPLASH, "obituary.bfg");
        FRAG.put(DamageCause.BFG_RAY, "obituary.bfgLaser");
        FRAG.put(DamageCause.TELEFRAG, "obituary.telefrag");
    }
}
