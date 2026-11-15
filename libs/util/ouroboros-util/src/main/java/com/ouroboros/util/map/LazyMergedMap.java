package com.ouroboros.util.map;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 综合性的 Lazy Map：
 * - 多 Map 合并（按顺序决定优先级）
 * - 懒加载键（Supplier）
 * - 动态解析键（Function/BiFunction）
 * - 可选 key hints，用于 entrySet/keySet/values 的完整枚举
 * - 可写 overlay，支持 put/remove/clear（不影响底层源）
 */
public final class LazyMergedMap implements Map<String, Object> {

  /**
   * 用于缓存 null 值的哨兵
   */
  private static final Object NULL_SENTINEL = new Object();
  /**
   * 底层普通 sources（按顺序决定优先级）
   */
  private final List<Map<String, Object>> sources;
  /**
   * 懒加载 sources（按顺序决定优先级）
   */
  private final List<Map<String, Supplier<Object>>> lazySources;
  /**
   * 动态解析器（按顺序依次尝试）
   */
  private final List<BiFunction<String, LazyMergedMap, Object>> resolvers;
  /**
   * 候选动态 key 提示（用于 keySet/entrySet/values）
   */
  private final Set<String> dynamicKeyHints;
  /**
   * 合并策略
   */
  private final MergePolicy mergePolicy;
  /**
   * 是否在第一次 materialize 视图时就把懒/动态值一并算出（仅影响 entrySet/values/keySet 的枚举）
   */
  private final boolean materializeOnView;
  /**
   * overlay：对外可写的一层（优先级最高）
   */
  private final Map<String, Object> overlay;
  /**
   * 值缓存（包含 NULL_SENTINEL）
   */
  private final Map<String, Object> cache = new HashMap<>();

  private LazyMergedMap(Builder b) {
    this.sources = Collections.unmodifiableList(new ArrayList<>(b.sources));
    this.lazySources = Collections.unmodifiableList(new ArrayList<>(b.lazySources));
    this.resolvers = Collections.unmodifiableList(new ArrayList<>(b.resolvers));
    this.dynamicKeyHints = Collections.unmodifiableSet(new LinkedHashSet<>(b.dynamicKeyHints));
    this.mergePolicy = b.mergePolicy;
    this.materializeOnView = b.materializeOnView;
    this.overlay = b.mutableOverlay ? new LinkedHashMap<>() : Collections.emptyMap();
  }

  /**
   * 快捷构建：一次性传多个普通 Map（顺序即优先级）
   */
  @SafeVarargs
  public static LazyMergedMap of(Map<String, Object>... maps) {
    return new Builder().addSources(maps).build();
  }

  /**
   * 快捷构建：从 stream/reduce 结果
   */
  public static LazyMergedMap fromReduced(Map<String, Object> reduced) {
    return new Builder().addReducedSource(reduced).build();
  }

  private static Object safeGetSupplier(Supplier<Object> s) {
    return s == null ? null : s.get();
  }

  /**
   * 从流式 reduce 构造：例如 maps.stream().reduce(new HashMap<>(), (a,b)->{a.putAll(b); return a;})
   */
  public static LazyMergedMap fromStreamReduced(Map<String, Object> reduced, MergePolicy policy) {
    return new Builder().addReducedSource(reduced).mergePolicy(policy).build();
  }

  // ------------------------ Map 接口实现 ------------------------

  @Override
  public int size() {
    // 由于存在 resolver，严格意义上 size 不可精确（除非 materialize）。
    // 按照“可见键”的并集估算。
    return keySet().size();
  }

  @Override
  public boolean isEmpty() {
    return keySet().isEmpty();
  }

  @Override
  public boolean containsKey(Object key) {
    if (!(key instanceof String)) {
      return false;
    }
    String k = (String) key;
    if (overlay.containsKey(k)) {
      return true;
    }
    if (cache.containsKey(k)) {
      return true;
    }
    for (Map<String, Object> s : sources) {
      if (s.containsKey(k)) {
        return true;
      }
    }
    for (Map<String, Supplier<Object>> s : lazySources) {
      if (s.containsKey(k)) {
        return true;
      }
    }
    if (dynamicKeyHints.contains(k)) {
      return true;
    }
    // 不在 hints 中，也可能由 resolver 动态产生；这里给“轻量 contains”：
    // 只有在实际 get 时才真正尝试 resolver，以避免“containsKey 调用触发大量计算”。
    return false;
  }

