package com.ouroboros.data.pkgenerator;

import java.util.Map;

@SuppressWarnings("unused")
public interface CodeGenerator {
  String get(String codingTemplate);

  String get(String codingTemplate, Map<String, Object> record);

  String peek(String codingTemplate);

  String peek(String codingTemplate, Map<String, Object> record);
}
