plugins {
    id("craftengineconverter.hook-conventions")
}

dependencies {
    compileOnly(libs.nexo) {
        exclude(group = "dev.triumphteam", module = "triumph-gui")
    }
}
