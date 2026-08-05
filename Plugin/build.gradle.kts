import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import io.papermc.paperweight.userdev.ReobfArtifactConfiguration
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JvmVendorSpec
import xyz.jpenilla.runpaper.task.RunServer

plugins {
    id("craftengineconverter.java-conventions")
    alias(libs.plugins.shadow)
    alias(libs.plugins.paperweight.userdev)
    alias(libs.plugins.run.paper)
}

val pluginVersion: String = project.version.toString()

paperweight {
    injectPaperRepository = false
    reobfArtifactConfiguration = ReobfArtifactConfiguration.MOJANG_PRODUCTION
}

dependencies {
    paperweight.paperDevBundle(libs.versions.paper.get())

    rootProject.subprojects
        .filter { it.path.startsWith(":Hooks:") }
        .sortedBy { it.path }
        .forEach { implementation(project(it.path)) }

    implementation(libs.message.flow) { isTransitive = false }

    implementation(libs.bstats.bukkit)
    implementation(libs.bstats.base)
    implementation(libs.yamllibrary)
    implementation(libs.paper.dispatch)

    compileOnly(libs.craftengine.core)
    compileOnly(libs.craftengine.bukkit)
    compileOnly(libs.reflections)
    compileOnly(libs.snakeyaml)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

val resourceTokens: Map<String, Any> = mapOf(
    "version" to project.version.toString(),
    "project" to mapOf(
        "version" to project.version.toString(),
        "artifactId" to project.name,
        "parent" to mapOf("artifactId" to rootProject.name),
    ),
    "plugin" to mapOf(
        "finalVersion" to pluginVersion,
        "author" to providers.gradleProperty("plugin.author").get(),
        "description" to providers.gradleProperty("plugin.description").get(),
        "apiVersion" to providers.gradleProperty("plugin.apiVersion").get(),
        "mainClass" to providers.gradleProperty("plugin.mainClass").get(),
        "bootstrapper" to providers.gradleProperty("plugin.bootstrapper").get(),
        "loader" to providers.gradleProperty("plugin.loader").get(),
    ),
)

tasks.processResources {
    val tokens = resourceTokens

    inputs.property("resourceTokens", tokens.toString())

    filesMatching(listOf("paper-plugin.yml", "translations/**/messages.yml")) {
        expand(tokens)
    }
}

tasks.named<ShadowJar>("shadowJar") {
    archiveBaseName = "CraftEngineConverter"
    archiveVersion = pluginVersion
    archiveClassifier = ""

    dependencies {
        include(dependency("fr.robie.craftengineconverter:.*"))
        include(dependency("com.tcoded:FoliaLib"))
        include(dependency("org.bstats:bstats-bukkit"))
        include(dependency("org.bstats:bstats-base"))
        include(dependency("fr.maxlego08.sarah:sarah"))
        include(dependency("com.github.1robie.MessageFlow:message-flow"))
        include(dependency("fr.robie:yamllibrary"))
        include(dependency("com.github.1robie.paper-dispatch:paper-dispatch"))
    }

    relocate("com.tcoded.folialib", "fr.robie.craftengineconverter.lib.folialib")
    relocate("org.bstats", "fr.robie.craftengineconverter.lib.bstats")
    relocate("fr.maxlego08.sarah", "fr.robie.craftengineconverter.lib.sarah")
    relocate("fr.robie.messageflow", "fr.robie.craftengineconverter.lib.messageflow")

    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    mergeServiceFiles()

    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")

    exclude("org/slf4j/**")
}

tasks.assemble {
    dependsOn(tasks.named("shadowJar"))
}


val runServerJvmArgs: Provider<List<String>> = providers.gradleProperty("runServer.jvmArgs")
    .map { args -> args.split(' ').filter(String::isNotBlank) }
    .orElse(emptyList())

val runServerJavaAgent: Provider<String> = providers.gradleProperty("runServer.javaAgent")

val runServerJavaAgentArgs: Provider<String> = providers.gradleProperty("runServer.javaAgentArgs")

val runServerJavaVersion: Provider<Int> = providers.gradleProperty("runServer.javaVersion")
    .map(String::toInt)
    .orElse(libs.versions.java.map(String::toInt))

val runServerJavaVendor: Provider<String> = providers.gradleProperty("runServer.javaVendor")

tasks.withType<RunServer>().configureEach {
    // Lets the dev server run on a different JDK than the compilation toolchain - useful for
    // pointing it at a JetBrains Runtime / DCEVM install for HotSwap.
    javaLauncher = javaToolchains.launcherFor {
        languageVersion = runServerJavaVersion.map(JavaLanguageVersion::of)
        if (runServerJavaVendor.isPresent) {
            vendor = JvmVendorSpec.matching(runServerJavaVendor.get())
        }
    }

    jvmArgs(runServerJvmArgs.get())
    runServerJavaAgent.orNull?.let { agent ->
        val agentArgs = runServerJavaAgentArgs.orNull?.takeIf(String::isNotBlank)
        jvmArgs(if (agentArgs == null) "-javaagent:$agent" else "-javaagent:$agent=$agentArgs")
    }

    // downloadPlugins {
    //     hangar("PlaceholderAPI", "2.11.6")
    //     modrinth("packetevents", "2.9.1")
    // }
}
