package com.ouroboros.data.pkgenerator;

@SuppressWarnings("unused")
public interface CodingSequencer {
  Long next(String name);

  Long next(String name, Long step);

  Long next(String name, Long step, Long start);

  Long peek(String name);

  Long peek(String name, Long step);

  Long peek(String name, Long step, Long start);
}
