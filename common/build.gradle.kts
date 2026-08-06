dependencies {
    api(libs.jetbrains.annotations)
    // Gson annotations only — serialization remains caller's concern (paper already has gson)
    compileOnly(libs.gson)
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation(libs.gson)
}

tasks.test {
    useJUnitPlatform()
}
