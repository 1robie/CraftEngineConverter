package fr.robie.craftengineconverter.utils.enums;

public enum Template {
    MODEL_CUBE_ALL("templates/model/cube_all", TemplateType.BLOCK),
    MODEL_CUBE_TOP("templates/model/cube_top", TemplateType.BLOCK),
    MODEL_ITEM_GENERATED("templates/model/item_generated"),
    MODEL_ITEM_SHIELD("templates/model/item_shield"),
    MODEL_ITEM_FISHING_ROD("templates/model/item_fishing_rod"),
    MODEL_ITEM_CROSSBOW("templates/model/item_crossbow"),
    MODEL_ITEM_BOW("templates/model/item_bow"),
    MODEL_ITEM_DEFAULT("templates/model/item_default"),
    MODEL_ITEM_ELYTRA("templates/model/item_elytra"),
    MODEL_TRIDENT("templates/model/item_trident"),

    SETTINGS_PROJECTILE("templates/settings/projectile", TemplateType.SETTINGS),

    LOOT_TABLE_BASIC_DROP("templates/loot_table/basic_drop", TemplateType.LOOT_TABLE),
    LOOT_TABLE_SILK_TOUCH_ONLY("templates/loot_table/silk_touch_only_drop", TemplateType.LOOT_TABLE),

    MINECRAFT_EFFECT("templates/minecraft/effect",TemplateType.OTHER)

    ;
    private final String path;
    private final TemplateType type;

    Template(String path){
        this.path = path;
        this.type = TemplateType.ITEM;
    }

    Template(String path, TemplateType type) {
        this.path = path;
        this.type = type;
    }

    public String getPath() {return this.path;}
    public TemplateType getType() {return this.type;}
}
