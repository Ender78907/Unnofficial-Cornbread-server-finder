package ar.santi.cornbread;

import ar.santi.cornbread.core.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import static ar.santi.cornbread.core.Texts.t;

/** Actions stay off the compact rows, leaving space for server details. */
final class ServerActionsScreen extends Screen {
    private final FinderScreen parent;
    private final ServerEntry server;
    private final Runnable blockAction;
    private String message="";
    private int x,y,w;
    ServerActionsScreen(FinderScreen parent,ServerEntry server,Runnable blockAction) {
        super(Component.literal(t("Opciones del servidor", "Server options")));
        this.parent=parent;this.server=server;this.blockAction=blockAction;
    }
    private void button(String text,int top,Runnable action,FinderButton.Style style) {
        addRenderableWidget(new FinderButton(text,x+12,top,w-24,action,style));
    }
    private void halfButton(String text,int top,boolean right,Runnable action) {
        int half=(w-28)/2;
        addRenderableWidget(new FinderButton(text,x+12+(right?half+4:0),top,half,action,FinderButton.Style.NORMAL));
    }
    @Override protected void init() {
        w=Math.min(width-24,360);x=(width-w)/2;y=Math.max(4,(height-252)/2);
        button(t("Copiar IP y puerto", "Copy IP and port"),y+58,()->{
            try {minecraft.keyboardHandler.setClipboard(server.address());message=t("IP copiada: ", "Copied: ")+server.address();}
            catch(RuntimeException e){message=t("No se pudo copiar la IP.", "Could not copy the address.");}
        },FinderButton.Style.NORMAL);
        button(t("Guardar en Minecraft", "Save to Minecraft"),y+82,()->{
            try {
                var result=VanillaServers.add(minecraft,server);
                if(result==VanillaServers.Result.ADDED) parent.vanillaSaved();
                message=result==VanillaServers.Result.ADDED?t("Guardado en la lista de Multijugador.", "Saved to the Multiplayer server list.")
                    :t("Ese servidor ya está en tu lista.", "That server is already in your list.");
            }catch(Exception e){message=t("No se pudo guardar. Revisá latest.log y servers.dat.", "Could not save. Check latest.log and servers.dat.");}
        },FinderButton.Style.PRIMARY);
        halfButton(t("Jugadores online", "Online players"),y+106,false,()->minecraft.gui.setScreen(new PlayersScreen(this,server,false)));
        halfButton(t("Jugadores anteriores", "Past players"),y+106,true,()->minecraft.gui.setScreen(new PlayersScreen(this,server,true)));
        halfButton(t("Bloquear servidor", "Block server"),y+154,false,()->{
            blockAction.run();minecraft.gui.setScreen(parent);
        });
        halfButton(t("Ubicación", "Location"),y+154,true,()->minecraft.gui.setScreen(new LocationScreen(this,server)));
        halfButton(FinderClient.STORE.isFavorite(server)?t("Quitar favorito", "Unfavorite"):t("Añadir favorito", "Favorite"),y+130,false,()->{
            FinderClient.STORE.toggleFavorite(server);rebuildWidgets();
        });
        halfButton(t("Editar alias", "Edit alias"),y+130,true,()->minecraft.gui.setScreen(new AliasScreen(this,server)));
        button(t("Inspeccionar servidor", "Inspect server"),y+196,()->minecraft.gui.setScreen(new InspectorScreen(this,server)),FinderButton.Style.PRIMARY);
        button(t("Volver", "Back"),y+226,this::onClose,FinderButton.Style.QUIET);
    }
    @Override public void onClose(){minecraft.gui.setScreen(parent);}
    @Override public void extractBackground(GuiGraphicsExtractor g,int mx,int my,float delta){
        super.extractBackground(g,mx,my,delta);
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta){
        g.centeredText(font,title,width/2,y+13,0xFFF1F6FC);
        g.centeredText(font,font.plainSubstrByWidth(server.address(),w-24),width/2,y+29,0xFFFFFFFF);
        g.centeredText(font,t("Conexiones exitosas: ", "Successful joins: ")+FinderClient.STORE.countLabel(server),width/2,y+44,0xFFA0A0A0);
        g.centeredText(font,font.plainSubstrByWidth(message,w-24),width/2,y+181,0xFFFFD682);
        super.extractRenderState(g,mx,my,delta);
    }
}
