package com.ouroboros.data.model;

import java.io.Serializable;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.ouroboros.data.model.serializer.json.ModelValueJsonDeserializer;
import com.ouroboros.data.model.serializer.json.ModelValueJsonSerializer;

/**
 * @author liansz
 */
@JsonSerialize(using = ModelValueJsonSerializer.class)
@JsonDeserialize(using = ModelValueJsonDeserializer.class)
public abstract class ModelValue<P, M> implements Serializable {

  public static <P, M> Primary<P, M> primary(P primary) {
    return new Primary<>(primary);
  }

  public static <P, M> Model<P, M> model(M model) {
    return new Model<>(model);
  }

  public static <P, M> P getPrimary(ModelValue<P, M> modelValue) {
    return modelValue == null ? null : modelValue.isPrimary() ? modelValue.getPrimary() : null;
  }

  public static <P, M> M getModel(ModelValue<P, M> modelValue) {
    return modelValue == null ? null : modelValue.isModel() ? modelValue.getModel() : null;
  }

  public boolean isPrimary() {
    return this instanceof Primary;
  }

  public boolean isModel() {
    return this instanceof Model;
  }

  public P getPrimary() {
    return (P) get();
  }

  public M getModel() {
    return (M) get();
  }

  public abstract Object get();

  public static class Primary<P, M> extends ModelValue<P, M> {
    private P primary;

    private Primary() {
    }

    private Primary(P primary) {
      this.primary = primary;
    }

    @Override
    public Object get() {
      return primary;
    }

    @Override
    public P getPrimary() {
      return primary;
    }
  }

  public static class Model<P, M> extends ModelValue<P, M> {
    private M model;

    public Model() {
    }

    private Model(M model) {
      this.model = model;
    }

    @Override
    public Object get() {
      return model;
    }

    @Override
    public M getModel() {
      return model;
    }
  }
}
