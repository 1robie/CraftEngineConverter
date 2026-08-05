//
// Settings for the CraftEngineConverter multi-module build.
//
// Replaces the <modules> and <pluginManagement> sections of the old root pom.xml.
//
// Repositories are NOT declared here. paperweight-userdev registers extra repositories
// (mache, the plugin remapper, and the decompiler/param-mapping repos named by the dev bundle's
// own metadata) directly on the :Plugin project. Gradle cannot merge settings repositories with
// project repositories - whichever `RepositoriesMode` is chosen, one side is discarded - so a
// settings-level repository block would either be ignored for :Plugin or would silently break
// paperweight's setup pipeline. They therefore live in the `craftengineconverter.java-conventions`
// convention plugin, which is still a single declaration site for the whole build.
//

pluginManagement {
    repositories {
        gradlePluginPortal()
        // paperweight (and Paper plugin markers) are published here too.
        maven("https://repo.papermc.io/repository/maven-public/") {
            name = "papermc"
        }
    }

    // Convention plugins live in their own included build instead of buildSrc: editing one then
    // only re-runs what actually depends on it, and IntelliJ imports it as a separate,
    // properly-typed Gradle project.
    includeBuild("build-logic")
}

plugins {
    // Lets Gradle download a matching JDK on its own when the required toolchain (Java 21) is not
    // installed locally. Without this, a machine with only a newer JDK fails with
    // "No matching toolchain found".
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "CraftEngineConverter"

// Module layout is identical to the Maven one; Gradle derives the directories from the paths.
include(
    ":API",
    ":Common",
    ":Hooks",
    ":Hooks:PacketEvent",
    ":Hooks:PlaceholderAPI",
    ":Hooks:Nexo",
    ":Hooks:ItemsAdder",
    ":Plugin",
)
