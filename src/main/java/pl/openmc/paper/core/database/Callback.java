package pl.openmc.paper.core.database;

/**
 * Callback interface for asynchronous operations.
 *
 * @param <T> The type of result the callback will handle
 */
public interface Callback<T> {
  /**
   * Called when the operation completes successfully.
   *
   * @param result The result of the operation
   */
  void done(T result);

  /**
   * Called when the operation fails.
   *
   * @param e The exception that caused the failure
   */
  void error(Exception e);
}