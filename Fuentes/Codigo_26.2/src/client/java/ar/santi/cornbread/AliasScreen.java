package ar.santi.cornbread;

import ar.santi.cornbread.core.ServerEntry;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import static ar.santi.cornbread.core.Texts.t;

public final class AliasScreen extends Screen {
    private final Screen parent;
    private final ServerEntry server;
    private EditBox alias;
    private String draft;
    private int y;
    public AliasScreen(Screen parent,ServerEntry server) {
        super(Component.literal(t("Alias del servidor", "Server alias")));
        this.parent=parent;this.server=server;draft=FinderClient.STORE.alias(server);
    }
    @Override protected void init() {
        int w=Math.min(300,width-24),x=(width-w)/2;y=Math.max(8,(height-130)/2);
        alias=addRenderableWidget(new EditBox(font,x,y+40,w,20,title));
        alias.setMaxLength(48);alias.setValue(draft);alias.setResponder(value->draft=value);
        addRenderableWidget(new FinderButton(t("Guardar", "Save"),x,y+90,(w-4)/2,()->{
            FinderClient.STORE.setAlias(server,alias.getValue());onClose();
        },FinderButton.Style.PRIMARY));
        addRenderableWidget(new FinderButton(t("Cancelar", "Cancel"),x+(w+4)/2,y+90,(w-4)/2,this::onClose,FinderButton.Style.NORMAL));
        setInitialFocus(alias);
    }
    @Override public void onClose(){minecraft.gui.setScreen(parent);}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta) {
        g.centeredText(font,title,width/2,y,0xFFFFFFFF);
        g.centeredText(font,server.address(),width/2,y+20,0xFFA0A0A0);
        g.centeredText(font,t("Vacío = mostrar la IP. Máximo 48 caracteres.", "Empty = show IP. Maximum 48 characters."),width/2,y+68,0xFFA0A0A0);
        super.extractRenderState(g,mx,my,delta);
    }
}
