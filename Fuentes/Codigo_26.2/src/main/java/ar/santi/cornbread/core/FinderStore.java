package ar.santi.cornbread.core;

import static ar.santi.cornbread.core.Texts.t;

import com.google.gson.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public final class FinderStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final class Data {
        public String language = "es";
        public int density;
        public boolean retryFailedAutoJoin;
        public boolean retryWhitelist;
        public Map<String, ServerLocation> locations = new LinkedHashMap<>();
        public Map<String, ServerEntry> favorites = new LinkedHashMap<>();
        public Map<String, String> aliases = new LinkedHashMap<>();
        public Map<String, ServerEntry> blocked = new LinkedHashMap<>();
        public Map<String, Visits> visits = new LinkedHashMap<>();
        public Filters filters = new Filters();
        public Map<String, FailedVisit> failures = new LinkedHashMap<>();
        public List<ServerEntry> found = new ArrayList<>(), history = new ArrayList<>();
    }
    public static final class FailedVisit {
        public ServerEntry server;
        public long count,lastAttempt;
        public String reason="";
    }
    public static final class Visits {
        public long count, lastJoined;
        public boolean includesLegacy;
    }
    public final Path path;
    public Data data = new Data();
    public String warning = "";
    public FinderStore(Path path) { this.path = path; load(); }
    private void load() {
        if (!Files.exists(path)) return;
        try (Reader reader = Files.newBufferedReader(path)) {
            Data loaded = GSON.fromJson(reader, Data.class);
            if (loaded == null) throw new IOException(t("Archivo vacío", "Empty file"));
            data = loaded;
            data.language = "en".equals(data.language) ? "en" : "es";
            Texts.setLanguage(data.language);
            data.density = Math.max(0, Math.min(2, data.density));
            if (data.filters == null) data.filters = new Filters();
            data.filters.maxPlayers = Math.max(-1,Math.min(1_000_000,data.filters.maxPlayers));
            data.filters.minPlayers = Math.max(0, Math.min(1_000_000, data.filters.minPlayers));
            if (data.filters.whitelist < 0 || data.filters.whitelist > 2) data.filters.whitelist = 0;
            if (data.filters.cracked < 0 || data.filters.cracked > 2) data.filters.cracked = 0;
            data.found = sanitize(data.found); data.history = sanitize(data.history);
            if(data.failures==null)data.failures=new LinkedHashMap<>();
            data.failures.entrySet().removeIf(e->e.getValue()==null||e.getValue().server==null||!e.getValue().server.valid()||!e.getKey().equals(e.getValue().server.address()));
            if (data.blocked == null) data.blocked = new LinkedHashMap<>();
            data.blocked.entrySet().removeIf(e -> e.getValue() == null || !e.getValue().valid() || !e.getKey().equals(e.getValue().address()));
            if(data.locations==null)data.locations=new LinkedHashMap<>();
            data.locations.values().removeIf(java.util.Objects::isNull);
            if(data.favorites==null)data.favorites=new LinkedHashMap<>();
            data.favorites.entrySet().removeIf(e->e.getValue()==null||!e.getValue().valid()||!e.getKey().equals(e.getValue().address()));
            if(data.aliases==null)data.aliases=new LinkedHashMap<>();
            data.aliases.replaceAll((key,value)->cleanAlias(value));
            data.aliases.values().removeIf(String::isEmpty);
            if (data.visits == null) data.visits = new LinkedHashMap<>();
            data.visits.entrySet().removeIf(e -> e.getValue() == null || e.getValue().count < 0);
            for (ServerEntry s : data.history) if (!data.visits.containsKey(s.address())) {
                Visits v = new Visits(); v.count = 1; v.lastJoined = s.joinedAt; v.includesLegacy = true;
                data.visits.put(s.address(), v);
            }
        } catch (IOException | RuntimeException e) {
            data = new Data(); warning = t("No se pudo leer el historial anterior.", "Could not read the previous history.");
            try { Files.copy(path, path.resolveSibling(path.getFileName() + ".corrupt-" + System.currentTimeMillis())); }
            catch (IOException ignored) { warning += t(" No se pudo crear copia.", " Could not create a backup."); }
        }
    }
    private static List<ServerEntry> sanitize(List<ServerEntry> entries) {
        List<ServerEntry> list = new ArrayList<>();
        if (entries != null) for (ServerEntry s : entries) if (s != null && s.valid() && list.size() < 200) list.add(s);
        return list;
    }
    public void addFound(List<ServerEntry> entries) {
        stageFound(entries); save();
    }
    public void stageFound(List<ServerEntry> entries) {
        for (int i = entries.size() - 1; i >= 0; i--) {
            ServerEntry s = entries.get(i);
            ServerEntry old = known(s.address());
            if(old!=null) s.playerHistory=PlayerSeen.merge(old.playerHistory,s.playerHistory);
            if (isBlocked(s)) continue;
            Visits v = data.visits.get(s.address()); if (v != null) s.joinedAt = v.lastJoined;
            putFirst(data.found, s);
            if(isFavorite(s))data.favorites.put(s.address(),s.copy());
            for (int j=0;j<data.history.size();j++) if (data.history.get(j).address().equals(s.address())) data.history.set(j,s);
        }
    }
    public boolean isFavorite(ServerEntry s) { return data.favorites.containsKey(s.address()); }
    public void toggleFavorite(ServerEntry s) {
        if(isFavorite(s))data.favorites.remove(s.address());else data.favorites.put(s.address(),s.copy());
        save();
    }
    public List<ServerEntry> favorites() { return unblocked(new ArrayList<>(data.favorites.values())); }
    public String alias(ServerEntry s) { return data.aliases.getOrDefault(s.address(), ""); }
    public String displayName(ServerEntry s) { String name=alias(s);return name.isEmpty()?s.address():name; }
    public void setAlias(ServerEntry s,String name) {
        String value=cleanAlias(name);
        if(value.isEmpty())data.aliases.remove(s.address());else data.aliases.put(s.address(),value);
        save();
    }
    private static String cleanAlias(String name) {
        if(name==null)return "";
        String clean=name.replaceAll("[\\p{Cntrl}§]", "").strip();
        return clean.substring(0,Math.min(48,clean.length()));
    }
    public boolean isBlocked(ServerEntry s) { return data.blocked.containsKey(s.address()); }
    public void block(ServerEntry s) { data.blocked.put(s.address(), s); save(); }
    public void unblock(ServerEntry s) { data.blocked.remove(s.address()); save(); }
    public List<ServerEntry> unblocked(List<ServerEntry> entries) { return entries.stream().filter(s -> !isBlocked(s)).toList(); }
    public String countLabel(ServerEntry s) {
        Visits v = data.visits.get(s.address());
        return v == null ? "0" : (v.includesLegacy ? "≥" : "") + v.count;
    }
    public Visits visits(ServerEntry s) { return data.visits.get(s.address()); }
    public ServerEntry known(String address) {
        for (ServerEntry s : data.found) if (s.address().equals(address)) return s;
        for (ServerEntry s : data.history) if (s.address().equals(address)) return s;
        ServerEntry favorite=data.favorites.get(address);
        if(favorite!=null)return favorite;
        ServerEntry blocked=data.blocked.get(address);
        if(blocked!=null)return blocked;
        FailedVisit failure=data.failures.get(address);return failure==null?null:failure.server;
    }
    public void mergePlayers(ServerEntry s, List<PlayerSeen> history) {
        s.playerHistory=PlayerSeen.merge(s.playerHistory,history);
        for(ServerEntry entry:data.found) if(entry.address().equals(s.address())) entry.playerHistory=PlayerSeen.merge(entry.playerHistory,s.playerHistory);
        for(ServerEntry entry:data.history) if(entry.address().equals(s.address())) entry.playerHistory=PlayerSeen.merge(entry.playerHistory,s.playerHistory);
        save();
    }
    public void failed(ServerEntry server,String reason) {
        FailedVisit old=data.failures.remove(server.address());
        FailedVisit f=new FailedVisit();f.server=server.copy();f.count=old==null?1:old.count+1;
        f.lastAttempt=System.currentTimeMillis();f.reason=reason==null?"":reason.substring(0,Math.min(2000,reason.length()));
        data.failures.put(server.address(),f);
        while(data.failures.size()>200)data.failures.remove(data.failures.keySet().iterator().next());
        save();
    }
    public List<ServerEntry> failedServers() {
        return data.failures.values().stream().sorted(Comparator.comparingLong((FailedVisit f)->f.lastAttempt).reversed())
            .map(f->f.server).filter(s->!isBlocked(s)).toList();
    }
    public void joined(ServerEntry s) {
        ServerEntry old=known(s.address());
        if(old!=null)s.playerHistory=PlayerSeen.merge(old.playerHistory,s.playerHistory);
        Visits v = data.visits.computeIfAbsent(s.address(), ignored -> new Visits());
        v.count++; v.lastJoined = System.currentTimeMillis(); s.joinedAt = v.lastJoined;
        putFirst(data.history, s); if(isFavorite(s))data.favorites.put(s.address(),s.copy()); save();
    }
    private static void putFirst(List<ServerEntry> list, ServerEntry s) {
        list.removeIf(old -> old.address().equals(s.address())); list.addFirst(s);
        while (list.size() > 200) list.removeLast();
    }
    public void save() {
        try {
            Files.createDirectories(path.getParent());
            Path temp = path.resolveSibling(path.getFileName() + ".tmp");
            Files.writeString(temp, GSON.toJson(data));
            try { Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException e) { Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING); }
            warning = "";
        } catch (IOException e) { warning = t("No se pudo guardar el historial en disco.", "Could not save history to disk."); }
    }
}
