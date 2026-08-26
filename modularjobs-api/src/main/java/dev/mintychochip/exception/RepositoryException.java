package dev.mintychochip.exception;

/** Indicates a failure while accessing or persisting repository data. */
public class RepositoryException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  /** Creates an exception with the repository failure cause. */
  public RepositoryException(Throwable cause) {
    super(cause);
  }

  /** Creates an exception with a message and repository failure cause. */
  public RepositoryException(String message, Throwable cause) {
    super(message, cause);
  }

  /** Creates an exception with a description of the repository failure. */
  public RepositoryException(String message) {
    super(message);
  }

  /** Creates an exception without a message or cause. */
  public RepositoryException() {}
}
