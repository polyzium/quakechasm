package ru.darkchronics.quake.game.combat;

import ru.darkchronics.quake.game.combat.DamageCause;
import ru.darkchronics.quake.misc.Pair;

import java.util.EnumMap;

public abstract class DeathMessages {
    public static EnumMap<DamageCause, String> SINGLE = new EnumMap<>(DamageCause.class);
    public static EnumMap<DamageCause, Pair<String, String>> POLAR = new EnumMap<>(DamageCause.class);
    static {
        SINGLE.put(DamageCause.UNKNOWN, "died");
        SINGLE.put(DamageCause.SUICIDE, "suicides");
        SINGLE.put(DamageCause.FALLING, "cratered");
        SINGLE.put(DamageCause.CRUSH, "was squished");
        SINGLE.put(DamageCause.WATER, "sank like a rock");
        SINGLE.put(DamageCause.LAVA, "does a back flip into the lava");
        SINGLE.put(DamageCause.TARGET_LASER, "saw the light");
        SINGLE.put(DamageCause.TRIGGER_HURT, "was in the wrong place");
        SINGLE.put(DamageCause.ROCKET_SPLASH, "blew themselves up");
        SINGLE.put(DamageCause.BFG_SPLASH, "should have used a smaller gun");

        POLAR.put(DamageCause.UNKNOWN, new Pair<>("was killed by", ""));
//        POLAR.put(DamageCause.GRAPPLE, new Pair<>("was caught by", ""));
//        POLAR.put(DamageCause.GAUNTLET, new Pair<>("was pummeled by", ""));
        POLAR.put(DamageCause.MACHINEGUN, new Pair<>("was machinegunned by", ""));
        POLAR.put(DamageCause.SHOTGUN, new Pair<>("was gunned down by", ""));
//        POLAR.put(DamageCause.GRENADE, new Pair<>("ate", "'s grenade"));
//        POLAR.put(DamageCause.GRENADE_SPLASH, new Pair<>("was shredded by", "'s shrapnel"));
        POLAR.put(DamageCause.ROCKET, new Pair<>("ate", "'s rocket"));
        POLAR.put(DamageCause.ROCKET_SPLASH, new Pair<>("almost dodged", "'s rocket"));
        POLAR.put(DamageCause.PLASMA, new Pair<>("was melted by", "'s plasmagun"));
//        POLAR.put(DamageCause.PLASMA_SPLASH, new Pair<>("was melted by", "'s plasmagun"));
        POLAR.put(DamageCause.RAILGUN, new Pair<>("was railed by", ""));
        POLAR.put(DamageCause.LIGHTNING, new Pair<>("was electrocuted by", ""));
        POLAR.put(DamageCause.BFG, new Pair<>("was blasted by", "'s BFG"));
        POLAR.put(DamageCause.BFG_SPLASH, new Pair<>("was blasted by", "'s BFG"));
        POLAR.put(DamageCause.BFG_RAY, new Pair<>("saw the pretty lights from", "'s BFG"));
    }
}
