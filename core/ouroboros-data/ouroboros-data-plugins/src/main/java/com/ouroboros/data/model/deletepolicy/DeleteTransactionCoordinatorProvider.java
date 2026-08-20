package com.ouroboros.data.model.deletepolicy;

import java.util.Optional;

import com.ouroboros.data.model.DataModel;

public interface DeleteTransactionCoordinatorProvider {

  boolean support(DataModel sourceModel, DataModel recycleModel);

  Optional<DeleteTransactionCoordinator> build(DataModel sourceModel, DataModel recycleModel);
}
