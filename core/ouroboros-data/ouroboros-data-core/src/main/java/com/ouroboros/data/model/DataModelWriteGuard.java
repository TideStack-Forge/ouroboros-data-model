package com.ouroboros.data.model;

/** Verifies a model's resource before any write or write plugin can produce effects. */
public interface DataModelWriteGuard {
  void beforeWrite(DataModel model);

  /** Invalidates the owning transaction even if business code catches the write failure. */
  void markRollbackOnly();
}
