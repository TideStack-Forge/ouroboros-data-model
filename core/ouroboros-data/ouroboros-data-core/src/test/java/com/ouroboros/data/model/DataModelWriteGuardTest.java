package com.ouroboros.data.model;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.ouroboros.data.adapter.DataAdapter;
import com.ouroboros.data.dsl.SExpression;
import com.ouroboros.data.exception.DataModelException;
import com.ouroboros.data.station.DataStation;

class DataModelWriteGuardTest {
  @Test
  void unverifiedResourcesCannotReachCoreWritesOrPlugins() {
    var adapter = mock(DataAdapter.class);
    var station = mock(DataStation.class);
    when(station.getDataAdapter()).thenReturn(adapter);
    var meta = new DataModelMeta();
    meta.setName("Order");
    meta.setNamespace("sales");
    meta.setRawName("orders");
    DataModel core = new DefaultDataModel(meta, station);
    DataModel enhanced = new EnhancedDataModelProxy(core, List.of());
    var guard = mock(DataModelWriteGuard.class);
    doThrow(new DataModelException("unverified resource")).when(guard).beforeWrite(org.mockito.ArgumentMatchers.any());
    for (DataModel model : List.of(core, enhanced)) {
      DataModelExecutionContext.withWriteGuard(guard, () -> {
        assertTrue(model.insert(Map.of("id", "1")).isFailure());
        assertTrue(model.batchInsert(List.of(Map.of("id", "1"))).isFailure());
        assertTrue(model.update(SExpression.constant(true), Map.of("id", "1")).isFailure());
        assertTrue(model.delete(SExpression.constant(true)).isFailure());
        return null;
      });
    }
    verifyNoInteractions(adapter);
    assertNull(DataModelExecutionContext.getWriteGuard());
  }

  @Test
  void nestedExecutionCannotWeakenTheEnclosingGuardAndFailureRestoresTheScope() {
    var verified = mock(DataModel.class);
    var denied = mock(DataModel.class);
    var outer = mock(DataModelWriteGuard.class);
    var inner = mock(DataModelWriteGuard.class);
    doAnswer(invocation -> {
      if (invocation.getArgument(0) == denied) {
        throw new DataModelException("denied");
      }
      return null;
    }).when(outer).beforeWrite(org.mockito.ArgumentMatchers.any());
    DataModelExecutionContext.withWriteGuard(outer, () -> {
      DataModelExecutionContext.withExecution(false, null, () -> {
        DataModelExecutionContext.beforeWrite(verified);
        return null;
      });
      assertThrows(DataModelException.class, () -> DataModelExecutionContext.withWriteGuard(inner, () -> {
        DataModelExecutionContext.beforeWrite(denied);
        return null;
      }));
      DataModelExecutionContext.beforeWrite(verified);
      DataModelExecutionContext.withWriteGuard(inner, () -> {
        DataModelExecutionContext.markRollbackOnly();
        return null;
      });
      return null;
    });
    verify(outer, org.mockito.Mockito.times(2)).beforeWrite(verified);
    verify(outer).beforeWrite(denied);
    verify(outer).markRollbackOnly();
    verify(inner).markRollbackOnly();
    assertNull(DataModelExecutionContext.getWriteGuard());
  }
}
