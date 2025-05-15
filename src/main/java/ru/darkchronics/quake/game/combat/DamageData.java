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
