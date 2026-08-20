package com.ouroboros.data.dsl.query;

/**
 * Query facade source that can render itself to the existing raw FROM language.
 */
public interface QuerySource {

  Object toRawFrom();
}
