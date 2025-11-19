package fr.robie.craftEngineConverter.loader;

public enum Template {
    MODEL_CUBE_ALL("templates/model/cube_all"),
    MODEL_CUBE_TOP("templates/model/cube_top"),
    MODEL_ITEM_GENERATED("templates/model/item_generated"),
    MODEL_ITEM_SHIELD("templates/model/item_shield"),

    ;
    private final String path;

    Template(String path){
        this.path = path;
    }

    public String getPath() {return this.path;}
}
