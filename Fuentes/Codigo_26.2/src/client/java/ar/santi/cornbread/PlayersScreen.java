package ar.santi.cornbread;
import ar.santi.cornbread.core.*;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import static ar.santi.cornbread.core.Texts.t;

final class PlayersScreen extends Screen {
    private static final ExecutorService WORK=Executors.newFixedThreadPool(2,Thread.ofPlatform().daemon().name("cornbread-players-",0).factory());
    private final Screen parent;
    private ServerEntry server;
    private boolean past,first=true,busy,onlineConfirmed;
    private int page,rows,x,w,generation;
    private long nextRefresh;
    private Future<?> job;
    private String status="";
    PlayersScreen(Screen parent,ServerEntry server,boolean past) {
        super(Component.literal(t("Jugadores del servidor","Server players")));
        this.parent=parent;var saved=FinderClient.STORE.known(server.address());this.server=(saved==null?server:saved).copy();this.past=past;
    }
    private FinderButton button(String text,int bx,int by,int bw,Runnable action,boolean enabled,boolean selected) {
        var b=addRenderableWidget(new FinderButton(text,bx,by,bw,action,selected?FinderButton.Style.SELECTED:FinderButton.Style.NORMAL));b.active=enabled;return b;
    }
    @Override protected void init() {
        w=Math.min(width-24,540);x=(width-w)/2;rows=Math.max(1,(height-130)/24);
        int half=(w-6)/2;
        button("Online",x,42,half,()->change(false),true,!past);
        button(t("Anteriores","Past players"),x+half+6,42,half,()->change(true),true,past);
        page=Math.min(page,Math.max(0,(entries().size()-1)/rows));
        List<PlayerSeen> list=entries();
        for(int i=page*rows;i<Math.min(list.size(),(page+1)*rows);i++) {
            PlayerSeen player=list.get(i);int y=84+(i-page*rows)*24;
            button(t("Copiar","Copy"),x+w-64,y,60,()->copyNames(List.of(player),false),true,false)
                .setTooltip(Tooltip.create(Component.literal(t("Copiar nombre: ","Copy name: ")+player.name())));
        }
        button("<",x,height-30,20,()->{page--;rebuildWidgets();},page>0,false);
        button(">",x+72,height-30,20,()->{page++;rebuildWidgets();},(page+1)*rows<entries().size(),false);
        int bw=(w-112)/3;
        button(t("Actualizar","Refresh"),x+100,height-30,bw,this::refresh,!busy,false);
        button(bw<80?t("Copiar","Copy"):t("Copiar todos","Copy all"),x+106+bw,height-30,bw,()->copyNames(entries(),true),!PlayerSeen.namesForClipboard(entries()).isEmpty(),false)
            .setTooltip(Tooltip.create(Component.literal(past?
                t("Copia todos los nombres de Anteriores, de todas las páginas, uno por línea y sin repetir.","Copies all Past players names across all pages, one per line with duplicates removed."):
                t("Copia todos los nombres disponibles en Online, de todas las páginas. Puede ser una muestra parcial o guardada.","Copies all available names in Online across all pages. The sample may be partial or saved."))));
        button(t("Volver","Back"),x+112+2*bw,height-30,bw,this::onClose,true,false);
        if(first){first=false;refresh();}
    }
    private List<PlayerSeen> entries(){return past?server.playerHistory:server.players;}
    private void copyNames(List<PlayerSeen> players,boolean all) {
        String names=PlayerSeen.namesForClipboard(players);
        if(names.isEmpty()){status=t("No hay nombres para copiar.","No names to copy.");return;}
        try {
            minecraft.keyboardHandler.setClipboard(names);
            status=all?t("Nombres copiados: ","Names copied: ")+names.lines().count():t("Nombre copiado: ","Name copied: ")+names;
        } catch(RuntimeException e) {status=t("No se pudieron copiar los nombres.","Could not copy names.");}
    }
    private void change(boolean history){if(past==history)return;cancel();past=history;page=0;nextRefresh=0;refresh();}
    private void cancel(){generation++;if(job!=null)job.cancel(true);busy=false;}
    private void refresh() {
        if(busy)return;
        if(System.currentTimeMillis()<nextRefresh){status=t("Esperá unos segundos para actualizar.","Wait a few seconds before refreshing.");return;}
        nextRefresh=System.currentTimeMillis()+5000;
        boolean historical=past;ServerEntry snapshot=server.copy();int token=++generation;busy=true;
        if(!historical)onlineConfirmed=false;
        status=t("Consultando…","Loading…");rebuildWidgets();
        job=WORK.submit(()->{
            try {
                List<PlayerSeen> history=historical?new CornbreadApi().playerHistory(snapshot):null;
                ServerEntry checked=historical?null:StatusPing.check(snapshot,SharedConstants.getCurrentVersion().protocolVersion());
                Minecraft.getInstance().execute(()->{
                    if(generation!=token)return;busy=false;
                    if(historical){FinderClient.STORE.mergePlayers(server,history);status=t("Cornbread + observaciones locales (hasta 500).","Cornbread + local observations (up to 500).");}
                    else {
                        server=checked;onlineConfirmed=true;FinderClient.STORE.addFound(List.of(server));
                        status=server.playerSampleAvailable?t("Nombres publicados: ","Published names: ")+server.players.size()+" · Online: "+server.online
                            :t("El servidor no publica nombres de jugadores.","The server does not publish player names.");
                    }
                    rebuildWidgets();
                });
            }catch(Exception e){Minecraft.getInstance().execute(()->{
                if(generation!=token)return;busy=false;
                status=historical?t("Falló Cornbread; se muestran datos guardados.","Cornbread failed; showing saved observations."):t("Sin respuesta; datos guardados, no online confirmado.","No response; saved data, not confirmed online.");rebuildWidgets();
            });}
        });
    }
    @Override public void tick(){super.tick();if(onlineConfirmed&&System.currentTimeMillis()-server.checkedAt>=60000)onlineConfirmed=false;}
    @Override public void removed(){cancel();}
    @Override public void onClose(){minecraft.gui.setScreen(parent);}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta) {
        g.centeredText(font,title,width/2,10,0xFFFFFFFF);
        g.centeredText(font,font.plainSubstrByWidth(server.address(),w),width/2,25,0xFFA0A0A0);
        String note=past?t("Última vez visto; puede incluir jugadores actuales.","Last seen; may include current players.")
            :onlineConfirmed?t("Muestra del servidor; puede ser parcial o personalizada.","Server sample; may be partial or customized."):t("Última muestra guardada; pendiente de confirmar.","Last saved sample; awaiting confirmation.");
        g.text(font,font.plainSubstrByWidth(note,w),x,69,0xFFA0A0A0);
        List<PlayerSeen> list=entries();
        for(int i=page*rows;i<Math.min(list.size(),(page+1)*rows);i++) {
            PlayerSeen p=list.get(i);int y=84+(i-page*rows)*24;
            g.fill(x,y-2,x+w,y+21,0x80000000);
            g.text(font,font.plainSubstrByWidth(p.name(),w-76),x+5,y,0xFFFFFFFF);
            String date=p.lastSeen()<=0?t("Fecha desconocida","Date unknown"):DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").format(Instant.ofEpochMilli(p.lastSeen()).atZone(ZoneId.systemDefault()));
            g.text(font,font.plainSubstrByWidth(date,w-76),x+5,y+11,0xFF808080);
        }
        if(list.isEmpty())g.centeredText(font,t("No hay nombres disponibles.","No player names available."),width/2,94,0xFFA0A0A0);
        g.text(font,font.plainSubstrByWidth(status,w),x,height-44,0xFFA0A0A0);
        g.centeredText(font,(page+1)+" / "+Math.max(1,(list.size()+rows-1)/rows),x+46,height-24,0xFFFFFFFF);
        super.extractRenderState(g,mx,my,delta);
    }
}
