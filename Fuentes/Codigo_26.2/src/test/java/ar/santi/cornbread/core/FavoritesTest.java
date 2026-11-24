package ar.santi.cornbread.core;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class FavoritesTest {
 @TempDir Path temp;
 ServerEntry server(int port){ServerEntry s=new ServerEntry();s.host="127.0.0.1";s.port=port;return s;}
 @Test void favoriteAndAliasSurviveEvictionRestartAndBlocking(){
  Path p=temp.resolve("state.json");FinderStore store=new FinderStore(p);ServerEntry s=server(25565);
  store.toggleFavorite(s);store.setAlias(s,"Mi servidor");store.addFound(List.of(s));store.data.found.clear();store.save();
  store=new FinderStore(p);assertTrue(store.isFavorite(s));assertEquals("Mi servidor",store.displayName(s));assertNotNull(store.known(s.address()));
  store.block(s);assertTrue(store.favorites().isEmpty());store.unblock(s);assertEquals(1,store.favorites().size());
  store.toggleFavorite(s);assertTrue(store.favorites().isEmpty());assertEquals("Mi servidor",store.alias(s));
 }
 @Test void updatesRetainIdentityAndPortsStayIndependent(){
  FinderStore store=new FinderStore(temp.resolve("state.json"));ServerEntry s=server(25565),other=server(25566);
  store.toggleFavorite(s);store.setAlias(s,"Home");ServerEntry fresh=server(25565);fresh.online=8;store.addFound(List.of(fresh));
  assertEquals(8,store.favorites().getFirst().online);assertEquals("Home",store.displayName(fresh));assertFalse(store.isFavorite(other));assertEquals(other.address(),store.displayName(other));
 }
 @Test void legacyAndNullFieldsMigrateAndAliasesCanBeCleared() throws Exception {
  Path p=temp.resolve("state.json");Files.writeString(p,"{\"favorites\":null,\"aliases\":null}");FinderStore store=new FinderStore(p);ServerEntry s=server(25565);
  assertTrue(store.favorites().isEmpty());store.setAlias(s,"  Hello\nWorld§  ");assertEquals("HelloWorld",store.alias(s));
  store.setAlias(s,"x".repeat(80));assertEquals(48,store.alias(s).length());store.setAlias(s,"   ");assertEquals(s.address(),store.displayName(s));
 }
}
