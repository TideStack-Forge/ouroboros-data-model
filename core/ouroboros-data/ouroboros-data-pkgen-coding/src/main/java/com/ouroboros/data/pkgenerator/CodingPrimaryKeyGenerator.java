package com.ouroboros.data.pkgenerator;

import java.util.Map;

import com.ouroboros.data.model.PrimaryKeyGenerator;

public interface CodingPrimaryKeyGenerator extends PrimaryKeyGenerator<String> {
  String peek(Map<String, Object> record);
}
