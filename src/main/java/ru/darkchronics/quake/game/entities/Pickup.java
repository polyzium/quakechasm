package ru.darkchronics.quake.game.entities;

import org.bukkit.entity.Player;

public interface Pickup {
    public void onPickup(Player player);
    public void remove();
}
