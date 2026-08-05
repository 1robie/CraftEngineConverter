import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    `java-library`
}

repositories {
    mavenLocal {
        content { includeModule("fr.robie", "yamllibrary") }
    }

    mavenCentral()

    // Paper
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "papermc"
    }

    // CraftEngine
    maven("https://repo.momirealms.net/releases") {
        name = "momirealmsReleases"
        mavenContent { releasesOnly() }
        content { includeGroup("net.momirealms") }
    }
    maven("https://repo.momirealms.net/snapshots") {
        name = "momirealmsSnapshots"
        mavenContent { snapshotsOnly() }
        content { includeGroup("net.momirealms") }
    }

    // PacketEvents
    maven("https://repo.codemc.io/repository/maven-releases/") {
        name = "codemcReleases"
        mavenContent { releasesOnly() }
        content { includeGroup("com.github.retrooper") }
    }

    // PlaceholderAPI
    maven("https://repo.extendedclip.com/releases/") {
        name = "extendedclip"
        mavenContent { releasesOnly() }
        content { includeGroup("me.clip") }
    }

    // Nexo
    maven("https://repo.nexomc.com/releases") {
        name = "nexo"
        mavenContent { releasesOnly() }
        content {
            includeGroup("com.nexomc")
            includeGroup("team.unnamed")
            includeGroupByRegex("me\\.gabytm\\.util.*")
        }
    }

    // ItemsAdder
    maven("https://maven.devs.beer/") {
        name = "matteodev"
        content { includeGroup("dev.lone") }
    }

    // FoliaLib
    maven("https://repo.tcoded.com/releases") {
        name = "tcodedReleases"
        mavenContent { releasesOnly() }
        content { includeGroup("com.tcoded") }
    }

    // Sarah (database library)
    maven("https://repo.groupez.dev/releases") {
        name = "groupez"
        mavenContent { releasesOnly() }
        content { includeGroupByRegex("fr\\.maxlego08.*") }
    }

    // MessageFlow, paper-dispatch
    maven("https://jitpack.io") {
        name = "jitpack"
        content { includeGroupByRegex("com\\.github\\.1robie.*") }
        metadataSources { artifact() }
    }

    maven("https://central.sonatype.com/repository/maven-snapshots/") {
        name = "centralSnapshots"
        mavenContent { snapshotsOnly() }
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(libs.version("java").toInt())
    }
}

configurations {
    testImplementation {
        extendsFrom(compileOnly.get())
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release = libs.version("java").toInt()
}

tasks.withType<Javadoc>().configureEach {
    (options as StandardJavadocDocletOptions).encoding = "UTF-8"
}

tasks.withType<ProcessResources>().configureEach {
    filteringCharset = "UTF-8"
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    testLogging {
        events("failed")
        exceptionFormat = TestExceptionFormat.FULL
        showStackTraces = true
    }
}

tasks.withType<AbstractArchiveTask>().configureEach {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
    dirPermissions { unix("rwxr-xr-x") }
    filePermissions { unix("rw-r--r--") }
}
