package com.ouroboros.data.model;

import java.util.Objects;
import java.util.List;
import java.util.function.Supplier;

/** Scoped execution constraints shared by DataModel operations on the current thread. */
public final class DataModelExecutionContext {
  private static final ThreadLocal<Frame> CURRENT = new ThreadLocal<>();

  private DataModelExecutionContext() {
  }

  public static boolean isReadOnly() {
    Frame frame = CURRENT.get();
    return frame != null && frame.readOnly;
  }

  public static boolean isMutationTrackingEnabled() {
    Frame frame = CURRENT.get();
    return frame != null && frame.mutationObserver != null;
  }

  public static DataModelWriteGuard getWriteGuard() {
    Frame frame = CURRENT.get();
    return frame == null ? null : frame.writeGuard;
  }

  public static void beforeWrite(DataModel model) {
    DataModelWriteGuard guard = getWriteGuard();
    if (guard != null) {
      guard.beforeWrite(model);
    }
  }

  public static void markRollbackOnly() {
    DataModelWriteGuard guard = getWriteGuard();
    if (guard != null) {
      guard.markRollbackOnly();
    }
  }

  public static <T> T withWriteGuard(DataModelWriteGuard guard, Supplier<T> operation) {
    Objects.requireNonNull(guard, "guard must not be null");
    return withExecution(false, null, () -> {
      Frame frame = CURRENT.get();
      DataModelWriteGuard previousGuard = frame.writeGuard;
      frame.writeGuard = previousGuard == null ? guard : new DataModelWriteGuard() {
        @Override
        public void beforeWrite(DataModel model) {
          previousGuard.beforeWrite(model);
          guard.beforeWrite(model);
        }

        @Override
        public void markRollbackOnly() {
          previousGuard.markRollbackOnly();
          guard.markRollbackOnly();
        }
      };
      return operation.get();
    });
  }

  public static String getMutationOperation(String fallback) {
    Frame frame = CURRENT.get();
    return frame == null || frame.mutationOperation == null ? fallback : frame.mutationOperation;
  }

  public static <T> T withReadOnly(boolean readOnly, Supplier<T> operation) {
    return withExecution(readOnly, null, operation);
  }

  public static <T> T withExecution(boolean readOnly,
                                    DataModelMutationObserver mutationObserver,
                                    Supplier<T> operation) {
    Objects.requireNonNull(operation, "operation must not be null");
    Frame previous = CURRENT.get();
    boolean effectiveReadOnly = readOnly || (previous != null && previous.readOnly);
    DataModelMutationObserver effectiveObserver = mutationObserver != null
        ? mutationObserver : previous == null ? null : previous.mutationObserver;
    boolean newCapture = mutationObserver != null && (previous == null || mutationObserver != previous.mutationObserver);
    Frame current = new Frame(effectiveReadOnly, effectiveObserver,
        newCapture ? null : previous == null ? null : previous.mutationOperation,
        newCapture ? null : previous == null ? null : previous.mutationFailure);
    current.writeGuard = previous == null ? null : previous.writeGuard;
    CURRENT.set(current);
    try {
      return operation.get();
    } finally {
      if (previous == null) {
        CURRENT.remove();
      } else {
        if (!newCapture && previous.mutationFailure == null) {
          previous.mutationFailure = current.mutationFailure;
        }
        CURRENT.set(previous);
      }
    }
  }

  public static void recordMutation(String modelFullName, String operation, List<String> objectIds, long affectedRows) {
    Frame frame = CURRENT.get();
    if (frame != null && frame.mutationObserver != null && affectedRows > 0) {
      frame.mutationObserver.onMutation(new DataModelMutation(modelFullName, operation, objectIds, affectedRows));
    }
  }

  public static void recordMutationFailure(Throwable failure) {
    Frame frame = CURRENT.get();
    if (frame != null && frame.mutationObserver != null && frame.mutationFailure == null) {
      frame.mutationFailure = Objects.requireNonNull(failure, "failure must not be null");
    }
  }

  public static void assertMutationTrackingHealthy() {
    Frame frame = CURRENT.get();
    if (frame != null && frame.mutationFailure != null) {
      throw new com.ouroboros.data.exception.DataModelException(
          "业务 Action 的写集无法准确审计", frame.mutationFailure);
    }
  }

  public static <T> T withMutationOperation(String operation, Supplier<T> callback) {
    Objects.requireNonNull(operation, "operation must not be null");
    Objects.requireNonNull(callback, "callback must not be null");
    Frame previous = CURRENT.get();
    if (previous == null) {
      return callback.get();
    }
    String previousOperation = previous.mutationOperation;
    previous.mutationOperation = operation;
    try {
      return callback.get();
    } finally {
      previous.mutationOperation = previousOperation;
    }
  }

  /** Runs platform bookkeeping outside business write guards and the enclosing write set. */
  public static <T> T withPlatformWrite(Supplier<T> operation) {
    Objects.requireNonNull(operation, "operation must not be null");
    Frame previous = CURRENT.get();
    CURRENT.set(new Frame(false, null, null, null));
    try {
      return operation.get();
    } finally {
      if (previous == null) {
        CURRENT.remove();
      } else {
        CURRENT.set(previous);
      }
    }
  }

  private static final class Frame {
    private final boolean readOnly;
    private final DataModelMutationObserver mutationObserver;
    private String mutationOperation;
    private Throwable mutationFailure;
    private DataModelWriteGuard writeGuard;

    private Frame(boolean readOnly,
                  DataModelMutationObserver mutationObserver,
                  String mutationOperation,
                  Throwable mutationFailure) {
      this.readOnly = readOnly;
      this.mutationObserver = mutationObserver;
      this.mutationOperation = mutationOperation;
      this.mutationFailure = mutationFailure;
    }
  }
}
