package ar.santi.cornbread.mixin;

import ar.santi.cornbread.FinderClient;
import ar.santi.cornbread.AliasScreen;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import static ar.santi.cornbread.core.Texts.t;

@Mixin(PauseScreen.class)
public abstract class PauseMixin extends Screen {
    @Shadow private Button disconnectButton;
    protected PauseMixin(Component title){super(title);}
    @Inject(method="createPauseMenu",at=@At("TAIL"))
    private void cornbread$leaveAndFind(CallbackInfo ci) {
        if(minecraft.isLocalServer()||minecraft.getConnection()==null||disconnectButton==null)return;
        int originalWidth=Math.min(width-16,Math.max(308,disconnectButton.getWidth()));
        int left=(width-originalWidth)/2;
        int overflow=Math.max(0,disconnectButton.getY()+44-(height-4));
        if(overflow>0)for(var child:children())if(child instanceof AbstractWidget widget)widget.setY(widget.getY()-overflow);
        int half=(originalWidth-28)/2;
        disconnectButton.setX(left);disconnectButton.setWidth(half);
        int secondY=disconnectButton.getY()+24,third=(originalWidth-8)/3;
        addRenderableWidget(Button.builder(Component.literal(t("Salir y buscar","Leave & find")),b->FinderClient.leaveAndFind(this))
            .bounds(disconnectButton.getX()+half+4,disconnectButton.getY(),originalWidth-half-28,20)
            .tooltip(Tooltip.create(Component.literal(t("Desconectarte y buscar otro servidor con tus filtros. No bloquea el actual.","Disconnect and find another server using your filters. Does not block the current server."))))
            .build());
        var target=FinderClient.currentBlockTarget();
        boolean blocked=target!=null&&FinderClient.STORE.isBlocked(target);
        Button block=addRenderableWidget(Button.builder(Component.literal(blocked?t("Bloqueado","Blocked"):t("Bloquear","Block")),b->{
            var active=FinderClient.currentBlockTarget();
            if(active==null)return;
            FinderClient.STORE.block(active);
            if(FinderClient.STORE.warning.isEmpty()) {
                b.setMessage(Component.literal(t("Bloqueado","Blocked")));b.active=false;
                b.setTooltip(Tooltip.create(Component.literal(t("Servidor bloqueado. Podés recuperarlo en Bloqueados.","Server blocked. Restore it from Blocked."))));
            } else b.setTooltip(Tooltip.create(Component.literal(FinderClient.STORE.warning)));
        }).bounds(left,secondY,third,20)
            .tooltip(Tooltip.create(Component.literal(target==null?t("No hay una dirección IPv4 para bloquear.","No IPv4 address available to block."):
                blocked?t("Este servidor ya está bloqueado.","This server is already blocked."):
                t("Bloquear servidor sin desconectarte: ","Block server without disconnecting: ")+target.address())))
            .build());
        block.active=target!=null&&!blocked;
        Button blockAndFind=addRenderableWidget(Button.builder(Component.literal("×›"),b->FinderClient.blockAndFind(this))
            .bounds(disconnectButton.getX()+originalWidth-20,disconnectButton.getY(),20,20)
            .tooltip(Tooltip.create(Component.literal(t("Bloquear y buscar otro: desconectarte y entrar a la primera coincidencia con tus filtros.","Block & find another: disconnect and join the first match using your filters."))))
            .build());
        blockAndFind.active=target!=null;
        Button favorite=addRenderableWidget(Button.builder(Component.literal(target!=null&&FinderClient.STORE.isFavorite(target)?t("★ Favorito","★ Favorite"):t("☆ Favorito","☆ Favorite")),b->{
            var active=FinderClient.currentBlockTarget();if(active==null)return;
            FinderClient.STORE.toggleFavorite(active);
            b.setMessage(Component.literal(FinderClient.STORE.isFavorite(active)?t("★ Favorito","★ Favorite"):t("☆ Favorito","☆ Favorite")));
            b.setTooltip(Tooltip.create(Component.literal(FinderClient.STORE.warning.isEmpty()?
                (FinderClient.STORE.isFavorite(active)?t("Quitar de favoritos","Remove from favorites"):t("Añadir a favoritos","Add to favorites")):FinderClient.STORE.warning)));
        }).bounds(left+third+4,secondY,third,20)
            .tooltip(Tooltip.create(Component.literal(t("Añadir o quitar de favoritos sin salir del servidor.","Add or remove favorite without leaving the server.")))).build());
        favorite.active=target!=null;
        Button alias=addRenderableWidget(Button.builder(Component.literal("Alias"),b->{
            var active=FinderClient.currentBlockTarget();if(active!=null)minecraft.gui.setScreen(new AliasScreen(this,active));
        }).bounds(left+2*(third+4),secondY,originalWidth-2*(third+4),20)
            .tooltip(Tooltip.create(Component.literal(t("Editar el alias sin salir del servidor.","Edit the alias without leaving the server.")))).build());
        alias.active=target!=null;
    }
}
