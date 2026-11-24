package ar.santi.cornbread.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import static org.junit.jupiter.api.Assertions.*;

class ManagementTest {
    @TempDir Path temp;
    private ServerEntry server(int port) {
        ServerEntry s=new ServerEntry();s.host="127.0.0.1";s.port=port;s.online=3;s.max=20;s.protocol=999;s.whitelisted=false;return s;
    }
    @Test void blockSurvivesRestartAndUnblockRestoresHistoryAndCounts() {
        Path path=temp.resolve("state.json");FinderStore store=new FinderStore(path);ServerEntry s=server(25565);
        store.addFound(List.of(s));store.joined(s);store.joined(s);store.block(s);
        store=new FinderStore(path);
        assertTrue(store.isBlocked(s));assertTrue(store.unblocked(store.data.found).isEmpty());
        assertTrue(store.unblocked(store.data.history).isEmpty());assertEquals("2",store.countLabel(s));
        store.unblock(s);store=new FinderStore(path);
        assertFalse(store.isBlocked(s));assertEquals(1,store.unblocked(store.data.history).size());
        assertEquals("2",store.countLabel(s));
    }
    @Test void rediscoveryDoesNotUnblockOrResetCountersAndPortsAreIndependent() {
        FinderStore store=new FinderStore(temp.resolve("state.json"));ServerEntry a=server(25565),b=server(25566);
        store.joined(a);store.block(a);store.addFound(List.of(server(25565),b));
        assertEquals(1,store.data.found.size());assertEquals(b.address(),store.data.found.getFirst().address());
        assertTrue(store.isBlocked(a));assertFalse(store.isBlocked(b));assertEquals("1",store.countLabel(a));
        store.unblock(a);store.addFound(List.of(server(25565)));assertEquals("1",store.countLabel(a));
        store.joined(server(25565));assertEquals("2",store.countLabel(a));
    }
    @Test void legacyHistoryGetsHonestMinimumAndMigrationRunsOnce() throws Exception {
        Path path=temp.resolve("old.json");
        Files.writeString(path,"{\"history\":[{\"host\":\"127.0.0.1\",\"port\":25565,\"joinedAt\":123}],\"language\":\"en\"}");
        ServerEntry s=server(25565);FinderStore store=new FinderStore(path);
        assertEquals("≥1",store.countLabel(s));store.joined(s);assertEquals("≥2",store.countLabel(s));
        store=new FinderStore(path);assertEquals("≥2",store.countLabel(s));assertEquals("en",store.data.language);
    }
    @Test void compactPreferenceAndBlocksAreNotEvictedByHistoryLimit() {
        Path path=temp.resolve("state.json");FinderStore store=new FinderStore(path);
        store.data.density=2;
        for(int i=0;i<205;i++){ServerEntry s=server(10000+i);store.data.blocked.put(s.address(),s);}
        store.save();store=new FinderStore(path);
        assertEquals(205,store.data.blocked.size());assertTrue(store.isBlocked(server(10000)));assertEquals(2,store.data.density);
    }
    @Test @Timeout(5) void fastRepliesArePublishedBeforeSlowPeersAndBlocksAreNeverPinged() throws Exception {
        ExecutorService executor=Executors.newFixedThreadPool(2);
        try {
            CountDownLatch firstPublished=new CountDownLatch(1);List<Integer> order=new ArrayList<>();AtomicInteger probes=new AtomicInteger();
            var result=ServerVerifier.verify(executor,(s,p)->{
                probes.incrementAndGet();
                if(s.port==1&&!firstPublished.await(2,TimeUnit.SECONDS))throw new IllegalStateException("Fast reply not delivered");
                return s;
            },List.of(server(1),server(2),server(3)),new Filters(),999,Set.of(server(3).address()),()->false,update->{
                assertEquals(2,update.total());order.add(update.match().port);firstPublished.countDown();
            });
            assertEquals(List.of(2,1),order);assertEquals(2,result.size());assertEquals(2,probes.get());
        } finally {executor.shutdownNow();}
    }
    @Test @Timeout(5) void cancellationStopsFurtherResultDelivery() {
        ExecutorService executor=Executors.newFixedThreadPool(2);AtomicBoolean cancelled=new AtomicBoolean();AtomicInteger delivered=new AtomicInteger();
        try {
            assertThrows(InterruptedException.class,()->ServerVerifier.verify(executor,(s,p)->s,List.of(server(1),server(2)),new Filters(),999,Set.of(),cancelled::get,
                update->{delivered.incrementAndGet();cancelled.set(true);}));
            assertEquals(1,delivered.get());
        } finally {executor.shutdownNow();}
    }
}
