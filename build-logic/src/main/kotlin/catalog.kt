import org.gradle.api.Project
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.getByType

/**
 * Type-safe `libs.*` accessors are not generated inside an included build, so convention plugins
 * reach the consuming build's `gradle/libs.versions.toml` through the public
 * [VersionCatalogsExtension] API instead. No reflection tricks, no `LibrariesForLibs` hack.
 */
internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal fun VersionCatalog.library(alias: String): Provider<MinimalExternalModuleDependency> =
    findLibrary(alias).orElseThrow {
        IllegalStateException("No library '$alias' declared in gradle/libs.versions.toml")
    }

internal fun VersionCatalog.version(alias: String): String =
    findVersion(alias).orElseThrow {
        IllegalStateException("No version '$alias' declared in gradle/libs.versions.toml")
    }.requiredVersion
