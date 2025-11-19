package fr.robie.craftEngineConverter.core.utils;

public enum Template {
    MODEL_CUBE_ALL("templates/model/cube_all", TemplateType.BLOCK),
    MODEL_CUBE_TOP("templates/model/cube_top", TemplateType.BLOCK),
    MODEL_ITEM_GENERATED("templates/model/item_generated"),
    MODEL_ITEM_SHIELD("templates/model/item_shield"),
    MODEL_ITEM_FISHING_ROD("templates/model/item_fishing_rod"),
    MODEL_ITEM_CROSSBOW("templates/model/item_crossbow"),
    MODEL_ITEM_BOW("templates/model/item_bow"),
    MODEL_ITEM_DEFAULT("templates/model/item_default"),

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
