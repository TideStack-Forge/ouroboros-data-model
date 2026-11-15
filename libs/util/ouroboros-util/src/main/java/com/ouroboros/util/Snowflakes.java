package com.ouroboros.util;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class Snowflakes {
  private static final long TWEPOCH = 1288834974657L;
  private static final long WORKER_ID_BITS = 5L;
  private static final long DATACENTER_ID_BITS = 5L;
  private static final long SEQUENCE_BITS = 12L;
  private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);
  private static final long MAX_DATACENTER_ID = ~(-1L << DATACENTER_ID_BITS);
  private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;
  private static final long DATACENTER_ID_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;
  private static final long TIMESTAMP_LEFT_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS + DATACENTER_ID_BITS;
  private static final long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS);
  private static final long MAX_CLOCK_BACKWARD_MS = 2000L;
  private static final Map<Node, Snowflake> SNOWFLAKES = new ConcurrentHashMap<>();

  private Snowflakes() {
  }

  public static Snowflake get(long workerId, long datacenterId) {
    return SNOWFLAKES.computeIfAbsent(new Node(workerId, datacenterId),
        node -> new Snowflake(node.workerId, node.datacenterId));
  }

  public static long nextId(long workerId, long datacenterId) {
    return get(workerId, datacenterId).nextId();
  }

  private static class Node {
    private final long workerId;
    private final long datacenterId;

    private Node(long workerId, long datacenterId) {
      this.workerId = workerId;
      this.datacenterId = datacenterId;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof Node that)) {
        return false;
      }
      return workerId == that.workerId && datacenterId == that.datacenterId;
    }

    @Override
    public int hashCode() {
      return Objects.hash(workerId, datacenterId);
    }
  }

  public static class Snowflake {
    private final long workerId;
    private final long datacenterId;
    private long sequence = 0L;
    private long lastTimestamp = -1L;

    private Snowflake(long workerId, long datacenterId) {
      if (workerId > MAX_WORKER_ID || workerId < 0) {
        throw new IllegalArgumentException("workerId must be between 0 and " + MAX_WORKER_ID);
      }
      if (datacenterId > MAX_DATACENTER_ID || datacenterId < 0) {
        throw new IllegalArgumentException("datacenterId must be between 0 and " + MAX_DATACENTER_ID);
      }
      this.workerId = workerId;
      this.datacenterId = datacenterId;
    }

    public synchronized long nextId() {
      long timestamp = currentTime();
      if (timestamp < lastTimestamp) {
        long offset = lastTimestamp - timestamp;
        if (offset > MAX_CLOCK_BACKWARD_MS) {
          throw new IllegalStateException("Clock moved backwards. Refusing to generate id for " + offset + " milliseconds");
        }
        timestamp = waitUntil(lastTimestamp);
      }
      if (timestamp == lastTimestamp) {
        sequence = (sequence + 1) & SEQUENCE_MASK;
        if (sequence == 0) {
          timestamp = waitNextMillis(lastTimestamp);
        }
      } else {
        sequence = 0L;
      }

      lastTimestamp = timestamp;
      return ((timestamp - TWEPOCH) << TIMESTAMP_LEFT_SHIFT)
          | (datacenterId << DATACENTER_ID_SHIFT)
          | (workerId << WORKER_ID_SHIFT)
          | sequence;
    }

    private long waitNextMillis(long lastTimestamp) {
      long timestamp = currentTime();
      while (timestamp <= lastTimestamp) {
        timestamp = currentTime();
      }
      return timestamp;
    }

    private long waitUntil(long lastTimestamp) {
      long timestamp = currentTime();
      while (timestamp < lastTimestamp) {
        long waitMillis = Math.max(1L, lastTimestamp - timestamp);
        try {
          Thread.sleep(waitMillis);
        }
        catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          throw new IllegalStateException("Interrupted while waiting for clock to recover", e);
        }
        timestamp = currentTime();
      }
      return timestamp;
    }

    private long currentTime() {
      return System.currentTimeMillis();
    }
  }
}
