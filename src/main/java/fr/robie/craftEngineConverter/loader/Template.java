package fr.robie.craftEngineConverter.loader;

public enum Template {
    MODEL_CUBE_ALL("templates/model/cube_all"),
    MODEL_CUBE_TOP("templates/model/cube_top"),
    MODEL_ITEM_GENERATED("templates/model/item_generated"),
    MODEL_ITEM_SHIELD("templates/model/item_shield"),
    MODEL_ITEM_FISHING_ROD("templates/model/item_fishing_rod"),
    MODEL_ITEM_CROSSBOW("templates/model/item_crossbow"),

    ;
    private final String path;

    Template(String path){
        this.path = path;
    }

    public String getPath() {return this.path;}
}
