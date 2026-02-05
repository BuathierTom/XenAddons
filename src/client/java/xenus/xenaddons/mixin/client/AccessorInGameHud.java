package xenus.xenaddons.mixin.client;

import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(InGameHud.class)
public interface AccessorInGameHud {
    @Accessor("titleFadeInTicks")
    void setTitleFadeInTicks(int ticks);

    @Accessor("titleStayTicks")
    void setTitleStayTicks(int ticks);

    @Accessor("titleFadeOutTicks")
    void setTitleFadeOutTicks(int ticks);
}
