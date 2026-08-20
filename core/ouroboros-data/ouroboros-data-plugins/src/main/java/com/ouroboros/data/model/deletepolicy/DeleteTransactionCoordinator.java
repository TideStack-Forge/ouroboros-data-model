package com.ouroboros.data.model.deletepolicy;

import java.util.function.Supplier;

import io.vavr.control.Try;

import com.ouroboros.data.model.DataModel;

public interface DeleteTransactionCoordinator {

  <T> Try<T> execute(DataModel sourceModel, DataModel recycleModel, Supplier<Try<T>> work);
}
