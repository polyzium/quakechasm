package ru.darkchronics.quake.misc;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class QChatRenderer implements io.papermc.paper.chat.ChatRenderer {
    @Override
    public @NotNull Component render(@NotNull Player source, @NotNull Component sourceDisplayName, @NotNull Component message, @NotNull Audience viewer) {
        return MiniMessage.miniMessage().deserialize(
                "<b><color:#7f7f7f><source_display_name></color></b> <message>",
                Placeholder.component("source_display_name", sourceDisplayName),
                Placeholder.component("message", message)
        );
    }
}
