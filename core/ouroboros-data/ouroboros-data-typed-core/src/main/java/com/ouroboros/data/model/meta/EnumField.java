package com.ouroboros.data.model.meta;

import java.util.Objects;

/**
 * Typed enum field path.
 *
 * @param <E>     enum value type
 * @param <OWNER> owner meta type
 */
public final class EnumField<E extends Enum<E>, OWNER extends TypedModelMeta<?, ?>>
    extends TypedField<E, OWNER> {
  private final Class<E> enumType;

  EnumField(TypedModelMeta<?, ?> owner, String fieldName, Class<E> enumType) {
    super(owner, fieldName);
    this.enumType = Objects.requireNonNull(enumType, "enumType must not be null");
  }

  public Class<E> getEnumType() {
    return enumType;
  }
}
