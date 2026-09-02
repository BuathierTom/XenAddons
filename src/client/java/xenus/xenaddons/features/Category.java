package xenus.xenaddons.features;

public enum Category {
    DUNGEON("Dungeon"),
    SLAYER("Slayer"),
    MISC("Misc"),
    DEV("Dev");

    public final String name;

    Category(String name) {
        this.name = name;
    }
}
