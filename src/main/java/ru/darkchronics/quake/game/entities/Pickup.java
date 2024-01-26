package ru.darkchronics.quake.game.entities;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import ru.darkchronics.quake.game.entities.Trigger;

public interface Pickup extends Trigger {
    public void onPickup(Player player);
}