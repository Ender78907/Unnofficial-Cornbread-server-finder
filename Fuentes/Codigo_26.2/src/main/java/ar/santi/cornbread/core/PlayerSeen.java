package ar.santi.cornbread.core;
import com.google.gson.*;
import java.util.*;
/** Server-reported observations, not an authoritative membership list. */
public record PlayerSeen(String name, String id, long lastSeen) {
    /** Names only, one per line, preserving list order and omitting duplicate names. */
    public static String namesForClipboard(List<PlayerSeen> players) {
        Map<String,String> names=new LinkedHashMap<>();
        if(players!=null)for(PlayerSeen player:players) {
            if(player==null||player.name()==null)continue;
            String name=player.name().replaceAll("§.","").replaceAll("[\\p{Cntrl}]","").strip();
            if(!name.isEmpty())names.putIfAbsent(name.toLowerCase(Locale.ROOT),name);
        }
        return String.join("\n",names.values());
    }
    public String key() { return id != null && !id.isBlank() && !id.equals("00000000-0000-0000-0000-000000000000") ? id.toLowerCase(Locale.ROOT) : name.toLowerCase(Locale.ROOT); }
    public static List<PlayerSeen> parse(JsonElement array, long observedAt, boolean historical) {
        List<PlayerSeen> result=new ArrayList<>();
        if(array==null || !array.isJsonArray()) return result;
        for(JsonElement el:array.getAsJsonArray()) {
            if(result.size()>=500) break;
            try {
                JsonObject o=el.getAsJsonObject();
                String name=CornbreadApi.plain(o.get("name"));if(name.isBlank()) continue;
                name=name.substring(0,Math.min(64,name.length()));
                String id=o.has("id")&&!o.get("id").isJsonNull()?o.get("id").getAsString():"";
                if(id.length()>64) id="";
                long seen=observedAt;
                if(historical) { long sec=o.get("lastSession").getAsLong();seen=sec>0&&sec<=Long.MAX_VALUE/1000?sec*1000:0; }
                result.add(new PlayerSeen(name,id,seen));
            } catch(RuntimeException ignored) { }
        }
        return merge(List.of(),result);
    }
    public static List<PlayerSeen> merge(List<PlayerSeen> older,List<PlayerSeen> newer) {
        Map<String,PlayerSeen> all=new HashMap<>();
        for(List<PlayerSeen> list:Arrays.asList(older,newer)) if(list!=null) for(PlayerSeen p:list) {
            if(p==null||p.name()==null||p.name().isBlank())continue;
            all.merge(p.key(),p,(a,b)->a.lastSeen()>b.lastSeen()?a:b);
        }
        return all.values().stream().sorted(Comparator.comparingLong(PlayerSeen::lastSeen).reversed().thenComparing(PlayerSeen::name)).limit(500).toList();
    }
}
