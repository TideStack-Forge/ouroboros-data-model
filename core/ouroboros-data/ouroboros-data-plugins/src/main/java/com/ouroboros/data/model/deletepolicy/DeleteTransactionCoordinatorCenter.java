package com.ouroboros.data.model.deletepolicy;

import java.util.Optional;

import io.vavr.control.Try;

import com.ouroboros.data.model.DataModel;
import com.ouroboros.data.util.DataServices;

public final class DeleteTransactionCoordinatorCenter {

  private DeleteTransactionCoordinatorCenter() {
  }

  public static Try<DeleteTransactionCoordinator> getCoordinator(DataModel sourceModel, DataModel recycleModel) {
    return Try.of(() -> DataServices.getCachedReversedServiceStream(DeleteTransactionCoordinatorProvider.class)
        .filter(provider -> provider.support(sourceModel, recycleModel))
        .map(provider -> provider.build(sourceModel, recycleModel))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .findFirst()
        .orElseThrow(() -> new IllegalStateException(
            "No delete transaction coordinator provider for model pair: "
                + safeName(sourceModel) + " -> " + safeName(recycleModel)
        )));
  }

  private static String safeName(DataModel model) {
    return model == null ? "null" : model.getFullName();
  }
}
