package ru.darkchronics.quake.events.listeners;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import ru.darkchronics.quake.QuakePlugin;
import ru.darkchronics.quake.QuakeUserState;
import ru.darkchronics.quake.misc.QChatRenderer;

public class ChatListener implements Listener {
    @EventHandler
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();

        QuakeUserState userState = QuakePlugin.INSTANCE.userStates.get(player);
        if (userState.currentMatch == null) return;

        event.viewers().clear();
        event.viewers().add(Audience.audience(userState.currentMatch.getPlayers()));

        event.renderer(new QChatRenderer());
    }
}
