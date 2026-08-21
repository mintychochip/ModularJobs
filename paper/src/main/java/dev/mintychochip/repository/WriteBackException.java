package dev.mintychochip.repository;

/** Signals a write-back persistence failure while preserving the original cause. */
public final class WriteBackException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public WriteBackException(String message, Throwable cause) {
    super(message, cause);
  }
}