  @Override
  public boolean containsValue(Object value) {
    // 最稳妥的实现方式：遍历 values（会触发 materializeOnView 时的计算）
    return values().contains(value);
  }

  @Override
  public Object get(Object key) {
    if (!(key instanceof String)) {
      return null;
    }
    String k = (String) key;

    // overlay 优先
    if (overlay.containsKey(k)) {
      return overlay.get(k);
    }

    // 缓存
    if (cache.containsKey(k)) {
      Object v = cache.get(k);
      return v == NULL_SENTINEL ? null : v;
    }

    // sources
    switch (mergePolicy) {
      case FIRST_WINS: {
        // 从前到后查找，第一个命中即用
        for (Map<String, Object> s : sources) {
          if (s.containsKey(k)) {
            return cacheAndReturn(k, s.get(k));
          }
        }
        for (Map<String, Supplier<Object>> s : lazySources) {
          if (s.containsKey(k)) {
            return cacheAndReturn(k, safeGetSupplier(s.get(k)));
          }
        }
        break;
      }
      case LAST_WINS: {
        // 从后到前查找，最后一个命中即用
        for (int i = sources.size() - 1; i >= 0; i--) {
          Map<String, Object> s = sources.get(i);
          if (s.containsKey(k)) {
            return cacheAndReturn(k, s.get(k));
          }
        }
        for (int i = lazySources.size() - 1; i >= 0; i--) {
          Map<String, Supplier<Object>> s = lazySources.get(i);
          if (s.containsKey(k)) {
            return cacheAndReturn(k, safeGetSupplier(s.get(k)));
          }
        }
        break;
      }
      case FAIL_ON_CONFLICT: {
        // 收集所有命中值，发现多于一个且值不等则抛异常；等值可认为不冲突。
        List<Object> hits = new ArrayList<>();
        for (Map<String, Object> s : sources) {
          if (s.containsKey(k)) {
            hits.add(s.get(k));
          }
        }
        for (Map<String, Supplier<Object>> s : lazySources) {
          if (s.containsKey(k)) {
            hits.add(safeGetSupplier(s.get(k)));
          }
        }
        if (!hits.isEmpty()) {
          Object chosen = hits.get(0);
          boolean conflict = hits.stream().anyMatch(v -> !Objects.equals(v, chosen));
          if (conflict && hits.size() > 1) {
            throw new IllegalStateException("Key '" + k + "' conflicts under FAIL_ON_CONFLICT policy.");
          }
          return cacheAndReturn(k, chosen);
        }
        break;
      }
    }

    // resolvers
    for (BiFunction<String, LazyMergedMap, Object> r : resolvers) {
      Object v = r.apply(k, this);
      if (v != null) {
        return cacheAndReturn(k, v);
      }
    }

    // 无
    return null;
  }

  private Object cacheAndReturn(String key, Object value) {
    cache.put(key, value == null ? NULL_SENTINEL : value);
    return value;
  }

  private void requireOverlayWritable() {
    if (overlay == Collections.EMPTY_MAP) {
      throw new UnsupportedOperationException("This LazyMergedMap is read-only (no overlay enabled).");
    }
  }

  // ---------- 写操作：作用于 overlay（若 overlay 只读，则抛异常） ----------

  @Override
  public Object put(String key, Object value) {
    requireOverlayWritable();
    cache.remove(key);
    return overlay.put(key, value);
  }

  @Override
  public Object remove(Object key) {
    requireOverlayWritable();
    cache.remove(key);
    return overlay.remove(key);
  }

  @Override
  public void putAll(Map<? extends String, ?> m) {
    requireOverlayWritable();
    if (m == null || m.isEmpty()) {
      return;
    }
    m.forEach((k, v) -> {
      cache.remove(k);
      overlay.put(k, v);
    });
  }

  @Override
  public void clear() {
    requireOverlayWritable();
    overlay.clear();
    cache.clear();
  }

  @Override
  public Set<String> keySet() {
    Set<String> keys = new LinkedHashSet<>();

    // overlay 优先可见
    keys.addAll(overlay.keySet());

    // 普通 sources
    for (Map<String, Object> s : sources) {
      keys.addAll(s.keySet());
    }

    // 懒源 keys（不执行 Supplier）
    for (Map<String, Supplier<Object>> s : lazySources) {
      keys.addAll(s.keySet());
    }

    // 动态 key 提示
    keys.addAll(dynamicKeyHints);

    return Collections.unmodifiableSet(keys);
  }

  // ---------- 视图：keySet / values / entrySet ----------

