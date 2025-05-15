package ru.darkchronics.quake.game.combat;

import ru.darkchronics.quake.game.combat.DamageCause;
import ru.darkchronics.quake.misc.Pair;

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
