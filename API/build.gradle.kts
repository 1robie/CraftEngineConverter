plugins {
    id("craftengineconverter.java-conventions")
}

base {
    archivesName = "CraftEngineConverter-API"
}

dependencies {
    api(libs.folialib)

    compileOnly(libs.paper.api)
    compileOnly(libs.craftengine.core)
    compileOnly(libs.craftengine.bukkit)
    compileOnly(libs.sarah)
    compileOnly(libs.reflections)
    compileOnly(libs.message.flow)
    compileOnly(libs.yamllibrary)
}
