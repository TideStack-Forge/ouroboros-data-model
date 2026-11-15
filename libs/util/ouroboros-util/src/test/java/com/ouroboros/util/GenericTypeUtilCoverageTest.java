package com.ouroboros.util;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class GenericTypeUtilCoverageTest {

  private interface Holder<T> {
  }

  private static class DirectHolder implements Holder<Integer> {
  }

  private static class Parent implements Holder<String> {
  }

  private static class Child extends Parent {
  }

  private static class Base<T> {
  }

  private static class BaseChild extends Base<Long> {
  }

  @Test
  public void resolvesDirectSuperclassAndInterfaceGenerics() {
    assertArrayEquals(new Class<?>[]{Long.class}, GenericTypeUtil.getGenericType(new BaseChild(), Base.class));
    assertArrayEquals(new Class<?>[]{Integer.class}, GenericTypeUtil.getGenericType(new DirectHolder(), Holder.class));
  }

  @Test
  public void resolvesGenericsThroughParentTraversal() {
    assertArrayEquals(new Class<?>[]{String.class}, GenericTypeUtil.getGenericType(new Child(), Holder.class));
  }

  @Test
  public void returnsEmptyArrayWhenTargetTypeIsMissing() {
    assertEquals(0, GenericTypeUtil.getGenericType(new Object(), Holder.class).length);
  }
}
