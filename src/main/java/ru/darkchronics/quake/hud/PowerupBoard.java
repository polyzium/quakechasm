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

package ru.darkchronics.quake.hud;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.TextColor;
import fr.mrmicky.fastboard.adventure.FastBoard;
//import ru.darkchronics.fastboard.adventure.FastBoard;
import ru.darkchronics.quake.QuakeUserState;
import ru.darkchronics.quake.game.combat.powerup.Powerup;
import ru.darkchronics.quake.game.combat.powerup.PowerupType;

import java.util.ArrayList;
import java.util.EnumMap;

public class PowerupBoard {
    QuakeUserState state;
    FastBoard board;
    private boolean needsUpdate;
    public static EnumMap<PowerupType, Character> ICONS = new EnumMap<>(PowerupType.class);
    static {
        ICONS.put(PowerupType.QUAD_DAMAGE, Icons.QUAD_DAMAGE);
        ICONS.put(PowerupType.REGENERATION, Icons.REGENERATION);
        ICONS.put(PowerupType.PROTECTION, Icons.PROTECTION);
    };

    public PowerupBoard(QuakeUserState state) {
        this.state = state;
        this.board = new FastBoard(this.state.getPlayer());
    }

    public void rebuild() {
        this.board = new FastBoard(state.getPlayer());
    }

    public void update() {
        this.needsUpdate = true;
    }

    public void draw() {
        if (board.isDeleted() && state.activePowerups.isEmpty() || !needsUpdate)
            return;

        if (state.activePowerups.isEmpty()) {
            board.delete();
            return;
        } else if (board.isDeleted()) {
            rebuild();
        }

        ArrayList<Component> lines = new ArrayList<>(3);
        for (int i = 0; i < state.activePowerups.size(); i++) {
            Powerup powerup = state.activePowerups.get(i);

            Component icon = Component.text(ICONS.get(powerup.getType())).font(Key.key("hud"));
            Component time = Component.text(powerup.getTime()).color(TextColor.color(TextColor.color(0xff3f3f))).font(Key.key("hud"));
            Component component = Component.join(JoinConfiguration.noSeparators(), icon, time);

            lines.add(Component.empty());
            lines.add(component);
            lines.add(Component.empty());
            lines.add(Component.empty());
        }
        board.updateLines(lines);

        this.needsUpdate = false;
    }
}
