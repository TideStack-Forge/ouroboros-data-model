package com.ouroboros.util.map;

/**
 * @author liansz
 */
public class Pack {

  private ModelValue<String, Model> a;

  public Pack() {
  }

  public Pack(ModelValue<String, Model> a) {
    this.a = a;
  }

  public ModelValue<String, Model> getA() {
    return a;
  }

  public void setA(ModelValue<String, Model> a) {
    this.a = a;
  }

  @Override
  public String toString() {
    return "Pack{" +
        "a=" + a +
        '}';
  }
}
