package ar.santi.cornbread;

import ar.santi.cornbread.core.ServerEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.nbt.NbtIo;
import java.io.IOException;
import java.nio.file.Files;

final class VanillaServers {
    enum Result { ADDED, EXISTS }
    static boolean sameAddress(String candidate, String expected) {
        ServerAddress a=ServerAddress.parseString(candidate), b=ServerAddress.parseString(expected);
        return a.getHost().equalsIgnoreCase(b.getHost()) && a.getPort()==b.getPort();
    }
    static Result add(Minecraft client, ServerEntry entry) throws IOException {
        var path=client.gameDirectory.toPath().resolve("servers.dat");
        // Vanilla load() logs failures instead of throwing. Validate before modifying the list.
        if(Files.exists(path)) {
            var original=NbtIo.read(path);
            if(original==null || original.getList("servers").isEmpty()) throw new IOException("Invalid servers.dat");
        }
        ServerList list=new ServerList(client);list.load();
        for(int i=0;i<list.size();i++) if(sameAddress(list.get(i).ip,entry.address())) return Result.EXISTS;
        ServerData unhidden=list.unhide(entry.address());
        if(unhidden==null && entry.port==25565) unhidden=list.unhide(entry.host);
        if(unhidden==null) list.add(new ServerData("Cornbread · "+entry.address(),entry.address(),ServerData.Type.OTHER),false);
        list.save();
        // save() also logs errors internally, so only announce success if the file contains the entry.
        var saved=NbtIo.read(path);
        boolean exists=saved!=null && saved.getListOrEmpty("servers").compoundStream().anyMatch(tag ->
            !tag.getBooleanOr("hidden",false) && sameAddress(tag.getStringOr("ip",""),entry.address()));
        if(!exists) throw new IOException("Server list was not saved");
        return Result.ADDED;
    }
}
