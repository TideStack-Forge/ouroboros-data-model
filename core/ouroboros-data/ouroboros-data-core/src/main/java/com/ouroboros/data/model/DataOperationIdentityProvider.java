package com.ouroboros.data.model;

import java.util.Optional;

/**
 * Provides the current operator identity for data model plugins.
 */
public interface DataOperationIdentityProvider {

  Optional<String> findCurrentOperator();
}
