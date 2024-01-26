package ru.darkchronics.quake;

import ru.darkchronics.quake.game.combat.WeaponUserState;

public class QuakeUserState {
    public QuakePlugin plugin;
    public WeaponUserState weaponState;

    public QuakeUserState(QuakePlugin plugin) {
        this.plugin = plugin;
        this.weaponState = new WeaponUserState(plugin);
    }
}
