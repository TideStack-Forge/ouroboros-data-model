package com.ouroboros.common.service;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * @author liansz
 */
public interface SoftDelete {

  @JsonIgnore
  default String getDeletedFieldName() {
    return "deleted";
  }

  /**
   * 获取删除标记
   *
   * @return
   */
  boolean isDeleted();

  /**
   * 设置删除标记
   *
   * @param deleted
   */
  void setDeleted(boolean deleted);
}
