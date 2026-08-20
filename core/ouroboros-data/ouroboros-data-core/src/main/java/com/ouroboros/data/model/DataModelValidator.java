package com.ouroboros.data.model;

/**
 * Validates a fully decorated model definition before it is published.
 */
public interface DataModelValidator {

  /**
   * Validate the model definition.
   *
   * @param model fully decorated model
   */
  void validate(DataModel model);

  /**
   * @return lower values run earlier
   */
  default int getOrder() {
    return 0;
  }
}
