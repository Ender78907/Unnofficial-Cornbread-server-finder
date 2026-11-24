package ar.santi.cornbread;
import ar.santi.cornbread.core.*;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.FaviconTexture;
import java.util.*;
final class ServerIcons implements AutoCloseable {
    private record Icon(String data,FaviconTexture texture) {}
    private final Map<String,Icon> icons=new HashMap<>();
    void draw(GuiGraphicsExtractor g,ServerEntry server,int x,int y,int size) {
        Icon icon=icons.get(server.address());
        if(icon==null || !Objects.equals(icon.data(),server.favicon)) {
            if(icon!=null)icon.texture().close();
            if(icons.size()>=200)close();
            var texture=FaviconTexture.forServer(Minecraft.getInstance().getTextureManager(),"cornbread-finder/"+server.address());
            byte[] bytes=ServerIcon.decode(server.favicon);
            if(bytes!=null) {
                NativeImage image=null;
                try {image=NativeImage.read(bytes);texture.upload(image);image=null;}
                catch(Exception ignored) {}
                finally {if(image!=null)image.close();}
            }
            icon=new Icon(server.favicon,texture);icons.put(server.address(),icon);
        }
        g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,icon.texture().textureLocation(),x,y,0,0,size,size,size,size);
    }
    public void close(){icons.values().forEach(i->i.texture().close());icons.clear();}
}
