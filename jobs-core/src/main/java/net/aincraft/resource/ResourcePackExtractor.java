package net.aincraft.resource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Logger;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Extracts resource pack assets from the plugin JAR to ForcePack directory.
 * This enables automatic resource pack deployment without manual server configuration.
 */
public final class ResourcePackExtractor {

  private static final String ASSETS_PREFIX = "assets/";
  private static final String MINECRAFT_NAMESPACE = "assets/minecraft/";
  private static final String FORCEPACK_FOLDER = "ForcePack";
  private static final String RESOURCE_FOLDER = "aincraft-resources";
  private static final String PACK_MCMETA = "pack.mcmeta";
  private static final String HASH_FILE = ".modularjobs-hash";
  private static final int PACK_FORMAT = 46; // 1.21.2-1.21.4

  private final JavaPlugin plugin;
  private final Logger logger;

  public ResourcePackExtractor(JavaPlugin plugin) {
    this.plugin = plugin;
    this.logger = plugin.getLogger();
  }

  /**
   * Extract resource pack assets from JAR to ForcePack folder.
   * Only extracts if assets have changed (based on JAR hash).
   *
   * @return true if extraction was performed or skipped (up-to-date), false on error
   */
  public boolean extract() {
    Path pluginsFolder = plugin.getDataFolder().toPath().getParent();
    Path forcePackDir = pluginsFolder.resolve(FORCEPACK_FOLDER);

    // Check if ForcePack plugin folder exists
    if (!Files.exists(forcePackDir)) {
      logger.info("ForcePack folder not found - skipping resource pack extraction");
      logger.info("Install ForcePack plugin for automatic resource pack deployment");
      return true;
    }

    Path resourceDir = forcePackDir.resolve(RESOURCE_FOLDER);
    Path hashFile = resourceDir.resolve(HASH_FILE);

    try {
      // Calculate current JAR hash
      String currentHash = calculateJarHash();
      if (currentHash == null) {
        logger.warning("Could not calculate JAR hash - extracting anyway");
      }

      // Check if extraction is needed
      if (currentHash != null && Files.exists(hashFile)) {
        String storedHash = Files.readString(hashFile).trim();
        if (currentHash.equals(storedHash)) {
          logger.info("Resource pack assets are up-to-date");
          return true;
        }
      }

      // Perform extraction
      logger.info("Extracting resource pack assets to ForcePack...");
      extractAssets(resourceDir);
      createPackMcmeta(resourceDir);

      // Store hash for future comparisons
      if (currentHash != null) {
        Files.writeString(hashFile, currentHash);
      }

      logger.info("Resource pack extraction complete: " + resourceDir);
      return true;

    } catch (IOException e) {
      logger.severe("Failed to extract resource pack: " + e.getMessage());
      e.printStackTrace();
      return false;
    }
  }

  private void extractAssets(Path targetDir) throws IOException {
    // Get the plugin JAR file path as string first to handle URL encoding
    String jarPathStr = plugin.getClass().getProtectionDomain()
        .getCodeSource().getLocation().getPath();

    // Handle URL encoding in path (spaces become %20, etc.)
    jarPathStr = java.net.URLDecoder.decode(jarPathStr, java.nio.charset.StandardCharsets.UTF_8);
    // Remove leading slash on Windows (e.g., /C:/path -> C:/path)
    if (jarPathStr.startsWith("/") && System.getProperty("os.name").toLowerCase().contains("win")) {
      jarPathStr = jarPathStr.substring(1);
    }

    try (JarFile jar = new JarFile(jarPathStr)) {
      var entries = jar.entries();
      int extractedCount = 0;

      while (entries.hasMoreElements()) {
        JarEntry entry = entries.nextElement();
        String name = entry.getName();

        // Only extract assets/ folder contents, skip minecraft namespace (no vanilla overrides)
        if (!name.startsWith(ASSETS_PREFIX) || name.startsWith(MINECRAFT_NAMESPACE)) {
          continue;
        }

        Path targetPath = targetDir.resolve(name);

        if (entry.isDirectory()) {
          Files.createDirectories(targetPath);
        } else {
          // Ensure parent directories exist
          Files.createDirectories(targetPath.getParent());

          // Extract file
          try (InputStream in = jar.getInputStream(entry)) {
            Files.copy(in, targetPath, StandardCopyOption.REPLACE_EXISTING);
            extractedCount++;
          }
        }
      }

      logger.info("Extracted " + extractedCount + " resource pack files");
    }
  }

  private void createPackMcmeta(Path targetDir) throws IOException {
    Path packMcmeta = targetDir.resolve(PACK_MCMETA);

    String content = """
        {
            "pack": {
                "pack_format": %d,
                "description": "ModularJobs Resource Pack"
            }
        }
        """.formatted(PACK_FORMAT);

    Files.writeString(packMcmeta, content);
  }

  private String calculateJarHash() {
    try {
      // Get the plugin JAR file path as string first to handle URL encoding
      String jarPathStr = plugin.getClass().getProtectionDomain()
          .getCodeSource().getLocation().getPath();

      // Handle URL encoding in path (spaces become %20, etc.)
      jarPathStr = java.net.URLDecoder.decode(jarPathStr, java.nio.charset.StandardCharsets.UTF_8);
      // Remove leading slash on Windows (e.g., /C:/path -> C:/path)
      if (jarPathStr.startsWith("/") && System.getProperty("os.name").toLowerCase().contains("win")) {
        jarPathStr = jarPathStr.substring(1);
      }

      MessageDigest digest = MessageDigest.getInstance("SHA-1");
      byte[] fileBytes = Files.readAllBytes(Path.of(jarPathStr));
      byte[] hashBytes = digest.digest(fileBytes);

      StringBuilder sb = new StringBuilder();
      for (byte b : hashBytes) {
        sb.append(String.format("%02x", b));
      }
      return sb.toString();

    } catch (IOException | NoSuchAlgorithmException e) {
      return null;
    }
  }
}
