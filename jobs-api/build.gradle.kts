dependencies {
    compileOnly(libs.paper.api)
    compileOnly(libs.vault.api) {
        exclude(group = "org.bukkit")
    }
    compileOnly(libs.mcmmo) {
        exclude(group = "com.sk89q.worldguard")
    }
}
