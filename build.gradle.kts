import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

group = "org.aincraft"
version = "1.0-SNAPSHOT"

subprojects {
    apply(plugin = "java")
    apply(plugin = "java-library")

    configure<JavaPluginExtension> {
        toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    }
}

/**
 * Fast sync task for development - copies assets directly to ForcePack folder.
 * Use this for quick texture iteration without rebuilding the ZIP.
 *
 * Usage: gradle syncResourcePack
 * Continuous: gradle -t syncResourcePack (watches for changes)
 */
tasks.register<Sync>("syncResourcePack") {
    group = "build"
    description = "Syncs resource pack assets to ForcePack folder for development"

    from("jobs-core/src/main/resources/assets")
    into("jobs-core/run/plugins/ForcePack/modularjobs-resources/assets")

    // Capture path at configuration time
    val packMcmetaPath = file("jobs-core/run/plugins/ForcePack/modularjobs-resources/pack.mcmeta")

    // Also sync pack.mcmeta
    doLast {
        if (!packMcmetaPath.exists()) {
            packMcmetaPath.parentFile.mkdirs()
            packMcmetaPath.writeText("""
{
    "pack": {
        "pack_format": 75,
        "description": "ModularJobs Resource Pack"
    }
}
            """.trimIndent())
        }
        println("Synced assets to ForcePack/modularjobs-resources/")
    }
}

/**
 * Resource pack merge task for ForcePack integration.
 * Collects assets from all subprojects and merges them into a single resource pack.
 */
tasks.register("mergeResourcePack") {
    group = "build"
    description = "Merges resource pack assets from all subprojects and deploys to ForcePack"

    // Capture paths at configuration time (required for configuration cache)
    val assetDirs = subprojects.map { it.file("src/main/resources/assets") } +
        listOf(file("src/main/resources/assets"))
    val outputDir = layout.buildDirectory.dir("resource-pack").get().asFile
    val zipFilePath = File(outputDir, "modularjobs-resources.zip")
    val forcePackDir = file("jobs-core/run/plugins/ForcePack")

    doLast {
        outputDir.deleteRecursively()
        outputDir.mkdirs()

        val assetsDir = File(outputDir, "assets")
        assetsDir.mkdirs()

        // Collect assets from all captured directories
        assetDirs.forEach { sourceDir ->
            if (sourceDir.exists()) {
                sourceDir.walkTopDown().forEach { file ->
                    if (file.isFile) {
                        val relativePath = file.relativeTo(sourceDir)
                        val targetFile = File(assetsDir, relativePath.path)
                        targetFile.parentFile.mkdirs()
                        file.copyTo(targetFile, overwrite = true)
                    }
                }
            }
        }

        // Create pack.mcmeta
        val packMcmeta = File(outputDir, "pack.mcmeta")
        packMcmeta.writeText("""
{
    "pack": {
        "pack_format": 75,
        "description": "ModularJobs Resource Pack"
    }
}
        """.trimIndent())

        // Create ZIP using Java ZipOutputStream
        ZipOutputStream(zipFilePath.outputStream()).use { zos ->
            // Add pack.mcmeta
            zos.putNextEntry(ZipEntry("pack.mcmeta"))
            packMcmeta.inputStream().use { it.copyTo(zos) }
            zos.closeEntry()

            // Add all assets
            assetsDir.walkTopDown().forEach { file ->
                if (file.isFile) {
                    val entryName = "assets/" + file.relativeTo(assetsDir).path.replace("\\", "/")
                    zos.putNextEntry(ZipEntry(entryName))
                    file.inputStream().use { it.copyTo(zos) }
                    zos.closeEntry()
                }
            }
        }

        // Generate SHA-1 hash
        val sha1 = MessageDigest.getInstance("SHA-1")
            .digest(zipFilePath.readBytes())
            .joinToString("") { "%02x".format(it) }
        File(outputDir, "sha1.txt").writeText(sha1)

        // Deploy to ForcePack directory (dev server)
        if (!forcePackDir.exists()) {
            forcePackDir.mkdirs()
        }
        zipFilePath.copyTo(File(forcePackDir, zipFilePath.name), overwrite = true)

        println("┌─────────────────────────────────────────────────────────────┐")
        println("│ Resource Pack Built Successfully                           │")
        println("├─────────────────────────────────────────────────────────────┤")
        println("│ File: ${zipFilePath.absolutePath}")
        println("│ SHA-1: $sha1")
        println("│ Size: ${zipFilePath.length() / 1024} KB")
        println("│ Deployed to: ${forcePackDir.absolutePath}")
        println("└─────────────────────────────────────────────────────────────┘")
    }
}
