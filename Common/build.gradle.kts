plugins {
    id("craftengineconverter.java-conventions")
}

dependencies {
    api(project(":API"))
    api(libs.sarah)

    compileOnly(libs.paper.api)
    compileOnly(libs.craftengine.core)
    compileOnly(libs.craftengine.bukkit)
    compileOnly(libs.message.flow)
    compileOnly(libs.yamllibrary)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}
