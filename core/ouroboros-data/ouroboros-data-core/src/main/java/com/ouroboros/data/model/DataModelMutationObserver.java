package com.ouroboros.data.model;

/** Observes successful writes made through the public DataModel API. */
@FunctionalInterface
public interface DataModelMutationObserver {
  void onMutation(DataModelMutation mutation);
}
