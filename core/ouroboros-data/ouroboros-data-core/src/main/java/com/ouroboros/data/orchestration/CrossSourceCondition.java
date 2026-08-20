package com.ouroboros.data.orchestration;
import com.ouroboros.data.dsl.SExpression;
import com.ouroboros.data.model.DataModel;
/**
 * 跨源条件信息
 * <p>
 * 封装跨数据源关联条件的信息，用于后续的条件改写。
 * </p>
 */
public record CrossSourceCondition(String fieldPath, String sourceFieldPath, DataModel relatedModel, SExpression<Boolean> condition,
                                   String localKeyName, String referenceKeyName, boolean implicitFieldPath) {
}
