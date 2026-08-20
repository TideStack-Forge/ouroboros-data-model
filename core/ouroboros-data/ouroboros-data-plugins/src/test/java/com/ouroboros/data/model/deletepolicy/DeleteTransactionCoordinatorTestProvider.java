package com.ouroboros.data.model.deletepolicy;

import java.util.Optional;

import io.vavr.control.Try;

import com.ouroboros.data.model.DataModel;

public class DeleteTransactionCoordinatorTestProvider implements DeleteTransactionCoordinatorProvider {

  @Override
  public boolean support(DataModel sourceModel, DataModel recycleModel) {
    String sourceStation = sourceModel.getDataStation() == null ? "" : sourceModel.getDataStation().getName();
    String recycleStation = recycleModel.getDataStation() == null ? "" : recycleModel.getDataStation().getName();
    return sourceStation.startsWith("sql") && recycleStation.startsWith("sql");
  }

  @Override
  public Optional<DeleteTransactionCoordinator> build(DataModel sourceModel, DataModel recycleModel) {
    return Optional.of(new DeleteTransactionCoordinator() {
      @Override
      public <T> Try<T> execute(DataModel source, DataModel recycle, java.util.function.Supplier<Try<T>> work) {
        return work.get();
      }
    });
  }
}
