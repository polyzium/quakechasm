package ru.darkchronics.quake.misc;

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
