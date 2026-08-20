package com.ouroboros.data.model;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import io.vavr.control.Try;

/**
 * A TypedDataModel that supports deferred, asynchronous execution for all its methods.
 *
 * @param <PK> Primary Key Type
 * @param <M>  Model Type
 * @author Gemini
 */
public interface DeferredTypedDataModel<PK, M> extends TypedDataModel<PK, M> {

  CompletableFuture<DataModel> getDataModelAsync();

  CompletableFuture<Try<M>> insertAsync(M data);

  CompletableFuture<Try<List<M>>> batchInsertAsync(Collection<M> dataList);

  CompletableFuture<Try<Long>> deleteAsync(PK id);

  CompletableFuture<Try<Long>> deleteAsync(Collection<PK> ids);

  CompletableFuture<Try<Long>> deleteAsync(Map<String, Object> where);

  CompletableFuture<Try<Long>> updateAsync(PK id, M data);

  CompletableFuture<Try<Long>> updateAsync(PK id, Map<String, Object> data);

  CompletableFuture<Try<Long>> updateAsync(Collection<PK> ids, Map<String, Object> data);

  CompletableFuture<Try<Long>> updateAsync(Map<String, Object> where, Map<String, Object> data);

  CompletableFuture<Try<Long>> countAsync(Map<String, Object> where);

  CompletableFuture<Try<M>> getAsync(PK id);

  CompletableFuture<Try<M>> getAsync(PK id, Map<String, Object> statement);

  CompletableFuture<Try<List<M>>> queryAsync(Collection<PK> ids);

  CompletableFuture<Try<List<M>>> queryAsync(Map<String, Object> statement);

  CompletableFuture<Try<List<M>>> queryAsync(Collection<String> select, Map<String, Object> where);

  CompletableFuture<Try<List<M>>> queryAsync(Collection<String> select, Map<String, Object> where, String orderBy);

  CompletableFuture<Try<List<M>>> queryAsync(Collection<String> select, Map<String, Object> where, String orderBy, Integer offset, Integer limit);

  CompletableFuture<Boolean> hasPluginAsync(String name);
}
