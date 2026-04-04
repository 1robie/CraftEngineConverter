package fr.robie.craftengineconverter.api.enums.item.component;

public enum ConsumableAnimation {
    NONE, EAT, DRINK, BLOCK, BOW, SPEAR, CROSSBOW, SPYGLASS, TOOT_HORN, BRUSH, BUNDLE, TRIDENT;

    public String toKey() {
        return this.name().toLowerCase();
    }
}
