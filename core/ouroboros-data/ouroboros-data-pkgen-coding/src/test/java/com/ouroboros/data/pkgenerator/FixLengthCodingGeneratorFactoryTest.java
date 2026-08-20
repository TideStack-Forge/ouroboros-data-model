package com.ouroboros.data.pkgenerator;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

class FixLengthCodingGeneratorFactoryTest {
  @Test
  void keepsExpressionAndSequenceTemplateBehavior() {
    var factory = new FixLengthCodingGeneratorFactory(new SingleCodingSequencerProvider(new InMemoryCodingSequencer()));

    var generator = factory.get("prefix-${name}-<0001>").orElseThrow();

    assertEquals("prefix-A-0001", generator.next(Map.of("name", "A")));
    assertEquals("prefix-A-0002", generator.peek(Map.of("name", "A")));
  }

  private static class InMemoryCodingSequencer implements CodingSequencer {
    private long current;

    @Override
    public Long next(String name) {
      return next(name, 1L, 1L);
    }

    @Override
    public Long next(String name, Long step) {
      return next(name, step, 1L);
    }

    @Override
    public Long next(String name, Long step, Long start) {
      current = current == 0L ? start : current + step;
      return current;
    }

    @Override
    public Long peek(String name) {
      return peek(name, 1L, 1L);
    }

    @Override
    public Long peek(String name, Long step) {
      return peek(name, step, 1L);
    }

    @Override
    public Long peek(String name, Long step, Long start) {
      return current == 0L ? start : current + step;
    }
  }

  private static class SingleCodingSequencerProvider implements ObjectProvider<CodingSequencer> {
    private final CodingSequencer codingSequencer;

    private SingleCodingSequencerProvider(CodingSequencer codingSequencer) {
      this.codingSequencer = codingSequencer;
    }

    @Override
    public CodingSequencer getObject(Object... args) {
      return codingSequencer;
    }

    @Override
    public CodingSequencer getIfAvailable() {
      return codingSequencer;
    }

    @Override
    public CodingSequencer getIfUnique() {
      return codingSequencer;
    }

    @Override
    public CodingSequencer getObject() {
      return codingSequencer;
    }

    @Override
    public Stream<CodingSequencer> stream() {
      return Stream.of(codingSequencer);
    }

    @Override
    public Stream<CodingSequencer> orderedStream() {
      return Stream.of(codingSequencer);
    }
  }
}
