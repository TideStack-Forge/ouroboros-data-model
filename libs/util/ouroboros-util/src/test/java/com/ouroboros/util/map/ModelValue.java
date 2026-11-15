package com.ouroboros.util.map;

/**
 * @author liansz
 */
public abstract class ModelValue<P, M> {

  public static <P, M> Primary<P, M> primary(P primary) {
    return new Primary<>(primary);
  }

  public static <P, M> Model<P, M> model(M model) {
    return new Model<>(model);
  }

  public abstract Object get();

  public abstract void set(Object object);

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
    public void set(Object object) {
      this.primary = (P) object;
    }

    @Override
    public String toString() {
      return "Primary{" +
          "primary=" + primary +
          '}';
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
    public void set(Object object) {
      this.model = (M) object;
    }

    public M getModel() {
      return model;
    }

    public void setModel(M model) {
      this.model = model;
    }

    @Override
    public String toString() {
      return "Model{" +
          "model=" + model +
          '}';
    }
  }
}
