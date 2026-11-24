package ar.santi.cornbread;

import ar.santi.cornbread.core.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

public final class FinderClient implements ClientModInitializer {
    public static FinderStore STORE;
    private static final JoinAttempt ATTEMPT = new JoinAttempt();
    public static Screen pendingParent() { return (Screen)ATTEMPT.parent(); }
    public static void begin(ServerEntry server,FinderScreen parent,boolean automatic) {ATTEMPT.begin(server,parent,automatic);}
    public static void cancelAttempt() {ATTEMPT.cancel();}
    public static JoinAttempt.Failure failed(Screen parent,Component reason) {
        return ATTEMPT.failed(STORE,parent,reason.getString(),whitelistReason(reason));
    }
    private static boolean whitelistReason(Component reason) {
        if(reason.getContents() instanceof TranslatableContents translated) {
            if(JoinAttempt.isWhitelist(translated.getKey()))return true;
            for(Object arg:translated.getArgs())
                if(arg instanceof Component component && whitelistReason(component))return true;
        }
        for(Component sibling:reason.getSiblings())if(whitelistReason(sibling))return true;
        return JoinAttempt.isWhitelist(reason.getString());
    }
    /** Uses the advertised numeric address, or the connected IPv4 for DNS/SRV entries. */
    public static ServerEntry currentBlockTarget() {
        Minecraft client=Minecraft.getInstance();
        if(client.isLocalServer()||client.getConnection()==null)return null;
        var current=client.getCurrentServer();
        if(current!=null) {
            var address=net.minecraft.client.multiplayer.resolver.ServerAddress.parseString(current.ip);
            ServerEntry entry=new ServerEntry();entry.host=address.getHost();entry.port=address.getPort();
            if(entry.valid()) {var known=STORE.known(entry.address());return known==null?entry:known.copy();}
        }
        var remote=client.getConnection().getConnection().getRemoteAddress();
        if(remote instanceof java.net.InetSocketAddress socket && socket.getAddress() instanceof java.net.Inet4Address) {
            ServerEntry entry=new ServerEntry();entry.host=socket.getAddress().getHostAddress();entry.port=socket.getPort();
            var known=STORE.known(entry.address());return known==null?entry:known.copy();
        }
        return null;
    }
    public static void leaveAndFind(net.minecraft.client.gui.screens.Screen pause) { leaveAndFind(pause,false); }
    public static void blockAndFind(net.minecraft.client.gui.screens.Screen pause) { leaveAndFind(pause,true); }
    private static void leaveAndFind(net.minecraft.client.gui.screens.Screen pause,boolean blockCurrent) {
        Minecraft client=Minecraft.getInstance();
        if(client.isLocalServer()||client.getConnection()==null)return;
        client.getReportingContext().draftReportHandled(client,pause,()->{
            if(blockCurrent) {
                ServerEntry target=currentBlockTarget();
                if(target==null)return;
                STORE.block(target);
                if(!STORE.warning.isEmpty()) {
                    client.gui.setScreen(new net.minecraft.client.gui.screens.AlertScreen(()->client.gui.setScreen(pause),
                        net.minecraft.network.chat.Component.literal(Texts.t("No se pudo guardar el bloqueo","Could not save block")),
                        net.minecraft.network.chat.Component.literal(STORE.warning)));
                    return;
                }
            }
            java.util.Set<String> excluded=new java.util.HashSet<>();
            var current=client.getCurrentServer();
            if(current!=null){var address=net.minecraft.client.multiplayer.resolver.ServerAddress.parseString(current.ip);excluded.add(address.getHost()+":"+address.getPort());}
            var listener=client.getConnection();
            if(listener!=null && listener.getConnection().getRemoteAddress() instanceof java.net.InetSocketAddress remote && remote.getAddress()!=null)
                excluded.add(remote.getAddress().getHostAddress()+":"+remote.getPort());
            cancelAttempt();
            client.disconnectFromWorld(net.minecraft.client.multiplayer.ClientLevel.DEFAULT_QUIT_MESSAGE);
            var parent=client.gui.screen();
            if(parent==null)parent=new net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen(new net.minecraft.client.gui.screens.TitleScreen());
            new FinderScreen(parent).findAnother(excluded);
        },true);
    }
    @Override public void onInitializeClient() {
        STORE = new FinderStore(FabricLoader.getInstance().getConfigDir().resolve("cornbread-finder.json"));
        Texts.setLanguage(STORE.data.language);
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            // JOIN can fire before Minecraft.player is assigned. Read the listener
            // that actually joined, not Minecraft.getCurrentServer() (player-dependent).
            var current = handler.getServerData();
            if (current != null) {
                var address = net.minecraft.client.multiplayer.resolver.ServerAddress.parseString(current.ip);
                String key = address.getHost() + ":" + address.getPort();
                ServerEntry pending = ATTEMPT.pending();
                ServerEntry joined = pending != null && pending.address().equals(key) ? pending : STORE.known(key);
                if (joined != null) STORE.joined(joined);
            }
            cancelAttempt();
        });

    }
}
