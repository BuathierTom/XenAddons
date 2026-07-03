package xenus.xenaddons.mixin.client;

import net.minecraft.client.gui.Gui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Gui.class)
public interface AccessorInGameHud {
    @Accessor("titleFadeInTime")
    void setTitleFadeInTicks(int ticks);

    @Accessor("titleStayTime")
    void setTitleStayTicks(int ticks);

    @Accessor("titleFadeOutTime")
    void setTitleFadeOutTicks(int ticks);
}
