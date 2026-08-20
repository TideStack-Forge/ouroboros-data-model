package com.ouroboros.data.model.deletepolicy;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import io.vavr.control.Try;

import com.ouroboros.data.model.DataModel;
import com.ouroboros.data.station.DataStation;

public class DeleteTransactionCoordinatorCenterTest {

  @Test
  public void testShouldResolveCoordinatorForSupportedModelPair() {
    DataModel sourceModel = mockModel("sql-source");
    DataModel recycleModel = mockModel("sql-archive");

    Try<DeleteTransactionCoordinator> result = DeleteTransactionCoordinatorCenter.getCoordinator(sourceModel, recycleModel);

    assertTrue(result.isSuccess());
  }

  @Test
  public void testShouldFailWhenNoProviderSupportsModelPair() {
    DataModel sourceModel = mockModel("memory-source");
    DataModel recycleModel = mockModel("memory-archive");

    Try<DeleteTransactionCoordinator> result = DeleteTransactionCoordinatorCenter.getCoordinator(sourceModel, recycleModel);

    assertTrue(result.isFailure());
    assertTrue(result.getCause().getMessage().contains("No delete transaction coordinator provider"));
  }

  private DataModel mockModel(String dataStationName) {
    DataModel model = mock(DataModel.class);
    @SuppressWarnings("rawtypes")
    DataStation dataStation = mock(DataStation.class);
    when(dataStation.getName()).thenReturn(dataStationName);
    when(model.getDataStation()).thenReturn(dataStation);
    when(model.getFullName()).thenReturn(dataStationName + ".Model");
    return model;
  }
}
