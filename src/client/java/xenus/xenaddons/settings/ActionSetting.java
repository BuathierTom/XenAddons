package xenus.xenaddons.settings;

public class ActionSetting extends Setting<Boolean> {
    private final Runnable action;

    public ActionSetting(String name, Runnable action) {
        super(name, false);
        this.action = action;
    }

    public void run() {
        if (action != null) action.run();
    }
    
    @Override
    public Boolean getValue() {
        return false; // Action settings don't really have a persistent value
    }
}
