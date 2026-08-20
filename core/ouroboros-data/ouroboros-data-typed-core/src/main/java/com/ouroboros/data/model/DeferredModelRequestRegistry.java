package com.ouroboros.data.model;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

/**
 * Coordinates pending deferred typed model requests.
 */
public class DeferredModelRequestRegistry {

  private final Map<String, List<DeferredModelRequest<?, ?>>> pendingRequests = new LinkedHashMap<>();
  private boolean refreshCompleted;

  public synchronized <PK, M> DeferredTypedDataModel<PK, M> resolveOrDefer(
      String modelName,
      Class<PK> pkClass,
      Class<M> modelClass,
      Function<String, Optional<DataModel>> resolver
  ) {
    var future = new CompletableFuture<TypedDataModel<PK, M>>();
    var request = new DeferredModelRequest<>(future, pkClass, modelClass);

    var dataModel = resolver.apply(modelName);
    if (dataModel.isPresent()) {
      request.complete(dataModel.get());
    } else if (refreshCompleted) {
      request.completeExceptionally(modelNotFoundException(modelName, pkClass, modelClass));
    } else {
      pendingRequests.computeIfAbsent(modelName, k -> new ArrayList<>()).add(request);
    }

    return deferredProxy(future);
  }

  public synchronized void completePending(Function<String, Optional<DataModel>> resolver) {
    refreshCompleted = true;
    Map<String, List<DeferredModelRequest<?, ?>>> requests = new LinkedHashMap<>(pendingRequests);
    pendingRequests.clear();

    for (var entry : requests.entrySet()) {
      var dataModel = resolver.apply(entry.getKey());
      if (dataModel.isPresent()) {
        completeRequests(entry.getValue(), dataModel.get());
      } else {
        completeRequestsExceptionally(entry.getKey(), entry.getValue());
      }
    }
  }

  public synchronized int getPendingRequestCount() {
    return pendingRequests.values().stream().mapToInt(List::size).sum();
  }

  private void completeRequests(List<DeferredModelRequest<?, ?>> requests, DataModel dataModel) {
    for (DeferredModelRequest<?, ?> request : requests) {
      request.complete(dataModel);
    }
  }

  private void completeRequestsExceptionally(String modelName, List<DeferredModelRequest<?, ?>> requests) {
    for (DeferredModelRequest<?, ?> request : requests) {
      request.completeExceptionally(modelNotFoundException(modelName, request.pkClass, request.modelClass));
    }
  }

  private IllegalStateException modelNotFoundException(String modelName, Class<?> pkClass, Class<?> modelClass) {
    return new IllegalStateException(
        "Model not found after refresh: " + modelName
            + ", pkClass=" + (pkClass == null ? "unknown" : pkClass.getName())
            + ", modelClass=" + modelClass.getName()
    );
  }

  private <PK, M> DeferredTypedDataModel<PK, M> deferredProxy(CompletableFuture<TypedDataModel<PK, M>> future) {
    return DeferredTypedDataModel.class.cast(Proxy.newProxyInstance(
        DeferredTypedDataModel.class.getClassLoader(),
        new Class<?>[]{DeferredTypedDataModel.class},
        new DeferredTypedDataModelProxy<>(future)
    ));
  }

  private static class DeferredTypedDataModelProxy<PK, M> implements InvocationHandler {
    private final CompletableFuture<TypedDataModel<PK, M>> modelFuture;

    private DeferredTypedDataModelProxy(CompletableFuture<TypedDataModel<PK, M>> modelFuture) {
      this.modelFuture = modelFuture;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      if (method.getDeclaringClass().equals(Object.class)) {
        return method.invoke(this, args);
      }

      if (CompletableFuture.class.isAssignableFrom(method.getReturnType())) {
        return modelFuture.thenApplyAsync(model -> invokeAsyncDelegate(model, method, args));
      }

      if (!modelFuture.isDone()) {
        throw new IllegalStateException("Model is not ready. Please wait for the typed model refresh bridge to complete.");
      }
      try {
        return method.invoke(modelFuture.get(), args);
      } catch (ExecutionException e) {
        throw e.getCause();
      }
    }

    private Object invokeAsyncDelegate(TypedDataModel<PK, M> model, Method method, Object[] args) {
      try {
        String syncMethodName = method.getName().replace("Async", "");
        Method syncMethod = TypedDataModel.class.getMethod(syncMethodName, method.getParameterTypes());
        return syncMethod.invoke(model, args);
      } catch (InvocationTargetException e) {
        throw new CompletionException(e.getTargetException());
      } catch (Exception e) {
        throw new CompletionException(e);
      }
    }
  }
}

/**
 * A holder for the information needed to fulfill a deferred model request.
 * This is a package-private class.
 */
class DeferredModelRequest<PK, T> {
  final CompletableFuture<TypedDataModel<PK, T>> future;
  final Class<PK> pkClass;
  final Class<T> modelClass;

  DeferredModelRequest(CompletableFuture<TypedDataModel<PK, T>> future, Class<PK> pkClass, Class<T> modelClass) {
    this.future = future;
    this.pkClass = pkClass;
    this.modelClass = modelClass;
  }

  /**
   * Completes the future with a properly typed data model.
   *
   * @param dataModel The raw DataModel from the center.
   */
  void complete(DataModel dataModel) {
    future.complete(new BaseTypedDataModel<>(dataModel, modelClass));
  }

  /**
   * Completes the future exceptionally.
   *
   * @param exception The failure reason.
   */
  void completeExceptionally(Throwable exception) {
    future.completeExceptionally(exception);
  }
}
