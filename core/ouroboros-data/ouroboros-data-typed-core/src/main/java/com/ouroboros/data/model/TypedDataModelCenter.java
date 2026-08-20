package com.ouroboros.data.model;

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import com.ouroboros.data.annotation.Model;

/**
 * @author liansz
 */
public class TypedDataModelCenter {
  private static volatile DeferredModelRequestRegistry deferredModelRequestRegistry;

  private static String getModelName(Class<?> clazz) {
    Model model = clazz.getAnnotation(Model.class);
    if (model != null && StringUtils.isNotBlank(model.fullName())) {
      return model.fullName().trim();
    }
    return clazz.getSimpleName();
  }

  public static void setDeferredModelRequestRegistry(DeferredModelRequestRegistry registry) {
    deferredModelRequestRegistry = registry;
  }

  public static void clearDeferredModelRequestRegistry(DeferredModelRequestRegistry registry) {
    if (deferredModelRequestRegistry == registry) {
      deferredModelRequestRegistry = null;
    }
  }

  public static void clearDeferredModelRequestRegistry() {
    deferredModelRequestRegistry = null;
  }

  private static DeferredModelRequestRegistry getDeferredModelRequestRegistry() {
    if (deferredModelRequestRegistry == null) {
      throw new IllegalStateException(
          "DeferredModelRequestRegistry is not available. Make sure typed model runtime is initialized.");
    }
    return deferredModelRequestRegistry;
  }

  /**
   * Immediately retrieves a data model.
   * This method is now internal as the primary way to get a model should be the deferred method.
   */
  public static <PK, T> Optional<TypedDataModel<PK, T>> getDataModel(Class<T> clazz) {
    return DataModelCenter.getDataModel(getModelName(clazz))
        .map(dataModel -> new BaseTypedDataModel<>(dataModel, clazz));
  }

  public static <PK, T> Optional<TypedDataModel<PK, T>> getDataModel(Class<PK> pkClass, Class<T> clazz) {
    return DataModelCenter.getDataModel(getModelName(clazz))
        .map(dataModel -> new BaseTypedDataModel<>(dataModel, clazz));
  }

  public static <PK, T> DeferredTypedDataModel<PK, T> getDeferredDataModel(Class<T> clazz) {
    return getDeferredDataModel(null, clazz);
  }

  public static <PK, T> DeferredTypedDataModel<PK, T> getDeferredDataModel(Class<PK> pkClass, Class<T> clazz) {
    String modelName = getModelName(clazz);
    return getDeferredModelRequestRegistry().resolveOrDefer(modelName, pkClass, clazz, DataModelCenter::getDataModel);
  }
}
