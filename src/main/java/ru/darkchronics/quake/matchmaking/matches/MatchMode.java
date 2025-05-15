package ru.darkchronics.quake.matchmaking.matches;

public enum MatchMode {
    FFA("MATCH_FFA_NAME"),
    TDM("MATCH_TDM_NAME"),
    CTF("MATCH_CTF_NAME");

    private final String displayName;

    MatchMode(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
