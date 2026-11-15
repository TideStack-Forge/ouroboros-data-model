package com.ouroboros.util;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Type;
import java.util.List;

import org.junit.Test;

public class TypesCoverageTest {

  private List<String> names;

  @Test
  public void resolvesRawClassesFromCommonTypes() throws Exception {
    assertEquals(String.class, Types.rawClass(String.class));
    Type namesType = TypesCoverageTest.class.getDeclaredField("names").getGenericType();
    assertEquals(List.class, Types.rawClass(namesType));
  }
}
