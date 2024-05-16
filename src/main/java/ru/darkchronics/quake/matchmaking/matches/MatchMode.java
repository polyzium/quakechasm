package ru.darkchronics.quake.matchmaking.matches;

public enum MatchMode {
    FFA("Free For All"),
    TDM("Team Deathmatch"),
    CTF("Capture The Flag");

    private final String displayName;

    MatchMode(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
