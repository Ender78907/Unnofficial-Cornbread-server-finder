package ar.santi.cornbread.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.jupiter.api.Assertions.*;

class ConnectionHistoryTest {
    @TempDir Path temp;
    private ServerEntry server(int port){ServerEntry s=new ServerEntry();s.host="127.0.0.1";s.port=port;s.online=5;s.protocol=1;return s;}
    @Test void failuresPersistSeparatelyAndSuccessDoesNotEraseThem(){
        FinderStore store=new FinderStore(temp.resolve("history.json"));ServerEntry s=server(25565);
        store.failed(s,"Timed out");store.failed(s,"Whitelist");
        assertTrue(store.data.history.isEmpty());assertEquals("0",store.countLabel(s));
        store.joined(s);FinderStore loaded=new FinderStore(store.path);
        assertEquals(1,loaded.data.history.size());assertEquals("1",loaded.countLabel(s));
        assertEquals(2,loaded.data.failures.get(s.address()).count);
        assertEquals("Whitelist",loaded.data.failures.get(s.address()).reason);
        assertTrue(loaded.data.failures.get(s.address()).lastAttempt>0);
    }
    @Test void failuresRespectBlocksAndPortsAndDoNotCreateLegacyCounts(){
        FinderStore store=new FinderStore(temp.resolve("history.json"));ServerEntry a=server(25565),b=server(25566);
        store.failed(a,"Timeout");store.failed(b,"Refused");store.block(a);
        assertEquals(List.of(b.address()),store.failedServers().stream().map(ServerEntry::address).toList());
        store.unblock(a);FinderStore loaded=new FinderStore(store.path);
        assertEquals(2,loaded.failedServers().size());assertTrue(loaded.data.visits.isEmpty());
    }
    @Test void savedResultSnapshotAndCheckTimeSurviveRestart(){
        FinderStore store=new FinderStore(temp.resolve("history.json"));ServerEntry s=server(25565);s.checkedAt=1000;
        store.addFound(List.of(s));store.failed(s,"Offline later");
        FinderStore loaded=new FinderStore(store.path);
        assertEquals(1,loaded.data.found.size());assertEquals(1000,loaded.data.found.getFirst().checkedAt);
        assertEquals(5,loaded.data.found.getFirst().online);
    }
    @Test void failureHistoryIsBoundedAndOldConfigLoads()throws Exception{
        Path file=temp.resolve("history.json");Files.writeString(file,"{\"found\":[],\"history\":[]}");
        FinderStore store=new FinderStore(file);assertTrue(store.data.failures.isEmpty());
        for(int i=0;i<205;i++)store.failed(server(25000+i),"Error");
        assertEquals(200,store.data.failures.size());assertFalse(store.data.failures.containsKey(server(25000).address()));
    }
    @Test void firstVerifiedMatchCanCancelSlowChecksImmediately()throws Exception{
        ExecutorService workers=Executors.newFixedThreadPool(2);AtomicBoolean cancelled=new AtomicBoolean();
        CountDownLatch slowStarted=new CountDownLatch(1),release=new CountDownLatch(1);
        Filters filters=new Filters();filters.whitelist=0;
        try {
            assertThrows(InterruptedException.class,()->ServerVerifier.verify(workers,(s,p)->{
                if(s.port==25566){slowStarted.countDown();release.await();return s;}
                assertTrue(slowStarted.await(2,TimeUnit.SECONDS));return s;
            },List.of(server(25565),server(25566)),filters,1,Set.of(),cancelled::get,
                update->{if(update.match()!=null)cancelled.set(true);}));
            assertEquals(1,release.getCount());
        } finally {release.countDown();workers.shutdownNow();}
    }
}
