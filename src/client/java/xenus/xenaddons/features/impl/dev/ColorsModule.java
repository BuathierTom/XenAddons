package xenus.xenaddons.features.impl.dev;

import xenus.xenaddons.features.Category;
import xenus.xenaddons.features.Module;
import xenus.xenaddons.settings.ColorSetting;
import xenus.xenaddons.gui.config.ui.ClickGUI;
import xenus.xenaddons.gui.ui.Theme;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ColorsModule extends Module {
    public final ColorSetting menuColor = new ColorSetting("Menu Color", 0xFFA020F0);
    private static final ScheduledExecutorService COLOR_APPLY_EXECUTOR = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "xenaddons-color-apply");
        t.setDaemon(true);
        return t;
    });
    private final AtomicInteger colorChangeToken = new AtomicInteger(0);

    public ColorsModule() {
        super("Colors", "Change the GUI theme color", Category.DEV);
        menuColor.expanded = true;
        menuColor.setListener(this::applyMenuColorWithDelay);
        this.registerSetting(menuColor);
        Theme.setMenuPrimary(menuColor.getValue());
    }

    private void applyMenuColorWithDelay(Integer newColor) {
        final int token = colorChangeToken.incrementAndGet();
        COLOR_APPLY_EXECUTOR.schedule(() -> {
            if (token != colorChangeToken.get()) return;
            mc.execute(() -> Theme.setMenuPrimary(newColor));
            mc.execute(() -> {
                if (mc.currentScreen == ClickGUI.INSTANCE) {
                    xenus.xenaddons.gui.managers.config.ConfigManager.openConfig();
                }
            });
        }, 2, TimeUnit.SECONDS);
    }

    public int getColor() {
        return menuColor.getValue();
    }
}
