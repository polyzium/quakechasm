package ru.darkchronics.quake.events.listeners;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import ru.darkchronics.quake.QuakePlugin;
import ru.darkchronics.quake.QuakeUserState;
import ru.darkchronics.quake.misc.Chatroom;

public class ChatListener implements Listener {
    @EventHandler
    public void onChat(AsyncChatEvent event) {
        QuakeUserState userState = QuakePlugin.INSTANCE.userStates.get(event.getPlayer());
        switch (userState.currentChat) {
            case GLOBAL -> chatGlobal(event);
            case MATCH -> chatMatch(event);
            case PARTY -> chatParty(event);
            case TEAM -> chatTeam(event);
        }
    }

    // TODO: prefixes
    public void chatGlobal(AsyncChatEvent event) {
        event.renderer((source, sourceDisplayName, message, viewer) ->
                Component.textOfChildren(Chatroom.GLOBAL.getPrefix(), MiniMessage.miniMessage().deserialize(
                " <b><color:#7f7f7f><source_display_name></color></b> <message>",
                Placeholder.component("source_display_name", sourceDisplayName),
                Placeholder.component("message", message)
            ))
        );
    }

    public void chatMatch(AsyncChatEvent event) {
        QuakeUserState userState = QuakePlugin.INSTANCE.userStates.get(event.getPlayer());

        event.viewers().clear();
        event.viewers().add(Audience.audience(userState.currentMatch));

        event.renderer((source, sourceDisplayName, message, viewer) ->
                Component.textOfChildren(Chatroom.MATCH.getPrefix(), MiniMessage.miniMessage().deserialize(
                " <b><color:#7f7f7f><source_display_name></color></b> <message>",
                Placeholder.component("source_display_name", sourceDisplayName),
                Placeholder.component("message", message)
            ))
        );
    }

    public void chatParty(AsyncChatEvent event) {
        QuakeUserState userState = QuakePlugin.INSTANCE.userStates.get(event.getPlayer());

        event.viewers().clear();
        event.viewers().add(userState.mmState.currentParty);

        event.renderer((source, sourceDisplayName, message, viewer) ->
                Component.textOfChildren(Chatroom.PARTY.getPrefix(), MiniMessage.miniMessage().deserialize(
                " <b><color:#7f7f7f><source_display_name></color></b> <message>",
                Placeholder.component("source_display_name", sourceDisplayName),
                Placeholder.component("message", message)
            ))
        );
    }

    public void chatTeam(AsyncChatEvent event) {
        Player player = event.getPlayer();
        QuakeUserState userState = QuakePlugin.INSTANCE.userStates.get(player);

        event.viewers().clear();
        event.viewers().add(Audience.audience(userState.currentMatch.getTeamAudience(
                userState.currentMatch.getTeamOfPlayer(player)
        )));

        event.renderer((source, sourceDisplayName, message, viewer) ->
                Component.textOfChildren(Chatroom.TEAM.getPrefix(), MiniMessage.miniMessage().deserialize(
                " <b><color:#7f7f7f><source_display_name></color></b> <message>",
                Placeholder.component("source_display_name", sourceDisplayName),
                Placeholder.component("message", message)
            ))
        );
    }
}
