package ar.santi.cornbread.core;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.ByteBuffer;
import java.nio.file.*;
import java.net.http.HttpTimeoutException;
import java.util.*;
import java.util.concurrent.atomic.*;
import static org.junit.jupiter.api.Assertions.*;
class NewFeaturesTest {
    @TempDir Path temp;
    private ServerEntry entry(int id){var s=new ServerEntry();s.host="127.0.0."+id;s.online=id;s.protocol=1;return s;}
    @Test void maximumIsInclusiveAndCanBeUnlimited(){
        Filters f=new Filters();f.whitelist=0;f.minPlayers=2;f.maxPlayers=4;
        assertFalse(f.matches(entry(1)));assertTrue(f.matches(entry(2)));assertTrue(f.matches(entry(4)));assertFalse(f.matches(entry(5)));
        assertTrue(f.query(0).contains("maxPlayers=4"));assertEquals(4,f.copy().maxPlayers);
        f.maxPlayers=-1;assertTrue(f.matches(entry(255)));assertFalse(f.query(0).contains("maxPlayers="));
    }
    @Test void oldConfigurationDefaultsToNoMaximum()throws Exception{
        Path p=temp.resolve("old.json");Files.writeString(p,"{\"filters\":{\"minPlayers\":2}}");
        assertEquals(-1,new FinderStore(p).data.filters.maxPlayers);
    }
    @Test void moreResultsContinuesPastFortyRejectedCandidates()throws Exception{
        List<Integer> offsets=new ArrayList<>();AtomicInteger emitted=new AtomicInteger();
        var result=MatchedSearch.run((skip,limit)->{
            offsets.add(skip);List<ServerEntry> page=new ArrayList<>();for(int i=skip+1;i<=skip+limit;i++)page.add(entry(i));return page;
        },(list,progress)->{for(var s:list)progress.accept(new ServerVerifier.Progress(0,0,s.online>40?s:null));},0,Set.of(),()->false,p->{if(p.match()!=null)emitted.incrementAndGet();},n->{},n->{},ms->{});
        assertEquals(List.of(0,40),offsets);assertEquals(40,result.matches().size());assertEquals(40,emitted.get());assertEquals(80,result.nextSkip());
    }
    @Test void retriesTimedOutPageWithoutDroppingAlreadyFound()throws Exception{
        AtomicInteger attempt=new AtomicInteger(),retries=new AtomicInteger();List<Integer> offsets=new ArrayList<>();
        var result=MatchedSearch.run((skip,limit)->{
            offsets.add(skip);if(skip==40&&attempt.getAndIncrement()==0)throw new HttpTimeoutException("test timeout");
            List<ServerEntry> page=new ArrayList<>();for(int i=skip+1;i<=skip+limit;i++)page.add(entry(i));return page;
        },(list,progress)->{for(var s:list)progress.accept(new ServerVerifier.Progress(0,0,s.online%2==0?s:null));},0,Set.of(),()->false,p->{},n->{},retries::set,ms->{});
        assertEquals(40,result.matches().size());assertEquals(1,retries.get());assertEquals(40,offsets.get(1));assertEquals(40,offsets.get(2));
        assertEquals(40,result.matches().stream().map(ServerEntry::address).distinct().count());
    }
    @Test void excludedAndDuplicateServersDoNotCountAndEmptyPageStops()throws Exception{
        AtomicInteger calls=new AtomicInteger();
        var result=MatchedSearch.run((skip,limit)->calls.getAndIncrement()==0?List.of(entry(1),entry(2),entry(2)):List.of(),
            (list,progress)->list.forEach(s->progress.accept(new ServerVerifier.Progress(0,0,s))),0,Set.of(entry(1).address()),()->false,p->{},n->{},n->{},ms->{});
        assertEquals(1,result.matches().size());assertTrue(result.exhausted());assertEquals(2,calls.get());
    }
    @Test void cancellationStopsTimeoutRetries(){
        AtomicBoolean cancelled=new AtomicBoolean();AtomicInteger calls=new AtomicInteger();
        assertThrows(InterruptedException.class,()->MatchedSearch.run((skip,limit)->{calls.incrementAndGet();throw new HttpTimeoutException("test timeout");},
            (list,progress)->{},0,Set.of(),cancelled::get,p->{},n->{},n->{},ms->cancelled.set(true)));
        assertEquals(1,calls.get());
    }
    @Test void historyDatesMalformedRecordsAndDeduplication(){
        var rows=PlayerSeen.parse(JsonParser.parseString("""
        [{"name":"Alex","id":"uuid","lastSession":"1791452495"},{"name":"§aSteve","lastSession":1791452496},null,{"bad":true}]
        """),0,true);
        assertEquals(2,rows.size());assertEquals("Steve",rows.getFirst().name());assertEquals(1791452495000L,rows.get(1).lastSeen());
        var merged=PlayerSeen.merge(rows,List.of(new PlayerSeen("NewName","uuid",1791452497000L)));
        assertEquals(2,merged.size());assertEquals("NewName",merged.getFirst().name());
    }
    @Test void refreshPreservesPlayerHistoryWithoutAddingVisits(){
        FinderStore store=new FinderStore(temp.resolve("state.json"));var s=entry(1);
        s.playerHistory=List.of(new PlayerSeen("Alex","id",100));store.addFound(List.of(s));
        var update=entry(1);update.players=List.of(new PlayerSeen("Steve","id2",200));update.playerHistory=update.players;store.addFound(List.of(update));
        store.mergePlayers(update,List.of(new PlayerSeen("Alex","id",50)));
        var loaded=new FinderStore(store.path);assertEquals(2,loaded.data.found.getFirst().playerHistory.size());
        assertEquals(100,loaded.data.found.getFirst().playerHistory.get(1).lastSeen());assertTrue(loaded.data.history.isEmpty());assertEquals("0",loaded.countLabel(update));
    }
    @Test void onlyBounded64PixelPngHeadersAccepted(){
        byte[] bytes=ByteBuffer.allocate(24).putLong(0x89504e470d0a1a0aL).putInt(13).putInt(0x49484452).putInt(64).putInt(64).array();
        String prefix="data:image/png;base64,";assertArrayEquals(bytes,ServerIcon.decode(prefix+Base64.getEncoder().encodeToString(bytes)));
        ByteBuffer.wrap(bytes).putInt(16,65535);assertNull(ServerIcon.decode(prefix+Base64.getEncoder().encodeToString(bytes)));
        assertNull(ServerIcon.decode(prefix+"!bad!"));assertNull(ServerIcon.decode(prefix+"A".repeat(65536)));assertNull(ServerIcon.decode(null));
    }
}
