package ar.santi.cornbread.core;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.*;
class LocationTest {
 @TempDir Path temp;
 @Test void parseAndMissingFields() throws Exception {
  var result=ServerLocation.parse("{\"success\":true,\"ip\":\"8.8.8.8\",\"country\":\"US\",\"connection\":{\"org\":\"Google\"}}","8.8.8.8",100);
  assertEquals("US",result.country());assertEquals("",result.city());assertEquals("Google",result.provider());
 }
 @Test void rejectsFailureAndWrongIp() {
  assertThrows(IOException.class,()->ServerLocation.parse("{\"success\":false}","8.8.8.8",100));
  assertThrows(IOException.class,()->ServerLocation.parse("{\"success\":true,\"ip\":\"1.1.1.1\"}","8.8.8.8",100));
  assertThrows(IOException.class,()->ServerLocation.parse("not json","8.8.8.8",100));
 }
 @Test void cacheExpiryAndPersistence() {
  var value=new ServerLocation("US","California","City","ISP",100);
  assertTrue(value.fresh(101));assertFalse(value.fresh(99));assertFalse(value.fresh(100+7L*24*60*60*1000));
  Path p=temp.resolve("state.json");var store=new FinderStore(p);store.data.locations.put("8.8.8.8",value);store.save();
  assertEquals(value,new FinderStore(p).data.locations.get("8.8.8.8"));
 }
 @Test void oldConfigLoadsWithoutLocations() throws Exception {
  Path p=temp.resolve("old.json");Files.writeString(p,"{\"locations\":null}");assertTrue(new FinderStore(p).data.locations.isEmpty());
 }
}
