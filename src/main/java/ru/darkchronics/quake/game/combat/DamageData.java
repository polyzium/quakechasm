package ru.darkchronics.quake.game.combat;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

public class DamageData {
    private Entity attacker;
    private double damage;
    private DamageCause cause;
    private boolean cancelled;

    public DamageData(Entity attacker, double damage, DamageCause cause) {
        this.attacker = attacker;
        this.damage = damage;
        this.cause = cause;
    }

    public Entity getAttacker() {
        return attacker;
    }

    public DamageCause getCause() {
        return cause;
    }
}
