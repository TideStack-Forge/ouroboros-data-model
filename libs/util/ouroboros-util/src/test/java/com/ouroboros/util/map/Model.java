package com.ouroboros.util.map;

/**
 * @author liansz
 */
public class Model {
  private String id;

  public Model() {
  }

  public Model(String id) {
    this.id = id;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  @Override
  public String toString() {
    return "Model{" +
        "id='" + id + '\'' +
        '}';
  }
}
