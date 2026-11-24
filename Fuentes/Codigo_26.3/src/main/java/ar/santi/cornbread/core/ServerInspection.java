package ar.santi.cornbread.core;

import com.google.gson.*;
import java.util.*;

/** Conservative assessment of public status fields. No login or private probing. */
public final class ServerInspection {
    private ServerInspection() {}
    public record Result(String software, String confidence, String version, String protocol,
                         String modLoader, String mods, String proxy, String notes) {}
    private static String field(JsonObject object, String key) {
        try { return object.has(key) && object.get(key).isJsonPrimitive() ? object.get(key).getAsString() : ""; }
        catch(RuntimeException ignored) { return ""; }
    }
    private static JsonObject object(JsonObject parent, String key) {
        try {return parent.has(key) && parent.get(key).isJsonObject() ? parent.getAsJsonObject(key) : new JsonObject();}
        catch(RuntimeException ignored) {return new JsonObject();}
    }
    public static Result inspect(JsonObject status) {
        JsonObject v=object(status,"version");
        String name=field(v,"name"), protocol=field(v,"protocol");
        String software="Unknown", loader="Unknown", proxy="Not advertised", confidence="Unknown";
        List<String> advertised=new ArrayList<>();
        boolean forge=status.has("modinfo") || status.has("forgeData");
        JsonObject modinfo=object(status,"modinfo"), forgedata=object(status,"forgeData");
        if(forge) {
            String loaderId=field(modinfo,"type").toLowerCase(Locale.ROOT);
            boolean neo=loaderId.contains("neo") || name.toLowerCase(Locale.ROOT).contains("neoforge");
            loader=neo?"NeoForge / Forge-family":"Forge-family";
            software=loader;confidence="High (modded status payload)";
            addMods(advertised,modinfo,"modList");
            addMods(advertised,forgedata,"mods");
        }
        String lower=name.toLowerCase(Locale.ROOT);
        if(!forge && (lower.contains("fabric") || lower.contains("quilt") || lower.contains("neoforge") || lower.contains("forge"))) {
            software=name; loader=lower.contains("fabric")?"Fabric":lower.contains("quilt")?"Quilt":lower.contains("neo")?"NeoForge":"Forge";
            confidence="Medium (version text)";
        }
        if(lower.contains("velocity") || lower.contains("bungee") || lower.contains("waterfall")) {
            proxy=name; if(!forge) {software="Proxy advertised";confidence="Medium (version text)";}
        }
        if(lower.contains("paper") || lower.contains("purpur") || lower.contains("spigot")) {
            if(!forge) {software=name;confidence="Medium (version text)";}
        }
        if(advertised.isEmpty()) advertised.add("No mod list announced");
        return new Result(software,confidence,name.isBlank()?"Unknown":name,protocol.isBlank()?"Unknown":protocol,
                loader,String.join(", ",advertised),proxy,
                "Paper/Spigot/Vanilla and whitelist/auth usually cannot be identified from public status alone.");
    }
    private static void addMods(List<String> result,JsonObject root,String key) {
        if(!root.has(key) || !root.get(key).isJsonArray())return;
        for(JsonElement element:root.getAsJsonArray(key)) {
            if(result.size()>=30) {result.add("…");break;}
            if(!element.isJsonObject()) continue;
            JsonObject obj=element.getAsJsonObject();
            String id=field(obj,"modid"); if(id.isBlank())id=field(obj,"modId");
            if(id.isBlank())id=field(obj,"id");
            if(!id.isBlank())result.add(id);
        }
    }
}
