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

package com.github.polyzium.quakechasm.events.listeners;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import com.github.polyzium.quakechasm.QuakePlugin;
import com.github.polyzium.quakechasm.QuakeUserState;
import com.github.polyzium.quakechasm.misc.Chatroom;

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
