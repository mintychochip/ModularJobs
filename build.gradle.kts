group = "org.aincraft"
version = "1.0-SNAPSHOT"

subprojects {
    apply(plugin = "java")
    apply(plugin = "java-library")

    configure<JavaPluginExtension> {
        // Paper 26.2 / MockBukkit 26.2 require Java 25
        toolchain.languageVersion.set(JavaLanguageVersion.of(25))
    }
}
