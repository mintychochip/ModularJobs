dependencies {
    compileOnly(libs.paper.api)
    compileOnly(libs.mcmmo) {
        exclude(group = "com.sk89q.worldguard")
    }
}
