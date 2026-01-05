group = "org.aincraft"
version = "1.0-SNAPSHOT"

subprojects {
    apply(plugin = "java")
    apply(plugin = "java-library")

    configure<JavaPluginExtension> {
        toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    }
}
