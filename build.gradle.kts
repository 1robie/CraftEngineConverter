//
// Root build script.
//
// Deliberately almost empty: there is no `allprojects {}` / `subprojects {}` block here.
// Cross-project configuration is what makes Gradle builds slow and un-cacheable, so shared
// setup lives in the convention plugins under `build-logic/` and each module applies the one
// it needs. `group` and `version` come from gradle.properties and are inherited by every project
// automatically, which is what the Maven <parent> section used to do.
//

plugins {
    base
}

// Maven's <defaultGoal>clean package</defaultGoal>. `clean` is not part of it because Gradle is
// incremental - run `./gradlew clean build` explicitly if you really want a from-scratch build.
defaultTasks("build")