  @Override
  public Collection<Object> values() {
    if (!materializeOnView) {
      // 非 materialize 模式：只对 keySet 中的键做 get；动态但无 hint 的键不会出现在这里
      return Collections.unmodifiableList(
          keySet().stream().map(this::get).collect(Collectors.toList())
      );
    }
    // materialize：把可见键都算出来（含懒/动态值）
    return Collections.unmodifiableCollection(materialize().values());
  }

  @Override
  public Set<Entry<String, Object>> entrySet() {
    if (!materializeOnView) {
      // 非 materialize 模式：针对 keySet 做“惰性 get -> Entry”
      Set<Entry<String, Object>> set = new LinkedHashSet<>();
      for (String k : keySet()) {
        set.add(new AbstractMap.SimpleEntry<>(k, get(k)));
      }
      return Collections.unmodifiableSet(set);
    }
    // materialize
    return Collections.unmodifiableSet(materialize()
        .entrySet()
        .stream()
        .map(e -> new AbstractMap.SimpleEntry<>(e.getKey(), e.getValue()))
        .collect(Collectors.toSet()));
  }

  /**
   * 将当前 Map 完全物化为一个普通 HashMap（会执行所有懒值/动态值（在 key hints 范围内））
   */
  public Map<String, Object> materialize() {
    Map<String, Object> out = new LinkedHashMap<>();

    // 先按策略合并 sources 与 lazySources
    // （注：与 get 不同的是，这里需要所有键的最终值）
    LinkedHashSet<String> allKeys = new LinkedHashSet<>(keySet());
    // 合并顺序控制：我们先收集候选值列表再按策略决策
    for (String k : allKeys) {
      // overlay 最高
      if (overlay.containsKey(k)) {
        out.put(k, overlay.get(k));
        continue;
      }

      List<Object> hits = new ArrayList<>();
      for (Map<String, Object> s : sources) {
        if (s.containsKey(k)) {
          hits.add(s.get(k));
        }
      }
      for (Map<String, Supplier<Object>> s : lazySources) {
        if (s.containsKey(k)) {
          hits.add(safeGetSupplier(s.get(k)));
        }
      }

      Object chosen = null;
      switch (mergePolicy) {
        case FIRST_WINS:
          if (!hits.isEmpty()) {
            chosen = hits.get(0);
          }
          break;
        case LAST_WINS:
          if (!hits.isEmpty()) {
            chosen = hits.get(hits.size() - 1);
          }
          break;
        case FAIL_ON_CONFLICT:
          if (!hits.isEmpty()) {
            Object c = hits.get(0);
            boolean conflict = hits.stream().anyMatch(v -> !Objects.equals(v, c));
            if (conflict && hits.size() > 1) {
              throw new IllegalStateException("Key '" + k + "' conflicts under FAIL_ON_CONFLICT policy.");
            }
            chosen = c;
          }
          break;
      }

      if (chosen == null) {
        // 依次尝试 resolvers
        for (BiFunction<String, LazyMergedMap, Object> r : resolvers) {
          Object v = r.apply(k, this);
          if (v != null) {
            chosen = v;
            break;
          }
        }
      }
      if (chosen != null) {
        out.put(k, chosen);
      }
    }
    return out;
  }

  // ---------- 其他实用方法 ----------

  /**
   * 获取内部可写 overlay 的只读视图
   */
  public Map<String, Object> overlayView() {
    return Collections.unmodifiableMap(overlay);
  }

  /**
   * 清除内部缓存（不影响 overlay 与底层 sources）
   */
  public void clearCache() {
    cache.clear();
  }

  /**
   * 合并策略：多个数据源 key 冲突时如何决策
   */
  public enum MergePolicy {
    /**
     * 先来的优先（sources/懒源/overlay 的顺序）
     */
    FIRST_WINS,
    /**
     * 后来的优先（sources/懒源/overlay 的逆序）
     */
    LAST_WINS,
    /**
     * 发现冲突即抛出异常
     */
    FAIL_ON_CONFLICT
  }

  // ------------------------ Builder ------------------------
  public static final class Builder {
    private final List<Map<String, Object>> sources = new ArrayList<>();
    private final List<Map<String, Supplier<Object>>> lazySources = new ArrayList<>();
    private final List<BiFunction<String, LazyMergedMap, Object>> resolvers = new ArrayList<>();
    private final Set<String> dynamicKeyHints = new LinkedHashSet<>();
    // NEW: 单条目懒值临时桶，build() 时一次性并入 lazySources
    private final Map<String, Supplier<Object>> lazyEntryBucket = new LinkedHashMap<>();
    private MergePolicy mergePolicy = MergePolicy.FIRST_WINS;
    private boolean materializeOnView = true;
    private boolean mutableOverlay = true;

