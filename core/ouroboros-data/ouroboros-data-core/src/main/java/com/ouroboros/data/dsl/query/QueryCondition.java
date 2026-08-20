package com.ouroboros.data.dsl.query;

import java.util.Map;

/**
 * Query facade condition that can render itself to the existing raw WHERE map language.
 */
public interface QueryCondition {

  Map<String, Object> toRawCondition();
}
