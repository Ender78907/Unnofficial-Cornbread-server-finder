package ar.santi.cornbread;
import ar.santi.cornbread.core.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.concurrent.*;
import static ar.santi.cornbread.core.Texts.t;

final class LocationScreen extends Screen {
    private static final ExecutorService WORK=Executors.newSingleThreadExecutor(Thread.ofPlatform().daemon().name("cornbread-location").factory());
    private static long nextRequest;
    private final Screen parent;
    private final ServerEntry server;
    private ServerLocation location;
    private Future<?> job;
    private boolean first=true,busy;
    private int generation,x,w,y;
    private String status="";
    LocationScreen(Screen parent,ServerEntry server) {
        super(Component.literal(t("Ubicación aproximada","Approximate location")));
        this.parent=parent;this.server=server;
        location=FinderClient.STORE.data.locations.get(server.host);
    }
    @Override protected void init() {
        w=Math.min(420,width-24);x=(width-w)/2;y=Math.max(8,(height-220)/2);
        var refresh=addRenderableWidget(new FinderButton(t("Actualizar","Refresh"),x,y+190,(w-4)/2,()->lookup(),FinderButton.Style.NORMAL));refresh.active=!busy;
        addRenderableWidget(new FinderButton(t("Volver","Back"),x+(w+4)/2,y+190,(w-4)/2,this::onClose,FinderButton.Style.NORMAL));
        if(first){first=false;if(location==null||!location.fresh(System.currentTimeMillis()))lookup();else status=t("Datos guardados (hasta 7 días).","Cached data (up to 7 days).");}
    }
    private void lookup() {
        if(busy)return;
        if(System.currentTimeMillis()<nextRequest){status=t("Esperá unos segundos y tocá Actualizar.","Wait a few seconds, then press Refresh.");return;}
        nextRequest=System.currentTimeMillis()+5000;busy=true;status=t("Consultando ipwho.is…","Loading from ipwho.is…");int token=++generation;rebuildWidgets();
        job=WORK.submit(()->{
            try {
                var result=ServerLocation.lookup(server.host);
                Minecraft.getInstance().execute(()->{
                    if(generation!=token)return;
                    location=result;busy=false;
                    var cache=FinderClient.STORE.data.locations;cache.remove(server.host);cache.put(server.host,result);
                    while(cache.size()>500)cache.remove(cache.keySet().iterator().next());
                    FinderClient.STORE.save();status=t("Datos aproximados de ipwho.is.","Approximate data from ipwho.is.");rebuildWidgets();
                });
            }catch(Exception e){Minecraft.getInstance().execute(()->{
                if(generation!=token)return;busy=false;status=location==null?t("No disponible. Probá más tarde.","Unavailable. Try again later."):t("Falló la consulta; mostrando datos anteriores.","Lookup failed; showing previous data.");rebuildWidgets();
            });}
        });
    }
    private void line(GuiGraphicsExtractor g,String label,String value,int offset) {
        g.text(font,font.plainSubstrByWidth(label+(value==null||value.isBlank()?t("No disponible","Unavailable"):value),w),x,y+offset,0xFFFFFFFF);
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta) {
        g.centeredText(font,title,width/2,y,0xFFFFFFFF);
        g.centeredText(font,server.address(),width/2,y+18,0xFFA0A0A0);
        line(g,t("País: ","Country: "),location==null?null:location.country(),42);
        line(g,t("Región: ","Region: "),location==null?null:location.region(),60);
        line(g,t("Ciudad: ","City: "),location==null?null:location.city(),78);
        line(g,t("Proveedor: ","Provider: "),location==null?null:location.provider(),96);
        g.text(font,font.plainSubstrByWidth(t("Es la ubicación de la IP, no una dirección física exacta.","IP location, not an exact physical address."),w),x,y+122,0xFFA0A0A0);
        g.text(font,font.plainSubstrByWidth(t("Puede corresponder a un proxy o protección DDoS.","May identify a proxy or DDoS protection service."),w),x,y+136,0xFFA0A0A0);
        g.text(font,font.plainSubstrByWidth(t("Consulta externa: se envía la IP del servidor a ipwho.is.","External lookup: sends the server IP to ipwho.is."),w),x,y+150,0xFF808080);
        g.text(font,font.plainSubstrByWidth(status,w),x,y+172,0xFFFFD682);
        super.extractRenderState(g,mx,my,delta);
    }
    @Override public void removed(){generation++;if(job!=null)job.cancel(true);busy=false;}
    @Override public void onClose(){minecraft.gui.setScreen(parent);}
}
