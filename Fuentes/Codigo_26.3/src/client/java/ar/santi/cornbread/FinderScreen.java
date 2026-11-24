package ar.santi.cornbread;

import static ar.santi.cornbread.core.Texts.t;

import ar.santi.cornbread.core.*;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.Component;
import java.util.*;
import java.time.*;
import java.time.format.DateTimeFormatter;

public final class FinderScreen extends Screen {
    private final Screen parent;
    private final ServerIcons icons = new ServerIcons();
    private final SearchSession session = new SearchSession();
    private final Filters filters = FinderClient.STORE.data.filters;
    private final List<ServerEntry> live = FinderClient.STORE.data.found;
    private final Set<String> failedThisSearch = new HashSet<>();
    private final List<FinderButton> actions = new ArrayList<>();
    private final List<ServerEntry> shown = new ArrayList<>();
    private EditBox minimum, maximum;
    private boolean favoritesTab, history, failedTab, blockedTab, vanillaListChanged, firstInit = true;
    private int page, skip, left, contentWidth, rows, ticks;
    private int top, filterX, filterY, filterW, resultX, resultW, tabY, listY, rowHeight;
    private boolean wide, compact, dense, showFilters;
    private final List<Label> labels = new ArrayList<>();
    private record Label(String text, int x, int y) {}
    private String status = t("Elegí los filtros y buscá servidores.", "Choose your filters and find servers.");
    private String lastQuery = "";
    private static String choice(int value) { return value == 0 ? t("Cualquiera", "Any") : value == 1 ? t("Sí", "Yes") : t("No", "No"); }
    public FinderScreen(Screen parent) { super(Component.literal("Cornbread Finder")); this.parent = parent; }
    private int protocol() { return SharedConstants.getCurrentVersion().protocolVersion(); }
    private FinderButton button(String text, int x, int y, int w, Runnable action, boolean busyLock) {
        return button(text,x,y,w,action,busyLock,FinderButton.Style.NORMAL);
    }
    private FinderButton button(String text, int x, int y, int w, Runnable action, boolean busyLock, FinderButton.Style style) {
        FinderButton b = addRenderableWidget(new FinderButton(text,x,y,w,action,style));
        if (busyLock) { actions.add(b); b.active = !session.busy; }
        return b;
    }
    private void label(String text,int x,int y) { labels.add(new Label(text,x,y)); }
    @Override protected void init() {
        actions.clear(); shown.clear(); labels.clear();
        dense = FinderClient.STORE.data.density > 0 || height < 260;
        showFilters = FinderClient.STORE.data.density != 2;
        wide = width >= 660 && height >= 440 && showFilters;
        compact = height < 330;
        contentWidth = Math.min(width - 20, wide ? 940 : 720);
        left = (width - contentWidth) / 2; top = compact ? 10 : 20;
        button(Texts.isEnglish() ? "English" : "Español",left+contentWidth-84,top-3,84,()->{
            Texts.setLanguage(Texts.isEnglish()?"es":"en");
            FinderClient.STORE.data.language=Texts.language();
            save();status=t("Elegí los filtros y buscá servidores.","Choose your filters and find servers.");
            rebuildWidgets();
        },true).setTooltip(Tooltip.create(Component.literal("Español / English")));
        String densityLabel = switch (FinderClient.STORE.data.density) {
            case 1 -> t("Compacta", "Compact"); case 2 -> t("Lista", "List"); default -> t("Normal", "Normal");
        };
        button(densityLabel,left+contentWidth-174,top-3,84,()->{
            FinderClient.STORE.data.density=(FinderClient.STORE.data.density+1)%3;save();page=0;rebuildWidgets();
        },false).setTooltip(Tooltip.create(Component.literal(t("Vista: Normal → Compacta → Lista. Lista oculta los filtros para mostrar más servidores.", "View: Normal → Compact → List. List hides filters to show more servers."))));
        filterX = left; filterY = top + (compact ? 27 : 40); filterW = wide ? 196 : contentWidth;
        resultX = wide ? left + 210 : left; resultW = wide ? contentWidth - 210 : contentWidth;
        int actionY;
        minimum = null; maximum = null;
        if (showFilters) {
        int x=filterX+10, w=filterW-20;
        int col=(w-12)/3;
        int controlsY=filterY+(wide?34:15);
        int minimumW=wide?w:col;
        label(t("MÍN. / MÁX.", "MIN. / MAX."),x,controlsY-11);
        minimum = addRenderableWidget(new EditBox(font,x,controlsY,(minimumW-4)/2,20,Component.literal(t("Cantidad mínima de jugadores", "Minimum player count"))));
        minimum.setBordered(true); minimum.setTextColor(0xFFE5EDF6);
        minimum.setMaxLength(7); minimum.setValue(Integer.toString(filters.minPlayers)); minimum.setEditable(!session.busy);
        minimum.setResponder(value -> {
            if (value.matches("\\d{1,7}")) filters.minPlayers = Math.min(1_000_000,Integer.parseInt(value));
        });
        maximum=addRenderableWidget(new EditBox(font,x+(minimumW-4)/2+4,controlsY,(minimumW-4)/2,20,Component.literal(t("Máximo de jugadores (vacío: sin límite)","Maximum players (empty: unlimited)"))));
        maximum.setMaxLength(7);maximum.setValue(filters.maxPlayers<0?"":Integer.toString(filters.maxPlayers));maximum.setEditable(!session.busy);
        maximum.setResponder(value->{if(value.isBlank())filters.maxPlayers=-1;else if(value.matches("\\d{1,7}"))filters.maxPlayers=Math.min(1_000_000,Integer.parseInt(value));});
        maximum.setHint(Component.literal("∞"));maximum.setTooltip(Tooltip.create(Component.literal(t("Máximo de jugadores online. Vacío = sin límite.","Maximum online players. Empty = unlimited."))));
        int wx=wide?x:x+col+6, wy=wide?controlsY+44:controlsY;
        int cx=wide?x:x+2*(col+6), cy=wide?controlsY+88:controlsY;
        label("WHITELIST",wx,wy-11); label("CRACKED",cx,cy-11);
        button(choice(filters.whitelist),wx,wy,wide?w:col,()->{
            filters.whitelist=(filters.whitelist+1)%3;save();rebuildWidgets();
        },true).setTooltip(Tooltip.create(Component.literal(t("Según Cornbread. Sí / No excluye los estados desconocidos.", "Based on Cornbread data. Yes / No excludes unknown states."))));
        button(choice(filters.cracked),cx,cy,wide?w:col,()->{
            filters.cracked=(filters.cracked+1)%3;save();rebuildWidgets();
        },true).setTooltip(Tooltip.create(Component.literal(t("Cracked: servidor en offline-mode. Este filtro no modifica tu cuenta.", "Cracked: an offline-mode server. This filter does not change your account."))));
        actionY=wide?controlsY+122:controlsY+28;
        button(wide?t("Misma versión: ", "Same version: ")+(filters.compatibleOnly?t("Sí", "Yes"):t("Cualquiera", "Any")) : t("Compatible: ", "Compatible: ")+(filters.compatibleOnly?t("Sí", "Yes"):t("Todos", "Any")),x,actionY,wide?w:col,()->{
            filters.compatibleOnly=!filters.compatibleOnly;save();rebuildWidgets();
        },true).setTooltip(Tooltip.create(Component.literal(t("Exige el mismo protocolo. Algunos proxies admiten tu versión aunque anuncien otra.", "Requires the same protocol. Some proxies support your version while advertising another."))));
        button(t("Buscar", "Search"),wide?x:x+col+6,wide?actionY+36:actionY,wide?w:col,()->search(false,false),true);
        button(t("Buscar y entrar", "Search & join"),wide?x:x+2*(col+6),wide?actionY+64:actionY,wide?w:col,()->search(true,false),true,FinderButton.Style.PRIMARY);
        } else {
            actionY=filterY;
            int col=(contentWidth-12)/3;
            button(t("Filtros", "Filters"),left,actionY,col,()->{
                FinderClient.STORE.data.density=1;save();rebuildWidgets();
            },false);
            button(t("Buscar", "Search"),left+col+6,actionY,col,()->search(false,false),true);
            button(t("Buscar y entrar", "Search & join"),left+2*(col+6),actionY,col,()->search(true,false),true,FinderButton.Style.PRIMARY);
        }
        int recoveryY=wide?actionY+92:actionY+(showFilters?28:26);
        int recoveryHeight=recoveryOptions(wide?filterX+10:left+10,recoveryY,wide?filterW-20:contentWidth-20);
        tabY=wide?filterY:recoveryY+recoveryHeight+8; listY=tabY+(showFilters?30:26);
        int tabW=Math.min(126,(resultW-25)/6);
        button(resultW<380?t("Lista","Found"):t("Encontrados", "Found"),resultX,tabY,tabW,()->{favoritesTab=false;history=false;failedTab=false;blockedTab=false;page=0;rebuildWidgets();},false,!favoritesTab&&!history&&!failedTab&&!blockedTab?FinderButton.Style.SELECTED:FinderButton.Style.TAB);
        button(t("Historial", "History"),resultX+tabW+5,tabY,tabW,()->{favoritesTab=false;history=true;failedTab=false;blockedTab=false;page=0;rebuildWidgets();},false,history?FinderButton.Style.SELECTED:FinderButton.Style.TAB);
        button(t("Fallidos","Failed"),resultX+2*(tabW+5),tabY,tabW,()->{favoritesTab=false;history=false;failedTab=true;blockedTab=false;page=0;rebuildWidgets();},false,failedTab?FinderButton.Style.SELECTED:FinderButton.Style.TAB);
        button(t("Bloqueados", "Blocked"),resultX+3*(tabW+5),tabY,tabW,()->{favoritesTab=false;history=false;failedTab=false;blockedTab=true;page=0;rebuildWidgets();},false,blockedTab?FinderButton.Style.SELECTED:FinderButton.Style.TAB);
        button(resultW<500?"★":t("Favoritos", "Favorites"),resultX+4*(tabW+5),tabY,tabW,()->{favoritesTab=true;history=false;failedTab=false;blockedTab=false;page=0;rebuildWidgets();},false,favoritesTab?FinderButton.Style.SELECTED:FinderButton.Style.TAB)
            .setTooltip(Tooltip.create(Component.literal(t("Favoritos", "Favorites"))));
        FinderButton refreshButton=button(t("Actualizar", "Refresh"),resultX+resultW-tabW,tabY,tabW,this::refresh,true);
        refreshButton.active=!session.busy&&!blockedTab&&!failedTab;
        refreshButton.setTooltip(Tooltip.create(Component.literal(t("Comprueba hasta 200 guardados con 12 consultas simultáneas. Los bloqueados se omiten.", "Checks up to 200 saved servers with 12 concurrent requests. Blocked servers are skipped."))));
        rowHeight=dense?28:compact?44:56;
        rows=Math.max(1,(height-58-listY)/rowHeight);
        List<ServerEntry> list=visible(); page=Math.min(page,Math.max(0,(list.size()-1)/rows));
        for(int i=page*rows;i<Math.min(list.size(),(page+1)*rows);i++) {
            ServerEntry entry=list.get(i);shown.add(entry);int y=listY+(i-page*rows)*rowHeight;
            int by=y+(rowHeight-26)/2;
            if(blockedTab) {
                button(t("Desbloquear", "Unblock"),resultX+resultW-84,by,78,()->{
                    FinderClient.STORE.unblock(entry);
                    status=t("Desbloqueado. Actualizá o buscá para comprobarlo.", "Unblocked. Refresh or search to check it.");rebuildWidgets();
                },false);
            } else {
                button(t("Entrar", "Join"),resultX+resultW-108,by,48,()->connect(entry),false,FinderButton.Style.PRIMARY)
                    .setTooltip(Tooltip.create(Component.literal(details(entry))));
                button("×",resultX+resultW-56,by,20,()->block(entry),false)
                    .setTooltip(Tooltip.create(Component.literal(t("Bloquear servidor", "Block server"))));
                button("…",resultX+resultW-30,by,24,()->minecraft.gui.setScreen(new ServerActionsScreen(this,entry,()->block(entry))),false)
                    .setTooltip(Tooltip.create(Component.literal(t("Opciones: jugadores online, anteriores, copiar IP, guardar o bloquear.", "Options: online players, past players, copy address, save or block."))));
            }
        }
        int bottom=height-30;
        FinderButton prev=button("<",left,bottom,24,()->{page--;rebuildWidgets();},false);prev.active=page>0;
        FinderButton next=button(">",left+72,bottom,24,()->{page++;rebuildWidgets();},false);next.active=(page+1)*rows<list.size();
        button(t("Más resultados", "More results"),left+104,bottom,Math.max(80,contentWidth-200),()->search(false,true),true);
        button(session.busy?t("Cancelar", "Cancel"):t("Volver", "Back"),left+contentWidth-88,bottom,88,()->{
            if(session.busy){session.cancel();status=t("Búsqueda cancelada.", "Search cancelled.");rebuildWidgets();}else onClose();
        },false);
        if(firstInit) {
            firstInit=false;
            if(!FinderClient.STORE.warning.isEmpty())status=FinderClient.STORE.warning;

        }
    }
    private int recoveryOptions(int x,int y,int w) {
        boolean stacked=w<260;
        boolean shortLabels=!stacked&&w<400;
        int boxWidth=stacked?w:(w-12)/2;
        Checkbox retry=addRenderableWidget(Checkbox.builder(Component.literal(shortLabels?t("Bloquear y seguir","Block & retry"):t("Bloquear y reintentar","Block & retry failures")),font)
            .pos(x,y).maxWidth(boxWidth).selected(FinderClient.STORE.data.retryFailedAutoJoin)
            .tooltip(Tooltip.create(Component.literal(t("Solo Buscar y entrar: si falla la conexión, bloquea ese servidor y busca otro con los mismos filtros. Podés cancelar durante 3 segundos.","Search & join only: if joining fails, block that server and find another using the same filters. You have 3 seconds to cancel."))))
            .onValueChange((box,checked)->{FinderClient.STORE.data.retryFailedAutoJoin=checked;save();rebuildWidgets();}).build());
        Checkbox whitelist=addRenderableWidget(Checkbox.builder(Component.literal(shortLabels?t("También whitelist","Whitelist too"):t("También por whitelist","Include whitelist errors")),font)
            .pos(stacked?x:x+boxWidth+12,stacked?y+retry.getHeight()+6:y).maxWidth(boxWidth).selected(FinderClient.STORE.data.retryWhitelist)
            .tooltip(Tooltip.create(Component.literal(t("Con Bloquear y reintentar activado: también bloquea y busca otro si el rechazo es por whitelist. Desmarcado: se detiene sin bloquear.","With Block & retry enabled: also block and find another after a whitelist rejection. Unchecked: stop without blocking."))))
            .onValueChange((box,checked)->{FinderClient.STORE.data.retryWhitelist=checked;save();}).build());
        retry.active=!session.busy;
        whitelist.active=!session.busy&&FinderClient.STORE.data.retryFailedAutoJoin;
        return stacked?retry.getHeight()+6+whitelist.getHeight():Math.max(retry.getHeight(),whitelist.getHeight());
    }
    private List<ServerEntry> visible() {
        if (blockedTab) return new ArrayList<>(FinderClient.STORE.data.blocked.values());
        if(favoritesTab)return FinderClient.STORE.favorites();
        if (history) return FinderClient.STORE.unblocked(FinderClient.STORE.data.history);
        if(failedTab)return FinderClient.STORE.failedServers();
        return FinderClient.STORE.unblocked(live);
    }
    private boolean readMinimum() {
        if (minimum == null) {
            if(filters.maxPlayers>=0&&filters.maxPlayers<filters.minPlayers){status=t("El máximo debe ser mayor o igual al mínimo.","Maximum must be greater than or equal to minimum.");return false;}
            return true;
        }
        if (!minimum.getValue().matches("\\d{1,7}")) { status = t("Ingresá un mínimo válido (0 a 1000000).", "Enter a valid minimum (0 to 1000000)."); return false; }
        int min=Math.min(1_000_000,Integer.parseInt(minimum.getValue()));
        String maxText=maximum==null?"":maximum.getValue().trim();
        if(!maxText.isEmpty()&&!maxText.matches("\\d{1,7}")){status=t("Máximo inválido.","Invalid maximum.");return false;}
        int max=maxText.isEmpty()?-1:Math.min(1_000_000,Integer.parseInt(maxText));
        if(max>=0&&max<min){status=t("El máximo debe ser mayor o igual al mínimo.","Maximum must be greater than or equal to minimum.");return false;}
        filters.minPlayers=min;filters.maxPlayers=max;save();return true;
    }
    private void save() { FinderClient.STORE.save(); }
    private void search(boolean autoJoin, boolean more) {
        if (session.busy || !readMinimum()) return;
        String query = filters.query(0) + filters.compatibleOnly;
        boolean continuing=more && query.equals(lastQuery);
        skip = continuing ? skip : 0; lastQuery = query;
        Set<String> alreadyShown=continuing?live.stream().map(ServerEntry::address).collect(java.util.stream.Collectors.toSet()):new HashSet<>();
        alreadyShown.addAll(failedThisSearch);
        if(!continuing)live.clear();
        favoritesTab=false; history = false; failedTab=false; blockedTab = false; page = 0;
        Filters snapshot = filters.copy();
        session.search(snapshot, protocol(), skip, alreadyShown, batch -> {
            List<ServerEntry> result=batch.matches();skip=batch.nextSkip();accept(result);
            status=result.size()+t(" coincidencias encontradas."," matches found.")+(batch.exhausted()?t(" No quedan más candidatos."," No more candidates available."):"");
            rebuildWidgets();

        }, message -> { status = message; rebuildWidgets(); }, update->{
            progress(new ServerVerifier.Progress(update.checked(),update.checked(),update.match()));
            if(autoJoin&&update.match()!=null) {
                session.cancel();save();connectVerified(update.match(),true);return;
            }
            status=t("Coincidencias: ","Matches: ")+update.matched()+"/40 · "+t("comprobados: ","checked: ")+update.checked();
        }, offset->skip=offset);
        rebuildWidgets();
    }
    private void accept(List<ServerEntry> result) {
        for (int i = result.size() - 1; i >= 0; i--) {
            ServerEntry s = result.get(i); if (FinderClient.STORE.isBlocked(s)) continue; live.removeIf(old -> old.address().equals(s.address())); live.addFirst(s);
        }
        while (live.size() > 200) live.removeLast();
        FinderClient.STORE.addFound(result);
    }
    private void refresh() {
        if (session.busy || blockedTab || failedTab || !readMinimum()) return;
        List<ServerEntry> stored = FinderClient.STORE.unblocked(new ArrayList<>(favoritesTab ? FinderClient.STORE.favorites() : history ? FinderClient.STORE.data.history : FinderClient.STORE.data.found));
        if (stored.isEmpty()) { status = t("Todavía no hay servidores guardados.", "No saved servers yet."); return; }
        status = t("Comprobando servidores guardados…", "Checking saved servers…");
        live.clear();
        session.refresh(stored, filters.copy(), protocol(), result -> {
            accept(result); status = result.size() + t(" guardados respondieron y cumplen los filtros.", " saved servers responded and match your filters."); rebuildWidgets();
        }, message -> { status = message; rebuildWidgets(); }, this::progress);
        rebuildWidgets();
    }
    private void connect(ServerEntry s) {
        if (FinderClient.STORE.isBlocked(s)) return;
        if (session.busy) session.cancel();
        status = t("Comprobando conexión con ", "Checking connection to ") + s.address() + "…";
        session.checkOne(s, protocol(), checked -> {
            if (FinderClient.STORE.isBlocked(checked)) return;
            // Search filters select candidates; an explicit Join remains allowed if player counts change.
            if (filters.compatibleOnly && checked.protocol != protocol()) {
                showFailure(s,t("Este servidor anuncia otra versión de Minecraft.","This server advertises a different Minecraft version."));return;
            }
            connectVerified(checked,false);
        }, message -> showFailure(s,message));
        rebuildWidgets();
    }
    private void connectVerified(ServerEntry server,boolean automatic) {
        if(FinderClient.STORE.isBlocked(server))return;
        session.cancel();save();FinderClient.begin(server,this,automatic);
        ServerData data=new ServerData("Cornbread · "+server.address(),server.address(),ServerData.Type.OTHER);
        try {ConnectScreen.startConnecting(this,minecraft,ServerAddress.parseString(server.address()),data,false,null);}
        catch(RuntimeException e){showFailure(server,e.getMessage()==null?t("No se pudo conectar.","Could not connect."):e.getMessage(),automatic);}
    }
    private void showFailure(ServerEntry server,String reason) {
        showFailure(server,reason,false);
    }
    private void showFailure(ServerEntry server,String reason,boolean automatic) {
        FinderClient.begin(server,this,automatic);
        minecraft.gui.setScreen(new net.minecraft.client.gui.screens.DisconnectedScreen(this,Component.literal(t("No se pudo conectar","Connection failed")),Component.literal(reason)));
    }
    public void findAnother(ServerEntry failed) {
        findAnother(Set.of(failed.address()));
    }
    public void findAnother(Set<String> excluded) {
        failedThisSearch.addAll(excluded);minecraft.gui.setScreen(this);session.retryNow();search(true,true);
    }
    public void blockAndFind(ServerEntry failed) {block(failed);findAnother(failed);}
    public void retryFailedAutomatically(ServerEntry failed) {
        FinderClient.STORE.block(failed);
        if(!FinderClient.STORE.warning.isEmpty()) {
            minecraft.gui.setScreen(this);status=FinderClient.STORE.warning;return;
        }
        findAnother(failed);
    }
    public void blockFailed(ServerEntry failed) {block(failed);minecraft.gui.setScreen(this);}
    @Override public void tick() {
        super.tick(); if(minimum!=null)minimum.setEditable(!session.busy);if(maximum!=null)maximum.setEditable(!session.busy);
        ticks++;
    }
    @Override public void removed() { session.cancel(); save(); icons.close(); }
    void vanillaSaved() { vanillaListChanged=true; }
    @Override public void onClose() {
        session.cancel();save();
        if(vanillaListChanged && parent instanceof SavedListRefresh refresh) { refresh.cornbread$refreshSavedList();vanillaListChanged=false; }
        minecraft.gui.setScreen(parent);
    }
    @Override public void extractBackground(GuiGraphicsExtractor g,int mx,int my,float delta) {
        super.extractBackground(g,mx,my,delta);
        extractMenuBackground(g,resultX,listY,resultW,Math.max(0,height-58-listY));
        g.fill(resultX,listY,resultX+resultW,height-58,0x80000000);
        for(int i=0;i<shown.size();i++) {
            int y=listY+i*rowHeight;
            boolean hover=mx>=resultX&&mx<resultX+resultW&&my>=y&&my<y+rowHeight-4;
            if(hover){g.fill(resultX,y,resultX+resultW,y+rowHeight-4,0xFF808080);g.fill(resultX+1,y+1,resultX+resultW-1,y+rowHeight-5,0xFF000000);}
        }
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta) {
        g.text(font,"CORNBREAD FINDER",left,top,0xFFFFFFFF);
        if(!compact)g.text(font,clip(t("Tu próximo servidor, desde Minecraft.", "Your next server, right from Minecraft."),contentWidth-184),left,top+15,0xFFA0A0A0);
        
        if(wide)g.text(font,t("FILTROS", "FILTERS"),filterX+10,filterY+9,0xFFFFFFFF);
        for(Label label:labels)g.text(font,label.text(),label.x(),label.y(),0xFFA0A0A0);
        for(int i=0;i<shown.size();i++) {
            ServerEntry s=shown.get(i);int y=listY+i*rowHeight;int iconSize=dense?20:32;
            icons.draw(g,s,resultX+4,y+Math.max(2,(rowHeight-4-iconSize)/2),iconSize);
            int textX=resultX+iconSize+10,textW=resultW-iconSize-128;
            int countW=Math.min(64,Math.max(0,textW/3));
            String count=failedTab?t("Falló","Failed"):blockedTab?t("Bloqueado", "Blocked"):s.online+"/"+s.max;
            int titleY=y+(dense?3:7);
            g.text(font,clip((FinderClient.STORE.isFavorite(s)?"★ ":"")+FinderClient.STORE.displayName(s),textW-countW-6),textX,titleY,0xFFFFFFFF);
            g.text(font,clip(count,countW),textX+textW-countW,titleY,history||blockedTab?0xFFA0A0A0:0xFFFFFFFF);
            String visits=t("Entradas: ", "Joins: ")+FinderClient.STORE.countLabel(s);
            FinderStore.FailedVisit failure=FinderClient.STORE.data.failures.get(s.address());
            String meta=failedTab&&failure!=null?t("Intentos: ","Attempts: ")+failure.count+" · "+date(failure.lastAttempt):visits+" · "+(history?t("Visitado ", "Visited ")+date(s.joinedAt):blockedTab?s.version:t("Consulta: ","Checked: ")+date(s.checkedAt));
            if(!dense)g.text(font,clip(failedTab&&failure!=null?failure.reason:s.description,textW),textX,y+(compact?18:22),0xFFA0A0A0);
            g.text(font,clip(meta,textW),textX,y+(dense?15:compact?29:37),0xFF808080);
        }
        if(shown.isEmpty()) {
            int y=listY+Math.max(8,(height-60-listY)/2-12);
            String title=favoritesTab?t("SIN FAVORITOS","NO FAVORITES YET"):failedTab?t("SIN INTENTOS FALLIDOS","NO FAILED ATTEMPTS"):blockedTab?t("SIN BLOQUEADOS", "NO BLOCKED SERVERS"):history?t("TODAVÍA SIN VISITAS", "NO VISITS YET"):t("LISTO PARA EXPLORAR", "READY TO EXPLORE");
            g.centeredText(font,clip(title,resultW-16),resultX+resultW/2,y,0xFFDCEAF6);
            String hint=favoritesTab?t("Usá … → Añadir a favoritos.","Use … → Add to favorites."):failedTab?t("Los errores al entrar se guardan por separado.","Join errors are recorded separately."):blockedTab?t("Usá … → Bloquear para ocultar un servidor.", "Use … → Block to hide a server."):history?t("Las conexiones exitosas se guardan acá.", "Successful connections appear here."):t("Elegí los filtros y tocá Buscar.", "Choose your filters and press Search.");
            g.centeredText(font,clip(hint,resultW-16),resultX+resultW/2,y+15,0xFF808080);
        }
        String message=FinderClient.STORE.warning.isEmpty()?status:FinderClient.STORE.warning;
        String prefix=session.busy?new String[]{"· ","·· ","··· "}[(ticks/7)%3]:"";
        g.text(font,clip(prefix+message,contentWidth),left,height-46,session.busy?0xFFFFFFFF:0xFFA0A0A0);
        int pages=Math.max(1,(visible().size()+rows-1)/rows);
        g.centeredText(font,(page+1)+" / "+pages,left+48,height-23,0xFFA0A0A0);
        if(session.busy) {
            int span=Math.max(20,contentWidth/5);int travel=contentWidth-span;
            int start=left+(int)((System.currentTimeMillis()%1800L)*travel/1800L);
            g.fill(start,height-55,start+span,height-53,0xFFFFFFFF);
        }
        super.extractRenderState(g,mx,my,delta);
    }
    private void progress(ServerVerifier.Progress update) {
        if(update.match()!=null && !FinderClient.STORE.isBlocked(update.match())) {
            ServerEntry match=update.match();live.removeIf(s->s.address().equals(match.address()));live.add(match);
            FinderClient.STORE.stageFound(List.of(match));
            while(live.size()>200)live.removeFirst();
        }
        status=t("Comprobados ", "Checked ")+update.checked()+"/"+update.total()+" · "+live.size()+t(" disponibles", " available");
        rebuildWidgets();
    }
    private void block(ServerEntry s) {
        session.cancel();FinderClient.STORE.block(s);live.removeIf(old->old.address().equals(s.address()));
        status=t("Servidor bloqueado. Podés desbloquearlo en Bloqueados.", "Server blocked. Restore it from Blocked.");rebuildWidgets();
    }
    private String details(ServerEntry s) {
        FinderStore.Visits v=FinderClient.STORE.visits(s);
        String text=FinderClient.STORE.displayName(s)+"\n"+s.address()+"\n"+s.version+"\n"+s.description+"\nCracked: "+flag(s.cracked)+" · Whitelist: "+flag(s.whitelisted)
            +t("\nConexiones exitosas: ", "\nSuccessful joins: ")+FinderClient.STORE.countLabel(s);
        if(v!=null&&v.includesLegacy)text+=t("\n≥: mínimo conocido; las versiones anteriores no contaban visitas.", "\n≥: known minimum; older versions did not count visits.");
        if(failedTab){var f=FinderClient.STORE.data.failures.get(s.address());if(f!=null)text+="\n"+date(f.lastAttempt)+"\n"+f.reason;}
        text+=t("\nÚltima consulta: ","\nLast checked: ")+date(s.checkedAt);
        if(v!=null)text+=t("\nÚltima entrada: ", "\nLast joined: ")+date(v.lastJoined);
        return text;
    }
    private String clip(String value, int w) { return font.plainSubstrByWidth(value == null ? "" : value, w); }
    private static String flag(Boolean v) { return v == null ? t("Desconocido", "Unknown") : v ? t("Sí", "Yes") : t("No", "No"); }
    private static String date(long millis) { return millis <= 0 ? "—" : DateTimeFormatter.ofPattern(Texts.isEnglish() ? "MMM d, HH:mm" : "dd/MM HH:mm", Texts.isEnglish() ? Locale.ENGLISH : Locale.forLanguageTag("es")).format(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault())); }
}
