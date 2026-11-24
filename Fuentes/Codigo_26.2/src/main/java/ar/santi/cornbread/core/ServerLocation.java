package ar.santi.cornbread.core;

import com.google.gson.*;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public record ServerLocation(String country,String region,String city,String provider,long checkedAt) {
    public boolean fresh(long now) { return checkedAt>0 && now>=checkedAt && now-checkedAt<7L*24*60*60*1000; }
    private static String field(JsonObject obj,String key) {
        if(obj==null||!obj.has(key)||!obj.get(key).isJsonPrimitive()||!obj.get(key).getAsJsonPrimitive().isString())return "";
        String value=obj.get(key).getAsString().replaceAll("[\\p{Cntrl}§]", "");
        return value.substring(0,Math.min(160,value.length()));
    }
    public static ServerLocation parse(String json,String ip,long now) throws IOException {
        try {
            JsonObject obj=JsonParser.parseString(json).getAsJsonObject();
            if(!obj.has("success")||!obj.get("success").getAsBoolean()||!ip.equals(field(obj,"ip")))throw new IOException("Location unavailable");
            JsonObject connection=obj.has("connection")&&obj.get("connection").isJsonObject()?obj.getAsJsonObject("connection"):null;
            String provider=field(connection,"isp");if(provider.isEmpty())provider=field(connection,"org");
            return new ServerLocation(field(obj,"country"),field(obj,"region"),field(obj,"city"),provider,now);
        }catch(RuntimeException e){throw new IOException("Invalid location response",e);}
    }
    public static ServerLocation lookup(String ip) throws Exception {
        if(!ip.matches("[0-9]{1,3}(\\.[0-9]{1,3}){3}"))throw new IOException("Invalid IP");
        try(HttpClient client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()) {
            var request=HttpRequest.newBuilder(URI.create("https://ipwho.is/"+ip+"?fields=success,ip,country,region,city,connection"))
                .timeout(Duration.ofSeconds(10)).header("User-Agent","CornbreadFinder/0.6.6").build();
            var response=client.send(request,HttpResponse.BodyHandlers.ofInputStream());
            try(var input=response.body()) {
                if(response.statusCode()!=200)throw new IOException("HTTP "+response.statusCode());
                byte[] body=input.readNBytes(16385);if(body.length>16384)throw new IOException("Response too large");
                return parse(new String(body,StandardCharsets.UTF_8),ip,System.currentTimeMillis());
            }
        }
    }
}
