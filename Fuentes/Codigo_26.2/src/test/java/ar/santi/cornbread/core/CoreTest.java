package ar.santi.cornbread.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.io.*;
import java.net.*;
import java.util.List;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class CoreTest {
    @TempDir Path temp;
    @org.junit.jupiter.api.AfterEach void resetLanguage() { Texts.setLanguage("es"); }
    @Test void languagePersistsAndLegacyConfigStillLoads() throws Exception {
        Path path = temp.resolve("language.json");
        FinderStore store = new FinderStore(path);
        store.data.language = "en"; store.save();
        FinderStore loaded = new FinderStore(path);
        assertEquals("en", loaded.data.language);
        assertEquals("Search", Texts.t("Buscar", "Search"));
        Files.writeString(path, "{\"filters\":{\"minPlayers\":5},\"found\":[],\"history\":[]}");
        FinderStore legacy = new FinderStore(path);
        assertEquals("es", legacy.data.language);
        assertEquals(5, legacy.data.filters.minPlayers);
        assertEquals("Buscar", Texts.t("Buscar", "Search"));
        Texts.setLanguage("invalid"); assertEquals("es", Texts.language());
    }

    @Test void unsignedIpAndRealApiShape() throws Exception {
        String fixture = """
            {"data":[{"ip":4294967295,"port":25565,"lastSeen":"1791452495",
            "version":{"name":"26.2","protocol":999},"players":{"online":5,"max":20},
            "description":"§aHola","cracked":null,"whitelisted":false}, {"bad":true}]}
            """;
        var entries = CornbreadApi.parse(fixture);
        assertEquals(1, entries.size()); assertEquals("255.255.255.255", entries.getFirst().host);
        assertNull(entries.getFirst().cracked); assertEquals("Hola", entries.getFirst().description);
        assertThrows(IOException.class, () -> CornbreadApi.parse("{\"error\":\"quota\"}"));
    }
    @Test void unknownFlagsNeverMatchExplicitChoices() {
        Filters f = new Filters(); ServerEntry s = new ServerEntry(); s.online = 3;
        assertFalse(f.matches(s)); s.whitelisted = false; assertTrue(f.matches(s));
        f.cracked = 1; assertFalse(f.matches(s)); s.cracked = true; assertTrue(f.matches(s));
        s.online = 0; assertFalse(f.matches(s));
        assertTrue(f.query(40).contains("whitelisted=false")); assertTrue(f.query(40).contains("cracked=true"));
    }
    @Test void historyPersistsAndDeduplicates() {
        Path path = temp.resolve("state.json"); FinderStore store = new FinderStore(path);
        ServerEntry s = new ServerEntry(); s.host = "127.0.0.1";
        store.addFound(List.of(s)); assertEquals(0, store.data.history.size());
        store.joined(s); store.joined(s);
        FinderStore loaded = new FinderStore(path);
        assertEquals(1, loaded.data.history.size()); assertTrue(loaded.data.history.getFirst().joinedAt > 0);
        assertEquals(1, loaded.data.found.size());
    }
    @Test void corruptStorageGetsBackup() throws Exception {
        Path path = temp.resolve("state.json"); Files.writeString(path, "{broken");
        FinderStore store = new FinderStore(path); assertFalse(store.warning.isEmpty());
        try (var files = Files.list(temp)) { assertTrue(files.anyMatch(p -> p.getFileName().toString().contains(".corrupt-"))); }
    }
    @Test void statusPingUsesLiveCountsAndHandlesFragmentation() throws Exception {
        try (ServerSocket server = new ServerSocket(0)) {
            var serving = CompletableFuture.runAsync(() -> {
                try (Socket socket = server.accept()) {
                    DataInputStream in = new DataInputStream(socket.getInputStream());
                    in.readNBytes(StatusPing.readVarInt(in));
                    assertEquals(1, in.readUnsignedByte()); assertEquals(0, in.readUnsignedByte());
                    byte[] json = "{\"players\":{\"online\":7,\"max\":30,\"sample\":[{\"name\":\"Alex\",\"id\":\"sample-id\"}]},\"version\":{\"name\":\"26.2\",\"protocol\":999},\"description\":{\"text\":\"Hola\",\"extra\":[{\"text\":\" mundo\"}]}}".getBytes(java.nio.charset.StandardCharsets.UTF_8);
                    ByteArrayOutputStream buffer = new ByteArrayOutputStream(); DataOutputStream p = new DataOutputStream(buffer);
                    StatusPing.writeVarInt(p, 0); StatusPing.writeVarInt(p, json.length); p.write(json);
                    DataOutputStream out = new DataOutputStream(socket.getOutputStream()); StatusPing.writeVarInt(out, buffer.size());
                    for (byte b : buffer.toByteArray()) { out.write(b); out.flush(); }
                } catch (IOException e) { throw new UncheckedIOException(e); }
            });
            ServerEntry s = new ServerEntry(); s.host = "127.0.0.1"; s.port = server.getLocalPort(); s.online = 100;
            StatusPing.check(s, 999); serving.get(5, TimeUnit.SECONDS);
            assertTrue(s.playerSampleAvailable); assertEquals("Alex",s.players.getFirst().name()); assertEquals(1,s.playerHistory.size());
            assertEquals(7, s.online); assertEquals("Hola mundo", s.description); assertTrue(s.checkedAt > 0);
        }
    }
    @Test void invalidVarIntIsBounded() {
        assertThrows(IOException.class, () -> StatusPing.readVarInt(new DataInputStream(new ByteArrayInputStream(new byte[]{-128,-128,-128,-128,-128}))));
    }
}
