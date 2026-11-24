package ar.santi.cornbread;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
/** Native widgets respect Minecraft textures, sounds and keyboard navigation. */
final class FinderButton extends Button.Plain {
    enum Style { NORMAL, PRIMARY, TAB, SELECTED, QUIET }
    FinderButton(String text,int x,int y,int w,Runnable action,Style style) {
        super(x,y,w,20,style==Style.SELECTED?Component.literal(text).withStyle(ChatFormatting.YELLOW):Component.literal(text),b->action.run(),DEFAULT_NARRATION);
    }
    static void panel(GuiGraphicsExtractor g,int x,int y,int w,int h,int color){g.fill(x,y,x+w,y+h,color);}
}
