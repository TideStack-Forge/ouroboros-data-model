package com.ouroboros.data.dsl;

import java.io.Serializable;

public enum JoinType implements Serializable {
  INNERJOIN,
  LEFTJOIN,
  RIGHTJOIN,
  FULLJOIN,
  DEFAULT
}
