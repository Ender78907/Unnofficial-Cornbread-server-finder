package ar.santi.cornbread.mixin;
import ar.santi.cornbread.FinderClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.multiplayer.*;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ConnectScreen.class)
public abstract class ConnectMixin {
    @Shadow @Final private Screen parent;
    @Inject(method="lambda$init$0",at=@At("HEAD"))
    private void cornbread$cancelAttempt(Button button,CallbackInfo ci) {
        if(parent==FinderClient.pendingParent())FinderClient.cancelAttempt();
    }
    @Inject(method="startConnecting",at=@At("HEAD"))
    private static void cornbread$clearUnrelatedAttempt(Screen parent,Minecraft client,ServerAddress address,ServerData data,boolean quickPlay,TransferState transfer,CallbackInfo ci) {
        if(parent!=FinderClient.pendingParent())FinderClient.cancelAttempt();
    }
}
