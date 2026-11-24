package ar.santi.cornbread.core;

import static ar.santi.cornbread.core.Texts.t;

public final class ServerEntry {
    public String host = "", version = "", description = "";
    public String favicon = "";
    public java.util.List<PlayerSeen> players = new java.util.ArrayList<>(), playerHistory = new java.util.ArrayList<>();
    public boolean playerSampleAvailable;
    public int port = 25565, online, max, protocol;
    public Boolean cracked, whitelisted;
    public long lastSeen, checkedAt, joinedAt, latency;
    public ServerEntry copy() {
        ServerEntry c = new ServerEntry(); c.host=host; c.port=port; c.online=online; c.max=max;
        c.version=version; c.protocol=protocol; c.description=description; c.cracked=cracked;
        c.whitelisted=whitelisted; c.lastSeen=lastSeen; c.joinedAt=joinedAt; c.checkedAt=checkedAt; c.latency=latency;
        c.favicon=favicon; c.playerSampleAvailable=playerSampleAvailable;
        c.players=new java.util.ArrayList<>(players==null?java.util.List.of():players);
        c.playerHistory=new java.util.ArrayList<>(playerHistory==null?java.util.List.of():playerHistory);
        return c;
    }
    public String address() { return host + ":" + port; }
    public boolean valid() { return host != null && host.matches("[0-9.]+") && port > 0 && port <= 65535; }
    public static String ipv4(long ip) {
        if (ip < 0 || ip > 0xffffffffL) throw new IllegalArgumentException(t("IPv4 inválida", "Invalid IPv4 address"));
        return ((ip >>> 24) & 255) + "." + ((ip >>> 16) & 255) + "." + ((ip >>> 8) & 255) + "." + (ip & 255);
    }
}
