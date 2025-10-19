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

package com.github.polyzium.quakechasm.misc;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;

public enum Chatroom {
    GLOBAL(Component.text("GLOBAL").color(TextColor.color(0x55ff55)).decorate(TextDecoration.BOLD)),
    MATCH(Component.text("MATCH").color(TextColor.color(0xffaa00)).decorate(TextDecoration.BOLD)),
    PARTY(Component.text("PARTY").color(TextColor.color(0xff55ff)).decorate(TextDecoration.BOLD)),
    TEAM(Component.text("TEAM").color(TextColor.color(0x55ffff)).decorate(TextDecoration.BOLD));

    final Component prefix;

    Chatroom(Component prefix) {
        this.prefix = prefix;
    }

    public Component getPrefix() {
        return prefix;
    }
}
