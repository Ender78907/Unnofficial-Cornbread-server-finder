package ar.santi.cornbread.core;

public final class Filters {
    public int minPlayers = 1;
    public int maxPlayers = -1; // -1 = no upper limit
    // 0 = cualquiera, 1 = sí, 2 = no. Desconocido no coincide con sí/no.
    public int whitelist = 2, cracked = 0;
    public boolean compatibleOnly = true;
    public boolean matches(ServerEntry s) {
        return s.online >= minPlayers && (maxPlayers < 0 || s.online <= maxPlayers) && matchesFlag(whitelist, s.whitelisted) && matchesFlag(cracked, s.cracked);
    }
    static boolean matchesFlag(int choice, Boolean value) {
        return choice == 0 || (value != null && value == (choice == 1));
    }
    public String query(int skip) {
        String q = "limit=40&skip=" + skip + "&sort=lastSeen&descending=true&minPlayers=" + Math.max(0, minPlayers);
        if (maxPlayers >= 0) q += "&maxPlayers=" + maxPlayers;
        if (whitelist != 0) q += "&whitelisted=" + (whitelist == 1);
        if (cracked != 0) q += "&cracked=" + (cracked == 1);
        return q;
    }
    public Filters copy() {
        Filters f = new Filters(); f.minPlayers = minPlayers; f.maxPlayers = maxPlayers; f.whitelist = whitelist;
        f.cracked = cracked; f.compatibleOnly = compatibleOnly; return f;
    }
}