    /**
     * 添加普通数据源（按添加顺序决定优先级）
     */
    public Builder addSource(Map<String, Object> map) {
      if (map != null) {
        sources.add(map);
      }
      return this;
    }

    @SafeVarargs
    public final Builder addSources(Map<String, Object>... maps) {
      if (maps != null) {
        Arrays.stream(maps).filter(Objects::nonNull).forEach(sources::add);
      }
      return this;
    }

    public Builder addReducedSource(Map<String, Object> reduced) {
      if (reduced != null) {
        sources.add(reduced);
      }
      return this;
    }

    /**
     * 添加懒加载数据源
     */
    public Builder addLazySource(Map<String, Supplier<Object>> lazyMap) {
      if (lazyMap != null) {
        lazySources.add(lazyMap);
      }
      return this;
    }

    /**
     * 添加一个简单 resolver（Function 版本）
     */
    public Builder addResolver(Function<String, Object> resolver) {
      if (resolver != null) {
        resolvers.add((k, m) -> resolver.apply(k));
      }
      return this;
    }

    /**
     * 添加一个增强 resolver（BiFunction 版本，可基于当前 Map 上下文计算）
     */
    public Builder addResolver(BiFunction<String, LazyMergedMap, Object> resolver) {
      if (resolver != null) {
        resolvers.add(resolver);
      }
      return this;
    }

    /**
     * 添加动态 key 提示（便于 keySet/entrySet 完整枚举）
     */
    public Builder addDynamicKeyHint(String key) {
      if (key != null && !key.isEmpty()) {
        dynamicKeyHints.add(key);
      }
      return this;
    }

    public Builder addDynamicKeyHints(Collection<String> keys) {
      if (keys != null) {
        keys.stream().filter(k -> k != null && !k.isEmpty()).forEach(dynamicKeyHints::add);
      }
      return this;
    }

    public Builder mergePolicy(MergePolicy policy) {
      this.mergePolicy = policy == null ? MergePolicy.FIRST_WINS : policy;
      return this;
    }

    public Builder materializeOnView(boolean enabled) {
      this.materializeOnView = enabled;
      return this;
    }

    public Builder mutableOverlay(boolean enabled) {
      this.mutableOverlay = enabled;
      return this;
    }

    // ================== 新增的两个便捷方法 ==================

    /**
     * 为指定 key 添加一个懒值（Supplier 版本）。
     * 注意：build() 时会把所有通过该方法添加的条目合并为一个 lazy source。
     */
    public Builder addLazyEntry(String key, Supplier<Object> supplier) {
      Objects.requireNonNull(key, "key");
      Objects.requireNonNull(supplier, "supplier");
      lazyEntryBucket.put(key, supplier);
      // 让 key 在枚举视图中可见（可选）
      dynamicKeyHints.add(key);
      return this;
    }

    /**
     * 为指定 key 添加一个动态计算（Function 版本，入参为 LazyMergedMap 上下文）。
     * 仅当访问的 key 与绑定的 key 相同时才会触发该计算。
     */
    public Builder addLazyEntry(String key, Function<LazyMergedMap, Object> computer) {
      Objects.requireNonNull(key, "key");
      Objects.requireNonNull(computer, "computer");
      resolvers.add((k, map) -> key.equals(k) ? computer.apply(map) : null);
      // 让 key 在枚举视图中可见（可选）
      dynamicKeyHints.add(key);
      return this;
    }

    /**
     * 为指定 key 添加一个动态计算（BiFunction 版本，入参为 (key, ctx)）。
     * 更灵活：可以拿到绑定的 key 和上下文。
     */
    public Builder addLazyEntry(String key, BiFunction<String, LazyMergedMap, Object> computer) {
      Objects.requireNonNull(key, "key");
      Objects.requireNonNull(computer, "computer");
      resolvers.add((k, map) -> key.equals(k) ? computer.apply(k, map) : null);
      dynamicKeyHints.add(key);
      return this;
    }

    public LazyMergedMap build() {
      // 把通过 addLazyEntry(Supplier) 积累的条目收拢为一个 lazy source
      if (!lazyEntryBucket.isEmpty()) {
        lazySources.add(new LinkedHashMap<>(lazyEntryBucket));
        lazyEntryBucket.clear();
      }
      return new LazyMergedMap(this);
    }
  }
}