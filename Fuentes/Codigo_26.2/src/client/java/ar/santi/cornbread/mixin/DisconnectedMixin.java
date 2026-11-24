package ar.santi.cornbread.mixin;

import ar.santi.cornbread.*;
import ar.santi.cornbread.core.*;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import static ar.santi.cornbread.core.Texts.t;

@Mixin(DisconnectedScreen.class)
public abstract class DisconnectedMixin extends Screen {
    @Shadow @Final private Screen parent;
    @Shadow @Final private DisconnectionDetails details;
    @Shadow @Final private LinearLayout layout;
    @Unique private JoinAttempt.Failure cornbread$failure;
    @Unique private int cornbread$retryTicks=-1;
    @Unique private Button cornbread$cancelRetry;
    protected DisconnectedMixin(Component title){super(title);}
    @Inject(method="init",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/layouts/LinearLayout;arrangeElements()V"))
    private void cornbread$failureOptions(CallbackInfo ci) {
        if(cornbread$failure==null) {
            cornbread$failure=FinderClient.failed(parent,details.reason());
            if(cornbread$failure!=null && cornbread$failure.retryAutomatically())cornbread$retryTicks=60;
        }
        if(cornbread$failure==null || !(parent instanceof FinderScreen finder))return;
        ServerEntry failed=cornbread$failure.server();
        if(cornbread$retryTicks>0)cornbread$cancelRetry=layout.addChild(Button.builder(cornbread$retryLabel(),b->{
            cornbread$retryTicks=-1;b.active=false;b.setMessage(Component.literal(t("Reintento cancelado","Retry cancelled")));
        }).width(200).tooltip(Tooltip.create(Component.literal(t("Se bloqueará este servidor y se buscará otro automáticamente. Cancelar evita ambas acciones.","This server will be blocked and another searched automatically. Cancel prevents both actions.")))).build());
        layout.addChild(Button.builder(Component.literal(t("Bloquear servidor","Block server")),b->finder.blockFailed(failed)).width(200).build());
        layout.addChild(Button.builder(Component.literal(t("Bloquear y buscar otro","Block & find another")),b->finder.blockAndFind(failed)).width(200).build());
        layout.addChild(Button.builder(Component.literal(t("Buscar otro servidor","Find another server")),b->finder.findAnother(failed)).width(200).build());
    }
    @Unique private Component cornbread$retryLabel() {
        return Component.literal(t("Cancelar reintento (","Cancel retry (")+(cornbread$retryTicks+19)/20+")");
    }
    @Override public void tick() {
        super.tick();
        if(cornbread$retryTicks<=0)return;
        cornbread$retryTicks--;
        if(cornbread$cancelRetry!=null)cornbread$cancelRetry.setMessage(cornbread$retryLabel());
        if(cornbread$retryTicks==0) {
            cornbread$retryTicks=-1;
            if(minecraft.gui.screen()==this && parent instanceof FinderScreen finder)
                finder.retryFailedAutomatically(cornbread$failure.server());
        }
    }
    @Override public void removed() {cornbread$retryTicks=-1;super.removed();}
}
