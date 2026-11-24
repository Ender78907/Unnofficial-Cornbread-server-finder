package ar.santi.cornbread.core;

import static ar.santi.cornbread.core.Texts.t;

import com.google.gson.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

/** Standard multiplayer status request; never sends a login or probes authentication. */
public final class StatusPing {
    /** Reads only the server's public status JSON. No login, whitelist or auth probes. */
    public static JsonObject query(ServerEntry entry, int clientProtocol) throws IOException {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(entry.host, entry.port), 2500);
            socket.setSoTimeout(2500);
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            DataInputStream in = new DataInputStream(socket.getInputStream());
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            DataOutputStream handshake = new DataOutputStream(buf);
            writeVarInt(handshake, 0); writeVarInt(handshake, clientProtocol);
            byte[] hostname = entry.host.getBytes(StandardCharsets.UTF_8);
            writeVarInt(handshake, hostname.length); handshake.write(hostname);
            handshake.writeShort(entry.port); writeVarInt(handshake, 1);
            writeVarInt(out, buf.size()); out.write(buf.toByteArray());
            out.writeByte(1); out.writeByte(0); out.flush();
            int length = readVarInt(in);
            if (length < 2 || length > 1_048_576) throw new IOException("Invalid status packet");
            byte[] packet = in.readNBytes(length);
            if (packet.length != length) throw new EOFException();
            DataInputStream data = new DataInputStream(new ByteArrayInputStream(packet));
            if (readVarInt(data) != 0) throw new IOException("Unexpected packet ID");
            int size = readVarInt(data);
            if (size < 2 || size > data.available()) throw new IOException("Incomplete status packet");
            try { return JsonParser.parseString(new String(data.readNBytes(size), StandardCharsets.UTF_8)).getAsJsonObject(); }
            catch(RuntimeException e) { throw new IOException("Invalid status JSON", e); }
        }
    }
    public static ServerEntry check(ServerEntry entry, int clientProtocol) throws IOException {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(entry.host, entry.port), 2500);
            socket.setSoTimeout(2500);
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            DataInputStream in = new DataInputStream(socket.getInputStream());
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            DataOutputStream handshake = new DataOutputStream(buf);
            writeVarInt(handshake, 0); writeVarInt(handshake, clientProtocol);
            byte[] hostname = entry.host.getBytes(StandardCharsets.UTF_8);
            writeVarInt(handshake, hostname.length); handshake.write(hostname);
            handshake.writeShort(entry.port); writeVarInt(handshake, 1);
            writeVarInt(out, buf.size()); out.write(buf.toByteArray());
            out.writeByte(1); out.writeByte(0); out.flush();
            long start = System.nanoTime();
            int length = readVarInt(in);
            if (length < 2 || length > 1_048_576) throw new IOException(t("Respuesta de estado inválida", "Invalid status response"));
            byte[] packet = in.readNBytes(length);
            if (packet.length != length) throw new EOFException();
            DataInputStream data = new DataInputStream(new ByteArrayInputStream(packet));
            if (readVarInt(data) != 0) throw new IOException(t("No es un paquete de estado", "Not a status packet"));
            int size = readVarInt(data);
            if (size < 2 || size > data.available()) throw new IOException(t("Estado incompleto", "Incomplete status response"));
            try {
                JsonObject status = JsonParser.parseString(new String(data.readNBytes(size), StandardCharsets.UTF_8)).getAsJsonObject();
                entry.online = status.getAsJsonObject("players").get("online").getAsInt();
                entry.max = status.getAsJsonObject("players").get("max").getAsInt();
                entry.protocol = status.getAsJsonObject("version").get("protocol").getAsInt();
                entry.version = status.getAsJsonObject("version").get("name").getAsString();
                entry.description = CornbreadApi.plain(status.get("description"));
                entry.playerSampleAvailable = status.getAsJsonObject("players").has("sample") && status.getAsJsonObject("players").get("sample").isJsonArray();
                entry.players = PlayerSeen.parse(status.getAsJsonObject("players").get("sample"), System.currentTimeMillis(), false);
                entry.playerHistory = PlayerSeen.merge(entry.playerHistory, entry.players);
                entry.favicon = "";
                if(status.has("favicon") && status.get("favicon").isJsonPrimitive()) {
                    String icon=status.get("favicon").getAsString();
                    if(ServerIcon.decode(icon)!=null) entry.favicon=icon;
                }
                entry.latency = (System.nanoTime() - start) / 1_000_000;
                entry.checkedAt = System.currentTimeMillis();
                return entry;
            } catch (RuntimeException e) { throw new IOException(t("Estado de Minecraft inválido", "Invalid Minecraft status"), e); }
        }
    }
    public static void writeVarInt(DataOutputStream out, int value) throws IOException {
        do { int b = value & 0x7f; value >>>= 7; out.writeByte(value != 0 ? b | 0x80 : b); } while (value != 0);
    }
    public static int readVarInt(DataInputStream in) throws IOException {
        int value = 0;
        for (int i = 0; i < 5; i++) {
            int b = in.readUnsignedByte(); value |= (b & 127) << (i * 7);
            if ((b & 128) == 0) return value;
        }
        throw new IOException(t("VarInt demasiado largo", "VarInt too long"));
    }
}
