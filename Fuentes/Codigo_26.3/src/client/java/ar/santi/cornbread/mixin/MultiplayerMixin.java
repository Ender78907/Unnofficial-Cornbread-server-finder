package ar.santi.cornbread.mixin;

import static ar.santi.cornbread.core.Texts.t;

import ar.santi.cornbread.FinderScreen;
import ar.santi.cornbread.SavedListRefresh;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import org.spongepowered.asm.mixin.Shadow;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(JoinMultiplayerScreen.class)
public abstract class MultiplayerMixin extends Screen implements SavedListRefresh {
    @Shadow protected ServerSelectionList serverSelectionList;
    @Override public void cornbread$refreshSavedList() {
        var list=((JoinMultiplayerScreen)(Object)this).getServers();
        list.load();serverSelectionList.updateOnlineServers(list);
    }
    @Unique private Button cornbread$button;
    protected MultiplayerMixin(Component title) { super(title); }
    @Inject(method = "init", at = @At("TAIL"))
    private void cornbread$addFinder(CallbackInfo ci) {
        cornbread$button = addRenderableWidget(Button.builder(Component.literal(t("Buscar servers", "Find servers")), b ->
            minecraft.gui.setScreen(new FinderScreen(this))).bounds(6, 6, 104, 20).build());
    }
    @Inject(method = "tick", at = @At("TAIL"))
    private void cornbread$language(CallbackInfo ci) {
        if (cornbread$button != null) cornbread$button.setMessage(Component.literal(t("Buscar servers", "Find servers")));
    }
}
