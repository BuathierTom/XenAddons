package xenus.xenaddons.features.impl.misc;

import xenus.xenaddons.config.CustomPlayerScaleManager;
import xenus.xenaddons.config.GoatedPlayersManager;
import xenus.xenaddons.cosmetics.CosmeticsRepository;
import xenus.xenaddons.features.Category;
import xenus.xenaddons.features.Module;
import xenus.xenaddons.settings.ActionSetting;
import xenus.xenaddons.settings.BooleanSetting;
import xenus.xenaddons.utils.ChatUtils;

import java.util.concurrent.atomic.AtomicBoolean;

public class CustomSizeModule extends Module {
    public final BooleanSetting customNames = new BooleanSetting("Custom names", true);
    public final BooleanSetting customSize = new BooleanSetting("Custom Size", true);
    public final ActionSetting refreshCosmetics = new ActionSetting("Refresh Cosmetics", this::refreshCosmetics);
    private final AtomicBoolean refreshInProgress = new AtomicBoolean(false);

    public CustomSizeModule() {
        super("Cosmetics", "Apply custom player cosmetics", Category.DEV);
        this.registerSetting(customNames);
        this.registerSetting(customSize);
        this.registerSetting(refreshCosmetics);
    }

    public boolean shouldApplyCustomNames() {
        return customNames.getValue();
    }

    public boolean shouldApplyCustomSize() {
        return this.isEnabled() && customSize.getValue();
    }

    private void refreshCosmetics() {
        if (!refreshInProgress.compareAndSet(false, true)) {
            ChatUtils.warning("Cosmetics refresh already running.");
            return;
        }

        ChatUtils.info("Refreshing cosmetics...");
        Thread.startVirtualThread(() -> {
            try {
                CosmeticsRepository.reload();
                GoatedPlayersManager.init();
                CustomPlayerScaleManager.init();
                mc.execute(() -> {
                    if (CosmeticsRepository.isUsingFallbackResource()) {
                        ChatUtils.warning("Cosmetics refreshed from fallback resource.");
                    } else {
                        ChatUtils.success("Cosmetics refreshed from API.");
                    }
                });
            } catch (Exception e) {
                mc.execute(() -> ChatUtils.error("Failed to refresh cosmetics."));
            } finally {
                refreshInProgress.set(false);
            }
        });
    }
}
