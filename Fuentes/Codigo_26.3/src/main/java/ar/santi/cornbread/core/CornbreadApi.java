package ar.santi.cornbread.core;

import static ar.santi.cornbread.core.Texts.t;

import com.google.gson.*;
import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;

public final class CornbreadApi {
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    public List<ServerEntry> search(Filters filters, int skip, int protocol) throws IOException, InterruptedException {
        return search(filters,skip,protocol,40);
    }
    public List<ServerEntry> search(Filters filters,int skip,int protocol,int limit) throws IOException,InterruptedException {
        return parse(request(filters.query(skip).replace("limit=40", "limit="+Math.clamp(limit,1,40)) + (filters.compatibleOnly ? "&protocol=" + protocol : "")));
    }
    public List<PlayerSeen> playerHistory(ServerEntry server) throws IOException, InterruptedException {
        long ip=0; for(String part:server.host.split("\\.")) ip=(ip<<8)|Integer.parseInt(part);
        String json=request("ip="+ip+"&port="+server.port+"&limit=1&includePlayers=true");
        try {
            var rows=JsonParser.parseString(json).getAsJsonObject().getAsJsonArray("data");
            if(rows==null) throw new IllegalStateException();
            if(rows.isEmpty()) return List.of();
            var row=rows.get(0).getAsJsonObject();
            if(!ServerEntry.ipv4(row.get("ip").getAsLong()).equals(server.host)||row.get("port").getAsInt()!=server.port) throw new IllegalStateException();
            return PlayerSeen.parse(row.get("playerHistory"),0,true);
        } catch(RuntimeException e) { throw new IOException(t("Historial de jugadores inválido.","Invalid player history."),e); }
    }
    private String request(String query) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder(URI.create("https://api.cornbread2100.com/v2/servers?" + query))
            .timeout(Duration.ofSeconds(25)).header("Accept", "application/json")
            .header("User-Agent", "CornbreadFinder/0.6.8").GET().build();
        HttpResponse<InputStream> response = http.send(req, HttpResponse.BodyHandlers.ofInputStream());
        try (InputStream stream = response.body()) {
            int code = response.statusCode();
            if (code == 429) throw new IOException(t("Cornbread limitó las consultas. Esperá y probá de nuevo.", "Cornbread rate limit reached. Wait and try again."));
            if (code == 401 || code == 403) throw new IOException(t("Cornbread rechazó el acceso (HTTP ", "Cornbread denied access (HTTP ") + code + ").");
            if (code == 503 || code == 504) throw new HttpTimeoutException(t("Cornbread agotó el tiempo de consulta.", "Cornbread query timed out."));
            if (code != 200) throw new IOException(t("Cornbread no respondió correctamente (HTTP ", "Cornbread returned an error (HTTP ") + code + ").");
            byte[] bytes = stream.readNBytes(2_000_001);
            if (bytes.length > 2_000_000) throw new IOException(t("Respuesta demasiado grande de Cornbread.", "Cornbread response is too large."));
            return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
        }
    }
    public static List<ServerEntry> parse(String json) throws IOException {
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            if (!root.has("data") || !root.get("data").isJsonArray()) throw new IOException(t("Cornbread devolvió un formato inesperado.", "Cornbread returned an unexpected format."));
            List<ServerEntry> result = new ArrayList<>();
            for (JsonElement item : root.getAsJsonArray("data")) {
                if (result.size() >= 40) break;
                try {
                    JsonObject o = item.getAsJsonObject();
                    ServerEntry s = new ServerEntry();
                    s.host = ServerEntry.ipv4(o.get("ip").getAsLong()); s.port = o.get("port").getAsInt();
                    s.version = o.getAsJsonObject("version").get("name").getAsString();
                    s.protocol = o.getAsJsonObject("version").get("protocol").getAsInt();
                    s.online = o.getAsJsonObject("players").get("online").getAsInt();
                    s.max = o.getAsJsonObject("players").get("max").getAsInt();
                    s.description = o.has("description") ? plain(o.get("description")) : "";
                    s.lastSeen = o.get("lastSeen").getAsLong();
                    s.cracked = flag(o, "cracked"); s.whitelisted = flag(o, "whitelisted");
                    if (s.valid()) result.add(s);
                } catch (RuntimeException malformedRecord) { /* Skip only the malformed row. */ }
            }
            return result;
        } catch (JsonParseException | IllegalStateException e) { throw new IOException(t("Respuesta JSON inválida de Cornbread.", "Invalid JSON response from Cornbread."), e); }
    }
    private static Boolean flag(JsonObject o, String key) {
        return o.has(key) && !o.get(key).isJsonNull() && o.get(key).isJsonPrimitive()
            && o.getAsJsonPrimitive(key).isBoolean() ? o.get(key).getAsBoolean() : null;
    }
    public static String plain(JsonElement el) {
        if (el == null || el.isJsonNull()) return "";
        if (el.isJsonPrimitive()) return el.getAsString().replaceAll("§.", "").replaceAll("[\\p{Cntrl}]", " ");
        StringBuilder out = new StringBuilder();
        if (el.isJsonArray()) for (JsonElement e : el.getAsJsonArray()) out.append(plain(e));
        else {
            JsonObject obj = el.getAsJsonObject();
            if (obj.has("text")) out.append(plain(obj.get("text")));
            if (obj.has("extra")) out.append(plain(obj.get("extra")));
        }
        return out.toString();
    }
}

