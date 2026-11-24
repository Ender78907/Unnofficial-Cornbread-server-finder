package ar.santi.cornbread;

import ar.santi.cornbread.core.*;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.concurrent.*;
import static ar.santi.cornbread.core.Texts.t;

/** Inspects publicly advertised server data; never attempts a login. */
final class InspectorScreen extends Screen {
    private static final ExecutorService WORK=Executors.newFixedThreadPool(2,
            Thread.ofPlatform().daemon().name("cornbread-inspector",0).factory());
    private final Screen parent;
    private final ServerEntry server;
    private ServerInspection.Result result;
    private Future<?> job;
    private int generation,x,y,w;
    private boolean first=true,busy;
    private String status="";
    InspectorScreen(Screen parent,ServerEntry server) {
        super(Component.literal(t("Inspector del servidor","Server Inspector")));
        this.parent=parent;this.server=server;
    }
    @Override protected void init() {
        w=Math.min(450,width-24);x=(width-w)/2;y=Math.max(8,(height-250)/2);
        var refresh=addRenderableWidget(new FinderButton(t("Analizar de nuevo","Inspect again"),x,y+222,(w-6)/2,this::lookup,FinderButton.Style.PRIMARY));
        refresh.active=!busy;
        addRenderableWidget(new FinderButton(t("Volver","Back"),x+(w+6)/2,y+222,(w-6)/2,this::onClose,FinderButton.Style.NORMAL));
        if(first){first=false;lookup();}
    }
    private void lookup() {
        if(busy)return;
        busy=true;status=t("Consultando estado público…","Reading public status…");int token=++generation;
        rebuildWidgets();
        job=WORK.submit(()->{
            try {
                var json=StatusPing.query(server,SharedConstants.getCurrentVersion().protocolVersion());
                var checked=ServerInspection.inspect(json);
                Minecraft.getInstance().execute(()->{
                    if(generation!=token)return;
                    result=checked;busy=false;status=t("Consulta completada (sin iniciar sesión).","Public status received (no login).");rebuildWidgets();
                });
            }catch(Exception e) {
                Minecraft.getInstance().execute(()->{
                    if(generation!=token)return;
                    busy=false;status=t("Sin respuesta de estado: ","Status failed: ")+e.getClass().getSimpleName();rebuildWidgets();
                });
            }
        });
    }
    private void line(GuiGraphicsExtractor g,String label,String value,int offset) {
        String text=label+(value==null?"—":value);
        g.text(font,font.plainSubstrByWidth(text,w),x,y+offset,0xFFE7EDF7);
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta) {
        g.centeredText(font,title,width/2,y,0xFFFFFFFF);
        g.centeredText(font,font.plainSubstrByWidth(server.address(),w),width/2,y+19,0xFFA0A0A0);
        if(result!=null) {
            line(g,t("Software: ","Software: "),result.software(),42);
            line(g,t("Confianza: ","Confidence: "),result.confidence(),59);
            line(g,t("Versión: ","Version: "),result.version(),76);
            line(g,t("Protocolo: ","Protocol: "),result.protocol(),93);
            line(g,t("Mod loader: ","Mod loader: "),result.modLoader(),110);
            line(g,t("Proxy: ","Proxy: "),result.proxy(),127);
            line(g,t("Mods anunciados: ","Announced mods: "),result.mods(),150);
        }
        line(g,"",t("Whitelist y online-mode: no verificables por ping.","Whitelist and online-mode: not detectable by ping."),179);
        line(g,"",t("No se puede distinguir siempre Paper / Vanilla.","Paper / Vanilla cannot always be distinguished."),193);
        line(g,"",status,208);
        super.extractRenderState(g,mx,my,delta);
    }
    @Override public void removed(){generation++;if(job!=null)job.cancel(true);busy=false;}
    @Override public void onClose(){minecraft.gui.setScreen(parent);}
}
