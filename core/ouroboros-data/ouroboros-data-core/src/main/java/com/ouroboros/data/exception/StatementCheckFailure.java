package com.ouroboros.data.exception;

/**
 * 验证失败
 *
 * @author Song Mingxu
 * @version 1.0.0
 */
public class StatementCheckFailure extends StatementError {
  private static final long serialVersionUID = 1L;
  private final String fieldName;
  private final FailureType failureType;

  public StatementCheckFailure(String fieldName, FailureType failureType, String extMessage) {
    super(extMessage);
    this.fieldName = fieldName;
    this.failureType = failureType;
  }

  public String getFieldName() {
    return fieldName;
  }

  public FailureType getFailureType() {
    return failureType;
  }

  public enum FailureType {
    TYPE_MISMATCH("TYPE_MISMATCH", "数据类型不匹配"),
    VALUE_REQUIRED("VALUE_REQUIRED", "值不能为空"),
    FORMAT_MISMATCH("FORMAT_MISMATCH", "格式不匹配"),
    RANGE_MISMATCH("RANGE_MISMATCH", "范围不匹配"),
    DATA_LOGIC_ERROR("DATA_LOGIC_ERROR", "数据逻辑错误"),
    INVALID_FIELD("INVALID_FIELD", "无效字段"),
    MISSING_FIELD("FIELD_IS_MISSING", "缺少字段"),
    FIELD_NOT_EXPECTED("FIELD_NOT_EXPECTED", "不应该出现的字段"),
    OTHER("OTHER", "其他错误");

    private final String typeName;
    private final String typeLabel;

    FailureType(String typeName, String typeLabel) {
      this.typeName = typeName;
      this.typeLabel = typeLabel;
    }

    public String getTypeName() {
      return typeName;
    }

    public String getTypeLabel() {
      return typeLabel;
    }
  }
}