dependencies {
    compileOnly(libs.paper.api)
    compileOnly(libs.mcmmo) {
        exclude(group = "com.sk89q.worldguard")
    }

    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    // MockBukkit for Paper 26.2 (brings aligned paper-api test runtime)
    testImplementation(libs.mockbukkit)
    testImplementation(libs.paper.api)
}

tasks.test {
    useJUnitPlatform()
}
