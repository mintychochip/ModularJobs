package dev.mintychochip.repository;

import java.io.FileNotFoundException;
import java.io.InputStream;
import org.jetbrains.annotations.ApiStatus.Internal;

/** Internal helper for reading packaged classpath resources (e.g. SQL schema files). */
@Internal
interface ResourceExtractor {

  /**
   * Opens the classpath resource at {@code filePath}.
   *
   * @param filePath classpath-relative resource path
   * @return an open {@link InputStream} for the resource; the caller must close it
   * @throws FileNotFoundException if no such resource exists
   */
  static InputStream getResourceStream(String filePath) throws FileNotFoundException {
    ClassLoader loader = Thread.currentThread().getContextClassLoader();
    InputStream resourceStream = loader.getResourceAsStream(filePath);
    if (resourceStream == null) {
      throw new FileNotFoundException(filePath);
    }
    return resourceStream;
  }
}
