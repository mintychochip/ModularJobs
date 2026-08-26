dependencies {
    api(libs.jetbrains.annotations)
    api(libs.gson)
    testImplementation(libs.databag.api)
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}
