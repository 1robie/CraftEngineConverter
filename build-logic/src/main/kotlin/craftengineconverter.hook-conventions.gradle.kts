plugins {
    `java-library`
    id("craftengineconverter.java-conventions")
}

dependencies {
    api(project(":Common"))

    compileOnly(libs.library("paper-api"))
    compileOnly(libs.library("message-flow"))
}
