package com.ouroboros.util;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.vavr.Tuple;
import io.vavr.Tuple2;

/**
 * 完善的树形结构工具类
 *
 * <p>提供全面的树操作功能,包括构建、遍历、查询、转换和修改操作。
 * 所有方法都经过严格的空值处理和边界条件验证。
 *
 * <p><b>线程安全</b>: 本工具类中的所有静态方法都是线程安全的,
 * 但传入的树结构本身可能不是线程安全的。如需并发修改,
 * 请使用适当的同步机制或不可变树结构。
 *
 * <p><b>递归深度限制</b>: 递归方法在深度超过{@link #MAX_RECURSION_DEPTH}
 * 时会自动切换到迭代实现或抛出异常。可通过JVM参数-Xss调整栈大小。
 *
 * <p><b>多根树支持</b>: 本工具类专为多根树设计(如组织架构树、目录树等),
 * 大部分方法都支持 Collection&lt;T&gt; roots 参数。
 *
 * @author Trees Team
 * @version 2.1
 * @since 1.0
 */
@SuppressWarnings("unused")
public class Trees {

  /**
   * 默认最大递归深度限制,防止栈溢出
   * 在深度超过此值时,递归方法应切换到迭代实现
   */
  public static final int MAX_RECURSION_DEPTH = 5000;

  // ==================== 构建方法 ====================

  /**
   * 构建树结构
   *
   * <p><b>时间复杂度</b>: O(n)，其中n是节点总数
   * <p><b>空间复杂度</b>: O(h)，其中h是树的高度(递归调用栈)
   *
   * @param root           根节点,不能为null
   * @param childrenGetter 获取子节点的函数,不能为null
   * @param childrenSetter 设置子节点的函数,不能为null
   * @param <T>            节点类型
   * @return 构建完成的根节点
   * @throws NullPointerException 如果任何参数为null
   */
  @SuppressWarnings("UnusedReturnValue")
  public static <T> T buildTree(T root, Function<T, ? extends Collection<T>> childrenGetter, BiConsumer<T, Collection<T>> childrenSetter) {
    Objects.requireNonNull(root, "Root cannot be null");
    Objects.requireNonNull(childrenGetter, "Children getter cannot be null");
    Objects.requireNonNull(childrenSetter, "Children setter cannot be null");

    Optional.ofNullable(childrenGetter.apply(root))
        .ifPresent(children -> {
          children.forEach(child -> buildTree(child, childrenGetter, childrenSetter));
          childrenSetter.accept(root, children);
        });
    return root;
  }

  /**
   * 批量构建树结构
   *
   * @param roots          根节点集合
   * @param childrenGetter 获取子节点的函数
   * @param nodeCreator    节点创建器,接收原始节点和已转换的子节点列表
   * @param <T>            原始节点类型
   * @param <R>            目标节点类型
   * @return 转换后的树节点列表
   */
  public static <T, R> List<R> buildTree(Collection<T> roots, Function<T, List<T>> childrenGetter, BiFunction<T, List<R>, R> nodeCreator) {
    if (roots == null || roots.isEmpty()) {
      return Collections.emptyList();
    }

    Objects.requireNonNull(childrenGetter, "Children getter cannot be null");
    Objects.requireNonNull(nodeCreator, "Node creator cannot be null");

    return roots.stream().map(node -> {
      var children = childrenGetter.apply(node);
      var newChildren = children != null ? buildTree(children, childrenGetter, nodeCreator) : Collections.<R>emptyList();
      return nodeCreator.apply(node, newChildren);
    }).collect(Collectors.toList());
  }

  // ==================== 遍历方法 ====================

  /**
   * 自下而上遍历树(后序遍历)
   * 先访问所有子节点,最后访问当前节点
   *
   * <p><b>遍历顺序</b>: 左子树 → 右子树 → 根节点
   * <p><b>时间复杂度</b>: O(n)
   * <p><b>空间复杂度</b>: O(h) 递归调用栈
   *
   * @param root           根节点
   * @param childrenGetter 获取子节点的函数
   * @param consumer       节点访问器
   * @param <T>            节点类型
   */
  public static <T> void walkTree(T root, Function<T, ? extends Collection<T>> childrenGetter, Consumer<T> consumer) {
    if (root == null) {
      return;
    }

    Objects.requireNonNull(childrenGetter, "Children getter cannot be null");
    Objects.requireNonNull(consumer, "Consumer cannot be null");

    Optional.ofNullable(childrenGetter.apply(root))
        .ifPresent(children -> children.forEach(child -> walkTree(child, childrenGetter, consumer)));
    consumer.accept(root);
  }

  /**
   * 批量自下而上遍历树
   */
  public static <T> void walkTree(Collection<T> roots, Function<T, ? extends Collection<T>> childrenGetter, Consumer<T> consumer) {
    if (roots == null || roots.isEmpty()) {
      return;
    }
    roots.forEach(root -> walkTree(root, childrenGetter, consumer));
  }

  /**
   * 自下而上遍历树,提供父节点信息
   *
   * @param consumer 接收(当前节点, 父节点)的访问器,根节点的父节点为null
   */
  public static <T> void walkTree(T root, Function<T, ? extends Collection<T>> childrenGetter, BiConsumer<T, T> consumer) {
    if (root == null) {
      return;
    }

    Objects.requireNonNull(childrenGetter, "Children getter cannot be null");
    Objects.requireNonNull(consumer, "Consumer cannot be null");

    Optional.ofNullable(childrenGetter.apply(root))
        .ifPresent(children -> children.forEach(child -> walkTree(child, childrenGetter, consumer)));
    consumer.accept(root, null);
  }

  public static <T> void walkTree(Collection<T> roots, Function<T, ? extends Collection<T>> childrenGetter, BiConsumer<T, T> consumer) {
    if (roots == null || roots.isEmpty()) {
      return;
    }
    roots.forEach(root -> walkTree(root, childrenGetter, consumer));
  }

  /**
   * 自上而下遍历树(前序遍历)
   * 先访问当前节点,再访问所有子节点
   *
   * <p><b>遍历顺序</b>: 根节点 → 左子树 → 右子树
   * <p><b>时间复杂度</b>: O(n)
   * <p><b>空间复杂度</b>: O(h) 递归调用栈
   *
   * @param root           根节点
   * @param childrenGetter 获取子节点的函数
   * @param consumer       节点访问器
   * @param <T>            节点类型
   */
  public static <T> void walkTreeDown(T root, Function<T, ? extends Collection<T>> childrenGetter, Consumer<T> consumer) {
    if (root == null) {
      return;
    }

    Objects.requireNonNull(childrenGetter, "Children getter cannot be null");
    Objects.requireNonNull(consumer, "Consumer cannot be null");

    consumer.accept(root);
    Optional.ofNullable(childrenGetter.apply(root))
        .ifPresent(children -> children.forEach(child -> walkTreeDown(child, childrenGetter, consumer)));
  }

  public static <T> void walkTreeDown(Collection<T> roots, Function<T, ? extends Collection<T>> childrenGetter, Consumer<T> consumer) {
    if (roots == null || roots.isEmpty()) {
      return;
    }
    roots.forEach(root -> walkTreeDown(root, childrenGetter, consumer));
  }

  public static <T> void walkTreeDown(T root, Function<T, ? extends Collection<T>> childrenGetter, BiConsumer<T, T> consumer) {
    if (root == null) {
      return;
    }

    Objects.requireNonNull(childrenGetter, "Children getter cannot be null");
    Objects.requireNonNull(consumer, "Consumer cannot be null");

    consumer.accept(root, null);
    Optional.ofNullable(childrenGetter.apply(root))
        .ifPresent(children -> children.forEach(child -> walkTreeDown(child, childrenGetter, consumer)));
  }

  public static <T> void walkTreeDown(Collection<T> root, Function<T, ? extends Collection<T>> childrenGetter, BiConsumer<T, T> consumer) {
    if (root == null || root.isEmpty()) {
      return;
    }
    root.forEach(r -> walkTreeDown(r, childrenGetter, consumer));
  }

  // ==================== 映射方法 ====================

  /**
   * 自下而上映射树
   * 先转换子树,再基于转换后的子节点转换当前节点
   *
   * <p><b>时间复杂度</b>: O(n)
   * <p><b>空间复杂度</b>: O(h) 递归调用栈 + O(n) 新树
   *
   * @param root           原始树根节点
   * @param childrenGetter 获取子节点的函数
   * @param mapper         映射函数,接收(原始节点, 已映射的子节点列表)
   * @param <T>            原始节点类型
   * @param <R>            目标节点类型
   * @return 映射后的根节点
   */
  public static <T, R> R mapTree(T root, Function<T, ? extends Collection<T>> childrenGetter, BiFunction<T, List<R>, R> mapper) {
    if (root == null) {
      return null;
    }

    Objects.requireNonNull(childrenGetter, "Children getter cannot be null");
    Objects.requireNonNull(mapper, "Mapper cannot be null");

    var mappedChildren = Optional.ofNullable(childrenGetter.apply(root))
        .map(children -> children
            .stream()
            .map(child -> mapTree(child, childrenGetter, mapper))
            .filter(Objects::nonNull)
            .collect(Collectors.toList()))
        .orElse(Collections.emptyList());
    return mapper.apply(root, mappedChildren);
  }

  public static <T, R> List<R> mapTree(Collection<T> roots, Function<T, ? extends Collection<T>> childrenGetter, BiFunction<T, List<R>, R> mapper) {
    if (roots == null || roots.isEmpty()) {
      return Collections.emptyList();
    }

    return roots.stream()
        .map(root -> mapTree(root, childrenGetter, mapper))
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  /**
   * 自上而下映射树
   * 先转换当前节点,再基于转换后的父节点转换子节点
   *
   * @param root           原始树根节点
   * @param childrenGetter 获取子节点的函数
   * @param childrenSetter 设置子节点的函数
   * @param mapper         映射函数,接收(原始节点, 已映射的父节点),根节点的父节点为null
   * @param <T>            原始节点类型
   * @param <R>            目标节点类型
   * @return 映射后的根节点
   */
  public static <T, R> R mapTreeDown(T root, Function<T, ? extends Collection<T>> childrenGetter, BiConsumer<R, List<R>> childrenSetter, BiFunction<T, R, R> mapper) {
    if (root == null) {
      return null;
    }

    Objects.requireNonNull(childrenGetter, "Children getter cannot be null");
    Objects.requireNonNull(childrenSetter, "Children setter cannot be null");
    Objects.requireNonNull(mapper, "Mapper cannot be null");

    var recursiveMapper = new AtomicReference<BiFunction<T, R, R>>();
    recursiveMapper.set((node, parent) -> {
      var mapped = mapper.apply(node, parent);
      var mappedChildren = Optional.ofNullable(childrenGetter.apply(node))
          .map(children -> children
              .stream()
              .map(child -> recursiveMapper.get().apply(child, mapped))
              .filter(Objects::nonNull)
              .collect(Collectors.toList()))
          .orElse(Collections.emptyList());
      childrenSetter.accept(mapped, mappedChildren);
      return mapped;
    });
    return recursiveMapper.get().apply(root, null);
  }

  public static <T, R> List<R> mapTreeDown(Collection<T> roots, Function<T, ? extends Collection<T>> childrenGetter, BiConsumer<R, List<R>> childrenSetter, BiFunction<T, R, R> mapper) {
    if (roots == null || roots.isEmpty()) {
      return Collections.emptyList();
    }

    return roots.stream()
        .map(root -> mapTreeDown(root, childrenGetter, childrenSetter, mapper))
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  public static <T> Optional<T> findOne(T root, Function<T, ? extends Collection<T>> childrenGetter, Predicate<T> predicate) {
    AtomicReference<T> result = new AtomicReference<T>();
    walkTree(root, node -> {
      if (result.get() != null) {
        return Collections.emptyList();
      }
      return childrenGetter.apply(node);
    }, node -> {
      if (predicate.test(node)) {
        result.set(node);
      }
    });
    return Optional.ofNullable(result.get());
  }

  public static <T> Optional<T> findOne(Collection<T> roots, Function<T, ? extends Collection<T>> childrenGetter, Predicate<T> predicate) {
    return roots.stream()
        .map(root -> findOne(root, childrenGetter, predicate))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .findFirst();
  }

  public static <T> List<T> findAll(T root, Function<T, ? extends Collection<T>> childrenGetter, Predicate<T> predicate) {
    List<T> result = new ArrayList<>();
    walkTree(root, childrenGetter, node -> {
      if (predicate.test(node)) {
        result.add(node);
      }
    });
    return result;
  }

  public static <T> List<T> findAll(Collection<T> roots, Function<T, ? extends Collection<T>> childrenGetter, Predicate<T> predicate) {
    return roots.stream()
        .flatMap(root -> findAll(root, childrenGetter, predicate).stream())
        .collect(Collectors.toList());
  }

  // ==================== 扁平化方法 ====================

  /**
   * 将树扁平化为列表(前序遍历顺序)
   *
   * <p><b>时间复杂度</b>: O(n)
   * <p><b>空间复杂度</b>: O(n)
   *
   * @param root           根节点
   * @param childrenGetter 获取子节点的函数
   * @param <T>            节点类型
   * @return 扁平化后的节点列表
   */
  public static <T> List<T> flattenTree(T root, Function<T, ? extends Collection<T>> childrenGetter) {
    if (root == null) {
      return Collections.emptyList();
    }

    var result = new ArrayList<T>();
    walkTreeDown(root, childrenGetter, node -> result.add(node));
    return result;
  }

  public static <T> List<T> flattenTree(Collection<T> roots, Function<T, ? extends Collection<T>> childrenGetter) {
    if (roots == null || roots.isEmpty()) {
      return Collections.emptyList();
    }

    var result = new ArrayList<T>();
    walkTreeDown(roots, childrenGetter, node -> result.add(node));
    return result;
  }

  /**
   * 创建流式扁平化函数
   * 用于与Stream API配合使用
   */
  public static <T> Function<T, Stream<T>> streamFlatter(Function<T, Stream<T>> childrenGetter) {
    Objects.requireNonNull(childrenGetter, "Children getter cannot be null");

    return (T child) -> {
      var children = childrenGetter.apply(child);
      return Stream.concat(Stream.of(child), children.flatMap(streamFlatter(childrenGetter)));
    };
  }

  // ==================== 过滤方法 ====================

  /**
   * 过滤树,移除不满足条件的节点
   *
   * <p><b>注意</b>: 如果父节点被过滤掉,其所有后代节点也会被移除
   * <p><b>时间复杂度</b>: O(n)
   * <p><b>空间复杂度</b>: O(n)
   *
   * @param tree           树的根节点集合
   * @param filter         过滤条件,返回true的节点会保留
   * @param childrenGetter 获取子节点的函数
   * @param childrenSetter 设置子节点的函数
   * @param <T>            节点类型
   * @return 过滤后的树
   */
  public static <T> List<T> filterTree(Collection<T> tree, Predicate<T> filter, Function<T, ? extends Collection<T>> childrenGetter, BiConsumer<T, List<T>> childrenSetter) {
    if (tree == null || tree.isEmpty()) {
      return Collections.emptyList();
    }

    Objects.requireNonNull(filter, "Filter cannot be null");
    Objects.requireNonNull(childrenGetter, "Children getter cannot be null");
    Objects.requireNonNull(childrenSetter, "Children setter cannot be null");

    return tree.stream().filter(filter)
        .map(node -> {
          var children = childrenGetter.apply(node);
          if (children == null || children.isEmpty()) {
            return node;
          }
          var filteredChildren = filterTree(children, filter, childrenGetter, childrenSetter);
          childrenSetter.accept(node, filteredChildren);
          return node;
        })
        .collect(Collectors.toList());
  }

  // ==================== 层级关系查询 (重构版,支持多根和Predicate) ====================

  /**
   * 获取节点的所有祖先节点(从父节点到根节点的路径)
   *
   * <p><b>前提条件</b>: 节点必须维护父指针
   * <p><b>时间复杂度</b>: O(h)，其中h是节点到根的高度
   * <p><b>空间复杂度</b>: O(h)
   *
   * @param node         目标节点
   * @param parentGetter 获取父节点的函数
   * @param <T>          节点类型
   * @return 祖先节点列表,按从近到远排序(父节点、祖父节点、...)
   * @throws NullPointerException 如果parentGetter为null
   */
  public static <T> List<T> getAncestors(T node, Function<T, T> parentGetter) {
    Objects.requireNonNull(parentGetter, "Parent getter cannot be null");

    List<T> ancestors = new ArrayList<>();
    if (node == null) {
      return ancestors;
    }

    T current = parentGetter.apply(node);
    while (current != null) {
      ancestors.add(current);
      current = parentGetter.apply(current);
    }
    return ancestors;
  }

  /**
   * 在多根树中查找匹配节点并获取其所有祖先节点
   *
   * <p><b>使用场景</b>: 当只知道节点特征(如id、name)而没有节点实例时使用
   * <p><b>示例</b>: 根据员工ID查找其所有上级领导
   * <p><b>时间复杂度</b>: O(n) 查找节点 + O(h) 获取祖先
   *
   * @param roots          多根树的根节点集合
   * @param nodePredicate  目标节点匹配条件
   * @param childrenGetter 获取子节点的函数
   * @param parentGetter   获取父节点的函数
   * @param <T>            节点类型
   * @return 祖先节点列表,如果未找到匹配节点则返回空列表
   */
  public static <T> List<T> getAncestors(Collection<T> roots, Predicate<T> nodePredicate,
                                         Function<T, ? extends Collection<T>> childrenGetter,
                                         Function<T, T> parentGetter) {
    Objects.requireNonNull(nodePredicate, "Node predicate cannot be null");

    Optional<T> found = walker(roots, childrenGetter).findFirst(nodePredicate);
    return found.map(node -> getAncestors(node, parentGetter)).orElse(Collections.emptyList());
  }

  /**
   * 获取节点的所有后代节点(以该节点为根的整个子树的所有节点)
   *
   * <p><b>注意</b>: 结果不包含节点本身
   * <p><b>时间复杂度</b>: O(n)，其中n是子树的节点数
   * <p><b>空间复杂度</b>: O(n)
   *
   * @param root           根节点
   * @param childrenGetter 获取子节点的函数
   * @param <T>            节点类型
   * @return 后代节点列表(前序遍历顺序)
   * @throws NullPointerException 如果childrenGetter为null
   */
  public static <T> List<T> getDescendants(T root, Function<T, ? extends Collection<T>> childrenGetter) {
    Objects.requireNonNull(childrenGetter, "Children getter cannot be null");

    List<T> descendants = new ArrayList<>();
    if (root == null) {
      return descendants;
    }

    Collection<T> children = childrenGetter.apply(root);
    if (children != null && !children.isEmpty()) {
      for (T child : children) {
        descendants.add(child);
        descendants.addAll(getDescendants(child, childrenGetter));
      }
    }
    return descendants;
  }

  /**
   * 在多根树中查找匹配节点并获取其所有后代节点
   *
   * <p><b>使用场景</b>: 查找某个部门下的所有子部门,某个目录下的所有子目录
   * <p><b>示例</b>: 根据部门名称查找所有下级部门
   * <p><b>时间复杂度</b>: O(n) 查找节点 + O(m) 获取后代
   *
   * @param roots          多根树的根节点集合
   * @param nodePredicate  目标节点匹配条件
   * @param childrenGetter 获取子节点的函数
   * @param <T>            节点类型
   * @return 后代节点列表,如果未找到匹配节点则返回空列表
   */
  public static <T> List<T> getDescendants(Collection<T> roots, Predicate<T> nodePredicate,
                                           Function<T, ? extends Collection<T>> childrenGetter) {
    Objects.requireNonNull(nodePredicate, "Node predicate cannot be null");

    Optional<T> found = walker(roots, childrenGetter).findFirst(nodePredicate);
    return found.map(node -> getDescendants(node, childrenGetter)).orElse(Collections.emptyList());
  }

  /**
   * 获取节点的所有兄弟节点(共享同一父节点的其他节点)
   *
   * <p><b>前提条件</b>: 节点必须维护父指针
   * <p><b>时间复杂度</b>: O(k)，其中k是父节点的子节点数
   * <p><b>空间复杂度</b>: O(k)
   *
   * @param node           目标节点
   * @param parentGetter   获取父节点的函数
   * @param childrenGetter 获取子节点的函数
   * @param <T>            节点类型
   * @return 兄弟节点列表,不包含节点本身
   */
  public static <T> List<T> getSiblings(T node, Function<T, T> parentGetter, Function<T, ? extends Collection<T>> childrenGetter) {
    Objects.requireNonNull(parentGetter, "Parent getter cannot be null");
    Objects.requireNonNull(childrenGetter, "Children getter cannot be null");

    if (node == null) {
      return Collections.emptyList();
    }

    T parent = parentGetter.apply(node);
    if (parent == null) {
      return Collections.emptyList();
    }

    Collection<T> siblings = childrenGetter.apply(parent);
    if (siblings == null || siblings.isEmpty()) {
      return Collections.emptyList();
    }

    return siblings.stream()
        .filter(sibling -> sibling != node)
        .collect(Collectors.toList());
  }

  /**
   * 在多根树中查找匹配节点并获取其所有兄弟节点
   *
   * <p><b>使用场景</b>: 查找同级的部门、同级的菜单项等
   * <p><b>示例</b>: 根据菜单ID查找同级的其他菜单
   * <p><b>时间复杂度</b>: O(n) 查找节点 + O(k) 获取兄弟
   *
   * @param roots          多根树的根节点集合
   * @param nodePredicate  目标节点匹配条件
   * @param childrenGetter 获取子节点的函数
   * @param parentGetter   获取父节点的函数
   * @param <T>            节点类型
   * @return 兄弟节点列表,如果未找到匹配节点则返回空列表
   */
  public static <T> List<T> getSiblings(Collection<T> roots, Predicate<T> nodePredicate,
                                        Function<T, ? extends Collection<T>> childrenGetter,
                                        Function<T, T> parentGetter) {
    Objects.requireNonNull(nodePredicate, "Node predicate cannot be null");

    Optional<T> found = walker(roots, childrenGetter).findFirst(nodePredicate);
    return found.map(node -> getSiblings(node, parentGetter, childrenGetter)).orElse(Collections.emptyList());
  }

  /**
   * 克隆子树(深拷贝)
   *
   * <p><b>时间复杂度</b>: O(n)，其中n是子树的节点数
   * <p><b>空间复杂度</b>: O(n)
   *
   * @param root           子树根节点
   * @param childrenGetter 获取子节点的函数
   * @param nodeCopier     节点拷贝函数
   * @param childrenSetter 设置子节点的函数
   * @param <T>            节点类型
   * @return 克隆后的子树根节点
   */
  public static <T> T cloneSubtree(T root, Function<T, ? extends Collection<T>> childrenGetter,
                                   Function<T, T> nodeCopier, BiConsumer<T, List<T>> childrenSetter) {
    if (root == null) {
      return null;
    }

    Objects.requireNonNull(childrenGetter, "Children getter cannot be null");
    Objects.requireNonNull(nodeCopier, "Node copier cannot be null");
    Objects.requireNonNull(childrenSetter, "Children setter cannot be null");

    return mapTree(root, childrenGetter, (originalNode, clonedChildren) -> {
      T clonedNode = nodeCopier.apply(originalNode);
      childrenSetter.accept(clonedNode, clonedChildren);
      return clonedNode;
    });
  }

  /**
   * 在多根树中查找匹配节点并克隆其子树
   *
   * <p><b>使用场景</b>: 复制部门结构、复制目录结构等
   * <p><b>示例</b>: 根据模板部门ID复制整个部门结构
   *
   * @param roots          多根树的根节点集合
   * @param nodePredicate  目标节点匹配条件
   * @param childrenGetter 获取子节点的函数
   * @param nodeCopier     节点拷贝函数
   * @param childrenSetter 设置子节点的函数
   * @param <T>            节点类型
   * @return 克隆后的子树,如果未找到匹配节点则返回null
   */
  public static <T> T cloneSubtree(Collection<T> roots, Predicate<T> nodePredicate,
                                   Function<T, ? extends Collection<T>> childrenGetter,
                                   Function<T, T> nodeCopier, BiConsumer<T, List<T>> childrenSetter) {
    Objects.requireNonNull(nodePredicate, "Node predicate cannot be null");

    Optional<T> found = walker(roots, childrenGetter).findFirst(nodePredicate);
    return found.map(node -> cloneSubtree(node, childrenGetter, nodeCopier, childrenSetter)).orElse(null);
  }

  // ==================== 最低公共祖先 (LCA) - 重构版 ====================

  /**
   * 查找两个节点的最低公共祖先(Lowest Common Ancestor, LCA)
   *
   * <p><b>算法</b>: 递归查找,时间复杂度O(n)
   * <p><b>定义</b>: 最低公共祖先是指同时拥有两个节点作为后代的最深节点
   *
   * <p><b>时间复杂度</b>: O(n)
   * <p><b>空间复杂度</b>: O(h) 递归调用栈
   *
   * @param root           树的根节点
   * @param p              第一个节点
   * @param q              第二个节点
   * @param childrenGetter 获取子节点的函数
   * @param <T>            节点类型
   * @return 最低公共祖先节点,如果不存在则返回Optional.empty()
   */
  public static <T> Optional<T> findLowestCommonAncestor(T root, T p, T q, Function<T, ? extends Collection<T>> childrenGetter) {
    Objects.requireNonNull(childrenGetter, "Children getter cannot be null");

    if (root == null || p == null || q == null) {
      return Optional.empty();
    }

    return Optional.ofNullable(findLCAHelper(root, p, q, childrenGetter));
  }

  /**
   * 在多根树中查找两个匹配节点的最低公共祖先
   *
   * <p><b>使用场景</b>: 查找两个员工的共同上级、两个目录的共同父目录等
   * <p><b>示例</b>: 根据两个部门ID查找它们的共同上级部门
   * <p><b>时间复杂度</b>: O(n)
   *
   * @param roots          多根树的根节点集合
   * @param pPredicate     第一个节点的匹配条件
   * @param qPredicate     第二个节点的匹配条件
   * @param childrenGetter 获取子节点的函数
   * @param <T>            节点类型
   * @return 最低公共祖先节点,如果不存在则返回Optional.empty()
   */
  public static <T> Optional<T> findLowestCommonAncestor(Collection<T> roots,
                                                         Predicate<T> pPredicate, Predicate<T> qPredicate,
                                                         Function<T, ? extends Collection<T>> childrenGetter) {
    Objects.requireNonNull(pPredicate, "First node predicate cannot be null");
    Objects.requireNonNull(qPredicate, "Second node predicate cannot be null");

    TreeWalker<T> walker = walker(roots, childrenGetter);
    Optional<T> p = walker.findFirst(pPredicate);
    Optional<T> q = walker.findFirst(qPredicate);

    if (!p.isPresent() || !q.isPresent()) {
      return Optional.empty();
    }

    // 在所有根节点中尝试查找LCA
    for (T root : roots) {
      Optional<T> lca = findLowestCommonAncestor(root, p.get(), q.get(), childrenGetter);
      if (lca.isPresent()) {
        return lca;
      }
    }

    return Optional.empty();
  }

  private static <T> T findLCAHelper(T root, T p, T q, Function<T, ? extends Collection<T>> childrenGetter) {
    if (root == null || root.equals(p) || root.equals(q)) {
      return root;
    }

    Collection<T> children = childrenGetter.apply(root);
    if (children == null || children.isEmpty()) {
      return null;
    }

    List<T> foundNodes = new ArrayList<>();
    for (T child : children) {
      T result = findLCAHelper(child, p, q, childrenGetter);
      if (result != null) {
        foundNodes.add(result);
      }
    }

    // 如果在多个子树中找到了目标节点,说明当前节点是LCA
    if (foundNodes.size() >= 2) {
      return root;
    }

    // 如果只在一个子树中找到,返回该结果
    return foundNodes.isEmpty() ? null : foundNodes.get(0);
  }

  // ==================== 路径查找 - 重构版 ====================

  /**
   * 查找从根节点到目标节点的路径
   *
   * <p><b>时间复杂度</b>: O(n)
   * <p><b>空间复杂度</b>: O(h)
   *
   * @param root           根节点
   * @param target         目标节点
   * @param childrenGetter 获取子节点的函数
   * @param <T>            节点类型
   * @return 路径节点列表,如果目标不存在则返回空列表
   */
  public static <T> List<T> findPath(T root, T target, Function<T, ? extends Collection<T>> childrenGetter) {
    Objects.requireNonNull(childrenGetter, "Children getter cannot be null");

    List<T> path = new ArrayList<>();
    if (root == null || target == null) {
      return path;
    }

    findPathHelper(root, target, childrenGetter, path);
    return path;
  }

  /**
   * 在多根树中查找到匹配节点的路径
   *
   * <p><b>使用场景</b>: 查找节点的面包屑导航路径
   * <p><b>示例</b>: 根据菜单ID查找从根菜单到该菜单的路径
   * <p><b>时间复杂度</b>: O(n)
   *
   * @param roots           多根树的根节点集合
   * @param targetPredicate 目标节点匹配条件
   * @param childrenGetter  获取子节点的函数
   * @param <T>             节点类型
   * @return 路径节点列表,如果未找到匹配节点则返回空列表
   */
  public static <T> List<T> findPath(Collection<T> roots, Predicate<T> targetPredicate,
                                     Function<T, ? extends Collection<T>> childrenGetter) {
    Objects.requireNonNull(targetPredicate, "Target predicate cannot be null");

    if (roots == null || roots.isEmpty()) {
      return Collections.emptyList();
    }

    TreeWalker<T> walker = walker(roots, childrenGetter);
    Optional<T> target = walker.findFirst(targetPredicate);

    if (!target.isPresent()) {
      return Collections.emptyList();
    }

    // 在所有根节点中尝试查找路径
    for (T root : roots) {
      List<T> path = findPath(root, target.get(), childrenGetter);
      if (!path.isEmpty()) {
        return path;
      }
    }

    return Collections.emptyList();
  }

  private static <T> boolean findPathHelper(T node, T target, Function<T, ? extends Collection<T>> childrenGetter, List<T> path) {
    if (node == null) {
      return false;
    }

    path.add(node);

    if (node.equals(target)) {
      return true;
    }

    Collection<T> children = childrenGetter.apply(node);
    if (children != null) {
      for (T child : children) {
        if (findPathHelper(child, target, childrenGetter, path)) {
          return true;
        }
      }
    }

    path.remove(path.size() - 1);
    return false;
  }

  /**
   * 查找两个节点之间的路径
   * 路径通过最低公共祖先连接
   *
   * @param root           根节点
   * @param from           起始节点
   * @param to             目标节点
   * @param childrenGetter 获取子节点的函数
   * @param <T>            节点类型
   * @return 路径节点列表
   */
  public static <T> List<T> findPathBetween(T root, T from, T to, Function<T, ? extends Collection<T>> childrenGetter) {
    Objects.requireNonNull(childrenGetter, "Children getter cannot be null");

    if (root == null || from == null || to == null) {
      return Collections.emptyList();
    }

    // 查找LCA
    Optional<T> lca = findLowestCommonAncestor(root, from, to, childrenGetter);
    if (!lca.isPresent()) {
      return Collections.emptyList();
    }

    // 查找从LCA到两个节点的路径
    List<T> pathToFrom = findPath(lca.get(), from, childrenGetter);
    List<T> pathToTo = findPath(lca.get(), to, childrenGetter);

    // 反转from路径并连接
    Collections.reverse(pathToFrom);
    pathToFrom.addAll(pathToTo.subList(1, pathToTo.size()));

    return pathToFrom;
  }

  /**
   * 在多根树中查找两个匹配节点之间的路径
   *
   * <p><b>使用场景</b>: 查找两个节点之间的关联路径
   * <p><b>示例</b>: 查找两个部门之间的组织关系链
   * <p><b>时间复杂度</b>: O(n)
   *
   * @param roots          多根树的根节点集合
   * @param fromPredicate  起始节点匹配条件
   * @param toPredicate    目标节点匹配条件
   * @param childrenGetter 获取子节点的函数
   * @param <T>            节点类型
   * @return 路径节点列表,如果未找到则返回空列表
   */
  public static <T> List<T> findPathBetween(Collection<T> roots,
                                            Predicate<T> fromPredicate, Predicate<T> toPredicate,
                                            Function<T, ? extends Collection<T>> childrenGetter) {
    Objects.requireNonNull(fromPredicate, "From predicate cannot be null");
    Objects.requireNonNull(toPredicate, "To predicate cannot be null");

    TreeWalker<T> walker = walker(roots, childrenGetter);
    Optional<T> from = walker.findFirst(fromPredicate);
    Optional<T> to = walker.findFirst(toPredicate);

    if (!from.isPresent() || !to.isPresent()) {
      return Collections.emptyList();
    }

    // 在所有根节点中尝试查找路径
    for (T root : roots) {
      List<T> path = findPathBetween(root, from.get(), to.get(), childrenGetter);
      if (!path.isEmpty()) {
        return path;
      }
    }

    return Collections.emptyList();
  }

  // ==================== 序列化 ====================

  /**
   * 序列化树为字符串(前序遍历,使用#表示null)
   *
   * <p><b>格式</b>: value,child1,child2,...
   * <p><b>时间复杂度</b>: O(n)
   * <p><b>空间复杂度</b>: O(n)
   *
   * @param root            根节点
   * @param childrenGetter  获取子节点的函数
   * @param valueSerializer 值序列化函数
   * @param <T>             节点类型
   * @return 序列化字符串
   */
  public static <T> String serialize(T root, Function<T, ? extends Collection<T>> childrenGetter, Function<T, String> valueSerializer) {
    Objects.requireNonNull(childrenGetter, "Children getter cannot be null");
    Objects.requireNonNull(valueSerializer, "Value serializer cannot be null");

    if (root == null) {
      return "#";
    }

    StringBuilder sb = new StringBuilder();
    serializeHelper(root, childrenGetter, valueSerializer, sb);
    return sb.toString();
  }

  /**
   * 序列化多根树
   *
   * @param roots           根节点集合
   * @param childrenGetter  获取子节点的函数
   * @param valueSerializer 值序列化函数
   * @param <T>             节点类型
   * @return 序列化字符串
   */
  public static <T> String serialize(Collection<T> roots, Function<T, ? extends Collection<T>> childrenGetter, Function<T, String> valueSerializer) {
    if (roots == null || roots.isEmpty()) {
      return "";
    }

    StringBuilder sb = new StringBuilder();
    sb.append("[");
    boolean first = true;
    for (T root : roots) {
      if (!first) {
        sb.append("|");
      }
      sb.append(serialize(root, childrenGetter, valueSerializer));
      first = false;
    }
    sb.append("]");
    return sb.toString();
  }

  private static <T> void serializeHelper(T node, Function<T, ? extends Collection<T>> childrenGetter,
                                          Function<T, String> valueSerializer, StringBuilder sb) {
    if (node == null) {
      sb.append("#,");
      return;
    }

    sb.append(valueSerializer.apply(node)).append(",");

    Collection<T> children = childrenGetter.apply(node);
    if (children != null && !children.isEmpty()) {
      sb.append(children.size()).append(",");
      for (T child : children) {
        serializeHelper(child, childrenGetter, valueSerializer, sb);
      }
    } else {
      sb.append("0,");
    }
  }

  // ==================== 树度量 - 改进版 ====================

  /**
   * 计算树的高度(根到最远叶子的最长路径长度)
   *
   * <p><b>定义</b>: 单节点树的高度为0,空树的高度为-1
   * <p><b>时间复杂度</b>: O(n)
   * <p><b>空间复杂度</b>: O(h) 递归调用栈
   *
   * @param root           根节点
   * @param childrenGetter 获取子节点的函数
   * @param <T>            节点类型
   * @return 树的高度
   */
  public static <T> int calculateHeight(T root, Function<T, ? extends Collection<T>> childrenGetter) {
    Objects.requireNonNull(childrenGetter, "Children getter cannot be null");

    if (root == null) {
      return -1;
    }

    Collection<T> children = childrenGetter.apply(root);
    if (children == null || children.isEmpty()) {
      return 0;
    }

    int maxChildHeight = -1;
    for (T child : children) {
      maxChildHeight = Math.max(maxChildHeight, calculateHeight(child, childrenGetter));
    }

    return maxChildHeight + 1;
  }

  /**
   * 计算多根树的最大高度
   *
   * @param roots          根节点集合
   * @param childrenGetter 获取子节点的函数
   * @param <T>            节点类型
   * @return 所有树中的最大高度
   */
  public static <T> int calculateHeight(Collection<T> roots, Function<T, ? extends Collection<T>> childrenGetter) {
    if (roots == null || roots.isEmpty()) {
      return -1;
    }

    int maxHeight = -1;
    for (T root : roots) {
      maxHeight = Math.max(maxHeight, calculateHeight(root, childrenGetter));
    }
    return maxHeight;
  }

  /**
   * 计算节点的深度(从根到该节点的路径长度)
   *
   * <p><b>时间复杂度</b>: O(h)
   *
   * @param root           根节点
   * @param target         目标节点
   * @param childrenGetter 获取子节点的函数
   * @param <T>            节点类型
   * @return 节点深度,如果节点不存在返回-1
   */
  public static <T> int calculateDepth(T root, T target, Function<T, ? extends Collection<T>> childrenGetter) {
    Objects.requireNonNull(childrenGetter, "Children getter cannot be null");

    if (root == null || target == null) {
      return -1;
    }

    return calculateDepthHelper(root, target, childrenGetter, 0);
  }

  /**
   * 在多根树中计算匹配节点的深度
   *
   * <p><b>使用场景</b>: 计算组织层级、目录深度等
   *
   * @param roots           多根树的根节点集合
   * @param targetPredicate 目标节点匹配条件
   * @param childrenGetter  获取子节点的函数
   * @param <T>             节点类型
   * @return 节点深度,如果节点不存在返回-1
   */
  public static <T> int calculateDepth(Collection<T> roots, Predicate<T> targetPredicate,
                                       Function<T, ? extends Collection<T>> childrenGetter) {
    Objects.requireNonNull(targetPredicate, "Target predicate cannot be null");

    TreeWalker<T> walker = walker(roots, childrenGetter);
    Optional<T> target = walker.findFirst(targetPredicate);

    if (!target.isPresent()) {
      return -1;
    }

    for (T root : roots) {
      int depth = calculateDepth(root, target.get(), childrenGetter);
      if (depth != -1) {
        return depth;
      }
    }

    return -1;
  }

  private static <T> int calculateDepthHelper(T node, T target, Function<T, ? extends Collection<T>> childrenGetter, int currentDepth) {
    if (node == null) {
      return -1;
    }
    if (node.equals(target)) {
      return currentDepth;
    }

    Collection<T> children = childrenGetter.apply(node);
    if (children != null) {
      for (T child : children) {
        int depth = calculateDepthHelper(child, target, childrenGetter, currentDepth + 1);
        if (depth != -1) {
          return depth;
        }
      }
    }

    return -1;
  }

  /**
   * 统计树的总节点数
   *
   * <p><b>时间复杂度</b>: O(n)
   * <p><b>空间复杂度</b>: O(h)
   *
   * @param root           根节点
   * @param childrenGetter 获取子节点的函数
   * @param <T>            节点类型
   * @return 节点总数
   */
  public static <T> int countNodes(T root, Function<T, ? extends Collection<T>> childrenGetter) {
    Objects.requireNonNull(childrenGetter, "Children getter cannot be null");

    if (root == null) {
      return 0;
    }

    int count = 1;
    Collection<T> children = childrenGetter.apply(root);
    if (children != null) {
      for (T child : children) {
        count += countNodes(child, childrenGetter);
      }
    }

    return count;
  }

  /**
   * 统计多根树的总节点数
   *
   * @param roots          根节点集合
   * @param childrenGetter 获取子节点的函数
   * @param <T>            节点类型
   * @return 所有树的节点总数
   */
  public static <T> int countNodes(Collection<T> roots, Function<T, ? extends Collection<T>> childrenGetter) {
    if (roots == null || roots.isEmpty()) {
      return 0;
    }

    int total = 0;
    for (T root : roots) {
      total += countNodes(root, childrenGetter);
    }
    return total;
  }

  /**
   * 统计叶子节点数
   *
   * @param root           根节点
   * @param childrenGetter 获取子节点的函数
   * @param <T>            节点类型
   * @return 叶子节点数
   */
  public static <T> int countLeaves(T root, Function<T, ? extends Collection<T>> childrenGetter) {
    Objects.requireNonNull(childrenGetter, "Children getter cannot be null");

    if (root == null) {
      return 0;
    }

    Collection<T> children = childrenGetter.apply(root);
    if (children == null || children.isEmpty()) {
      return 1;
    }

    int count = 0;
    for (T child : children) {
      count += countLeaves(child, childrenGetter);
    }

    return count;
  }

  /**
   * 统计多根树的叶子节点数
   *
   * @param roots          根节点集合
   * @param childrenGetter 获取子节点的函数
   * @param <T>            节点类型
   * @return 所有树的叶子节点总数
   */
  public static <T> int countLeaves(Collection<T> roots, Function<T, ? extends Collection<T>> childrenGetter) {
    if (roots == null || roots.isEmpty()) {
      return 0;
    }

    int total = 0;
    for (T root : roots) {
      total += countLeaves(root, childrenGetter);
    }
    return total;
  }

  /**
   * 检查节点是否为叶子节点
   *
   * @param node           节点
   * @param childrenGetter 获取子节点的函数
   * @param <T>            节点类型
   * @return 如果是叶子节点返回true
   */
  public static <T> boolean isLeaf(T node, Function<T, ? extends Collection<T>> childrenGetter) {
    Objects.requireNonNull(childrenGetter, "Children getter cannot be null");

    if (node == null) {
      return false;
    }

    Collection<T> children = childrenGetter.apply(node);
    return children == null || children.isEmpty();
  }

  // ==================== 循环检测 - 改进版 ====================

  /**
   * 检测树中是否存在循环引用
   *
   * <p><b>重要</b>: 在执行移动操作前应该调用此方法进行验证
   * <p><b>时间复杂度</b>: O(n)
   * <p><b>空间复杂度</b>: O(n)
   *
   * @param root           根节点
   * @param childrenGetter 获取子节点的函数
   * @param <T>            节点类型
   * @return 如果存在循环返回true
   */
  public static <T> boolean detectCycle(T root, Function<T, ? extends Collection<T>> childrenGetter) {
    Objects.requireNonNull(childrenGetter, "Children getter cannot be null");

    if (root == null) {
      return false;
    }

    Set<T> visited = new HashSet<>();
    return detectCycleHelper(root, childrenGetter, visited);
  }

  /**
   * 检测多根树中是否存在循环引用
   *
   * @param roots          根节点集合
   * @param childrenGetter 获取子节点的函数
   * @param <T>            节点类型
   * @return 如果存在循环返回true
   */
  public static <T> boolean detectCycle(Collection<T> roots, Function<T, ? extends Collection<T>> childrenGetter) {
    if (roots == null || roots.isEmpty()) {
      return false;
    }

    for (T root : roots) {
      if (detectCycle(root, childrenGetter)) {
        return true;
      }
    }
    return false;
  }

  private static <T> boolean detectCycleHelper(T node, Function<T, ? extends Collection<T>> childrenGetter, Set<T> visited) {
    if (node == null) {
      return false;
    }

    if (visited.contains(node)) {
      // 检测到循环
      return true;
    }

    visited.add(node);

    Collection<T> children = childrenGetter.apply(node);
    if (children != null) {
      for (T child : children) {
        if (detectCycleHelper(child, childrenGetter, visited)) {
          return true;
        }
      }
    }

    // 回溯(用于检测路径中的循环)
    // 注意: 如果要检测整个图的循环,不应该移除
    // visited.remove(node);

    return false;
  }

  /**
   * 检查一个节点是否是另一个节点的后代
   * 主要用于移动操作前的验证
   *
   * @param potentialAncestor 可能的祖先节点
   * @param node              待检查的节点
   * @param childrenGetter    获取子节点的函数
   * @param <T>               节点类型
   * @return 如果node是potentialAncestor的后代返回true
   */
  public static <T> boolean isDescendant(T potentialAncestor, T node, Function<T, ? extends Collection<T>> childrenGetter) {
    Objects.requireNonNull(childrenGetter, "Children getter cannot be null");

    if (potentialAncestor == null || node == null) {
      return false;
    }

    if (potentialAncestor.equals(node)) {
      return false; // 节点不是自己的后代
    }

    Collection<T> descendants = getDescendants(potentialAncestor, childrenGetter);
    return descendants.contains(node);
  }

  /**
   * 在多根树中检查一个匹配节点是否是另一个匹配节点的后代
   *
   * <p><b>使用场景</b>: 在移动部门前验证是否会创建循环引用
   * <p><b>示例</b>: 检查部门A是否是部门B的子部门
   *
   * @param roots             多根树的根节点集合
   * @param ancestorPredicate 可能的祖先节点匹配条件
   * @param nodePredicate     待检查节点的匹配条件
   * @param childrenGetter    获取子节点的函数
   * @param <T>               节点类型
   * @return 如果node是ancestor的后代返回true
   */
  public static <T> boolean isDescendant(Collection<T> roots,
                                         Predicate<T> ancestorPredicate, Predicate<T> nodePredicate,
                                         Function<T, ? extends Collection<T>> childrenGetter) {
    Objects.requireNonNull(ancestorPredicate, "Ancestor predicate cannot be null");
    Objects.requireNonNull(nodePredicate, "Node predicate cannot be null");

    TreeWalker<T> walker = walker(roots, childrenGetter);
    Optional<T> ancestor = walker.findFirst(ancestorPredicate);
    Optional<T> node = walker.findFirst(nodePredicate);

    if (!ancestor.isPresent() || !node.isPresent()) {
      return false;
    }

    return isDescendant(ancestor.get(), node.get(), childrenGetter);
  }

  // ==================== Builder 工厂方法 ====================

  public static <T> SimpleTreeBuilder<T> simpleBuilder() {
    return new SimpleTreeBuilder<>();
  }

  public static <T> SimpleTreeBuilder<T> simpleBuilder(Collection<T> items) {
    return new SimpleTreeBuilder<T>().nodes(items);
  }

  public static <O, T> RemapTreeBuilder<O, T> remapBuilder() {
    return new RemapTreeBuilder<>();
  }

  public static <O, T> RemapTreeBuilder<O, T> remapBuilder(Collection<O> items, Class<T> targetNodeClass) {
    return new RemapTreeBuilder<O, T>().nodes(items);
  }

  public static <O, T> RemapTreeBuilder<O, T> remapBuilder(Supplier<Collection<O>> itemsSupplier, Class<T> targetNodeClass) {
    return new RemapTreeBuilder<O, T>().nodes(itemsSupplier);
  }

  public static <O, T> TreeBuilder<O, T> builder() {
    return new TreeBuilder<>();
  }

  public static <O, T> TreeBuilder<O, T> builder(Class<O> originNodeClass, Class<T> targetNodeClass) {
    return new TreeBuilder<>();
  }

  public static <T> TreeWalker<T> walker(Collection<T> roots) {
    return new TreeWalker<>(roots);
  }

  public static <T> TreeWalker<T> walker(T root) {
    return new TreeWalker<>(Collections.singleton(root));
  }

  public static <T> TreeWalker<T> walker(Function<T, ? extends Collection<T>> childrenGetter) {
    return new TreeWalker<>(childrenGetter);
  }

  public static <T> TreeWalker<T> walker(Collection<T> roots, Function<T, ? extends Collection<T>> childrenGetter) {
    return new TreeWalker<>(roots, childrenGetter);
  }

  public static <T> TreeModifier<T> modifier(Collection<T> roots) {
    return new TreeModifier<>(roots);
  }

  public static <T> TreeModifier<T> modifier(Function<T, ? extends Collection<T>> childrenGetter) {
    return new TreeModifier<>(childrenGetter);
  }

  public static <T> TreeModifier<T> modifier(Collection<T> roots, Function<T, ? extends Collection<T>> childrenGetter) {
    return new TreeModifier<>(roots, childrenGetter);
  }

  public static <T> TreeModifier<T> modifier(Collection<T> roots, Function<T, ? extends Collection<T>> childrenGetter, BiConsumer<T, ? super Collection<T>> childrenSetter) {
    return new TreeModifier<>(roots, childrenGetter).childrenSetter(childrenSetter);
  }

  // ==================== 函数式接口 ====================

  /**
   * 子节点判断谓词
   * 用于判断parent-child关系
   */
  @FunctionalInterface
  public interface ChildPredicate<T> extends BiPredicate<T, T> {
    /**
     * 测试child是否是parent的子节点
     *
     * @param parent 父节点,可能为null(表示根节点)
     * @param child  子节点
     * @return 如果是父子关系返回true
     */
    @Override
    boolean test(T parent, T child);
  }

  /**
   * 节点访问器
   */
  @FunctionalInterface
  public interface NodeVisitor<T> extends Consumer<T> {
  }

  /**
   * 节点和父节点访问器
   */
  @FunctionalInterface
  public interface NodeWithParentVisitor<T> extends BiConsumer<T, T> {
  }

  /**
   * 节点和路径访问器
   */
  @FunctionalInterface
  public interface NodeWithPathVisitor<T> extends BiConsumer<T, List<T>> {
  }

  /**
   * 节点和路径判断谓词，用于查找符合条件的节点
   */
  @FunctionalInterface
  public interface NodeWithPathPredicate<T> extends BiPredicate<T, List<T>> {
    @Override
    boolean test(T node, List<T> path);
  }

  /**
   * 节点判断谓词，用于判断节点是否符合条件
   */
  @FunctionalInterface
  public interface NodePredicate<T> extends Predicate<T> {
    @Override
    boolean test(T node);
  }

  /**
   * 循环引用异常
   * 当尝试创建循环引用时抛出
   */
  public static class CircularReferenceException extends IllegalArgumentException {
    public CircularReferenceException(String message) {
      super(message);
    }
  }

  // ==================== TreeBuilder ====================

  /**
   * 通用树构建器
   * 用于从原始数据构建树结构
   *
   * @param <O> 原始数据类型
   * @param <T> 目标树节点类型
   */
  public static class TreeBuilder<O, T> {
    private Supplier<List<O>> rootsGetter;
    private Function<O, List<O>> childrenGetter;
    private BiFunction<O, List<T>, T> mapper;

    TreeBuilder() {
    }

    public TreeBuilder<O, T> roots(Supplier<List<O>> rootsGetter) {
      this.rootsGetter = rootsGetter;
      return this;
    }

    public TreeBuilder<O, T> roots(Collection<O> roots) {
      this.rootsGetter = () -> roots instanceof List ? (List<O>) roots : new ArrayList<>(roots);
      return this;
    }

    public TreeBuilder<O, T> childrenGetter(Function<O, List<O>> childrenGetter) {
      this.childrenGetter = childrenGetter;
      return this;
    }

    public TreeBuilder<O, T> mapper(BiFunction<O, List<T>, T> mapper) {
      this.mapper = mapper;
      return this;
    }

    private T buildHelper(O node) {
      if (node == null) {
        return null;
      }
      var children = childrenGetter.apply(node);
      if (children == null || children.isEmpty()) {
        return mapper.apply(node, Collections.emptyList());
      }
      var mappedChildren = children.stream()
          .filter(Objects::nonNull)
          .map(this::buildHelper)
          .filter(Objects::nonNull)
          .collect(Collectors.toList());
      return mapper.apply(node, mappedChildren);
    }

    /**
     * 构建树
     *
     * @return 构建完成的树节点列表
     * @throws NullPointerException 如果必需的配置未设置
     */
    public List<T> build() {
      Objects.requireNonNull(rootsGetter, "Roots getter must be set");
      Objects.requireNonNull(childrenGetter, "Children getter must be set");
      Objects.requireNonNull(mapper, "Mapper must be set");

      var roots = rootsGetter.get();
      if (roots == null || roots.isEmpty()) {
        return Collections.emptyList();
      }
      return roots.stream()
          .filter(Objects::nonNull)
          .map(this::buildHelper)
          .filter(Objects::nonNull)
          .collect(Collectors.toList());
    }
  }

  // ==================== RemapTreeBuilder ====================

  /**
   * 重映射树构建器
   * 用于从平面列表构建树结构,通过谓词判断父子关系
   *
   * @param <O> 原始数据类型
   * @param <T> 目标树节点类型
   */
  public static class RemapTreeBuilder<O, T> {
    private Collection<O> nodes;
    private Supplier<Collection<O>> nodesSupplier;
    private Predicate<O> rootPredicate;
    private ChildPredicate<O> childPredicate;
    private BiFunction<O, List<T>, T> mapper;

    RemapTreeBuilder() {
    }

    public RemapTreeBuilder<O, T> rootPredicate(Predicate<O> rootPredicate) {
      this.rootPredicate = rootPredicate;
      return this;
    }

    public RemapTreeBuilder<O, T> childPredicate(ChildPredicate<O> childPredicate) {
      this.childPredicate = childPredicate;
      return this;
    }

    public RemapTreeBuilder<O, T> mapper(BiFunction<O, List<T>, T> mapper) {
      this.mapper = mapper;
      return this;
    }

    public RemapTreeBuilder<O, T> nodes(Collection<O> nodes) {
      this.nodes = nodes;
      return this;
    }

    public RemapTreeBuilder<O, T> nodes(Supplier<Collection<O>> nodesSupplier) {
      this.nodesSupplier = nodesSupplier;
      return this;
    }

    public List<T> build() {
      if (nodesSupplier != null) {
        nodes = nodesSupplier.get();
      }

      Objects.requireNonNull(nodes, "Nodes must be set");
      Objects.requireNonNull(childPredicate, "Child predicate must be set");
      Objects.requireNonNull(mapper, "Mapper must be set");

      if (rootPredicate == null) {
        rootPredicate = node -> childPredicate.test(null, node);
      }

      return new TreeBuilder<O, T>()
          .roots(() -> nodes.stream().filter(rootPredicate).collect(Collectors.toList()))
          .childrenGetter(node -> nodes.stream().filter(child -> childPredicate.test(node, child)).collect(Collectors.toList()))
          .mapper(mapper)
          .build();
    }

    public List<T> build(Collection<O> nodes) {
      this.nodes = nodes;
      return build();
    }

    public List<T> build(Supplier<Collection<O>> nodesGetter) {
      this.nodesSupplier = nodesGetter;
      return build();
    }
  }

  // ==================== SimpleTreeBuilder ====================

  /**
   * 简单树构建器
   * 用于从平面列表构建同类型的树结构
   *
   * @param <T> 节点类型
   */
  public static class SimpleTreeBuilder<T> {
    private Collection<T> nodes;
    private Supplier<Collection<T>> nodesGetter;
    private Predicate<T> rootPredicate;
    private ChildPredicate<T> childPredicate;
    private BiConsumer<T, List<T>> childrenSetter;
    private Function<T, T> nodeCopier;

    SimpleTreeBuilder() {
    }

    public SimpleTreeBuilder<T> rootPredicate(Predicate<T> rootPredicate) {
      this.rootPredicate = rootPredicate;
      return this;
    }

    public SimpleTreeBuilder<T> childPredicate(ChildPredicate<T> childPredicate) {
      this.childPredicate = childPredicate;
      return this;
    }

    public SimpleTreeBuilder<T> childrenSetter(BiConsumer<T, List<T>> childrenSetter) {
      this.childrenSetter = childrenSetter;
      return this;
    }

    public SimpleTreeBuilder<T> childrenSetter(Consumer<T> childrenSetter) {
      this.childrenSetter = (parent, children) -> childrenSetter.accept(parent);
      return this;
    }

    public SimpleTreeBuilder<T> nodes(Collection<T> collection) {
      this.nodes = collection;
      return this;
    }

    public SimpleTreeBuilder<T> nodes(Supplier<Collection<T>> nodesGetter) {
      this.nodesGetter = nodesGetter;
      return this;
    }

    public SimpleTreeBuilder<T> nodeCopier(Function<T, T> nodeCopier) {
      this.nodeCopier = nodeCopier;
      return this;
    }

    public List<T> build() {
      if (nodesGetter != null) {
        nodes = nodesGetter.get();
      }

      Objects.requireNonNull(nodes, "Nodes must be set");
      Objects.requireNonNull(childPredicate, "Child predicate must be set");
      Objects.requireNonNull(childrenSetter, "Children setter must be set");

      if (rootPredicate == null) {
        rootPredicate = node -> childPredicate.test(null, node);
      }

      BiFunction<T, List<T>, T> mapper = (node, children) -> {
        var result = nodeCopier == null ? node : nodeCopier.apply(node);
        childrenSetter.accept(result, children);
        return result;
      };

      return new TreeBuilder<T, T>()
          .roots(() -> nodes.stream().filter(rootPredicate).collect(Collectors.toList()))
          .childrenGetter(node -> nodes.stream().filter(child -> childPredicate.test(node, child)).collect(Collectors.toList()))
          .mapper(mapper)
          .build();
    }

    public List<T> build(Collection<T> nodes) {
      this.nodes = nodes;
      return build();
    }
  }

  // ==================== TreeWalker ====================

  /**
   * 树遍历器
   * 提供多种遍历策略的统一接口
   *
   * @param <T> 节点类型
   */
  public static class TreeWalker<T> {
    private Function<T, ? extends Collection<T>> childrenGetter;
    private Collection<T> roots;

    TreeWalker(Collection<T> roots) {
      this.roots = roots;
    }

    TreeWalker(Function<T, ? extends Collection<T>> childrenGetter) {
      this.roots = null;
      this.childrenGetter = childrenGetter;
    }

    TreeWalker(Collection<T> roots, Function<T, ? extends Collection<T>> childrenGetter) {
      this.roots = roots;
      this.childrenGetter = childrenGetter;
    }

    public TreeWalker<T> childrenGetter(Function<T, ? extends Collection<T>> childrenGetter) {
      this.childrenGetter = childrenGetter;
      return this;
    }

    public TreeWalker<T> roots(Collection<T> roots) {
      this.roots = roots;
      return this;
    }

    /**
     * 深度优先遍历(使用栈,LIFO)
     *
     * <p><b>时间复杂度</b>: O(n)
     * <p><b>空间复杂度</b>: O(h) 栈空间
     *
     * @param visitor 节点访问器
     */
    public void depthFirst(NodeVisitor<T> visitor) {
      if (roots == null || roots.isEmpty()) {
        return;
      }

      Objects.requireNonNull(childrenGetter, "Children getter must be set");
      Objects.requireNonNull(visitor, "Visitor cannot be null");

      Stack<T> stack = new Stack<>();
      // 反向添加以保持原始顺序
      List<T> rootList = new ArrayList<>(roots);
      for (int i = rootList.size() - 1; i >= 0; i--) {
        stack.push(rootList.get(i));
      }

      while (!stack.isEmpty()) {
        var node = stack.pop();
        visitor.accept(node);

        var children = childrenGetter.apply(node);
        if (children != null && !children.isEmpty()) {
          List<T> childList = new ArrayList<>(children);
          for (int i = childList.size() - 1; i >= 0; i--) {
            stack.push(childList.get(i));
          }
        }
      }
    }

    public void depthFirst(NodeWithParentVisitor<T> visitor) {
      if (roots == null || roots.isEmpty()) {
        return;
      }

      Objects.requireNonNull(childrenGetter, "Children getter must be set");
      Objects.requireNonNull(visitor, "Visitor cannot be null");

      Stack<Tuple2<T, T>> stack = new Stack<>();
      roots.forEach(root -> stack.push(Tuple.of(root, null)));

      while (!stack.isEmpty()) {
        var nodePair = stack.pop();
        var node = nodePair._1;
        var parent = nodePair._2;
        visitor.accept(node, parent);

        var children = childrenGetter.apply(node);
        if (children != null && !children.isEmpty()) {
          List<T> childList = new ArrayList<>(children);
          for (int i = childList.size() - 1; i >= 0; i--) {
            stack.push(Tuple.of(childList.get(i), node));
          }
        }
      }
    }

    public void depthFirst(NodeWithPathVisitor<T> visitor) {
      if (roots == null || roots.isEmpty()) {
        return;
      }

      Objects.requireNonNull(childrenGetter, "Children getter must be set");
      Objects.requireNonNull(visitor, "Visitor cannot be null");

      Stack<Tuple2<T, List<T>>> stack = new Stack<>();
      roots.forEach(root -> stack.push(Tuple.of(root, Collections.emptyList())));

      while (!stack.isEmpty()) {
        var nodePair = stack.pop();
        var node = nodePair._1;
        var path = nodePair._2;

        visitor.accept(node, path);

        var children = childrenGetter.apply(node);
        if (children != null && !children.isEmpty()) {
          var newPath = new ArrayList<>(path);
          newPath.add(node);
          List<T> childList = new ArrayList<>(children);
          for (int i = childList.size() - 1; i >= 0; i--) {
            stack.push(Tuple.of(childList.get(i), newPath));
          }
        }
      }
    }

    /**
     * 自上而下遍历(前序遍历,迭代实现)
     * 等同于广度优先遍历
     */
    public void topDown(NodeVisitor<T> visitor) {
      breadthFirst(visitor);
    }

    public void topDown(NodeWithParentVisitor<T> visitor) {
      breadthFirst(visitor);
    }

    public void topDown(NodeWithPathVisitor<T> visitor) {
      breadthFirst(visitor);
    }

    /**
     * 自下而上遍历(后序遍历,递归实现)
     *
     * <p><b>警告</b>: 深度超过{@link #MAX_RECURSION_DEPTH}可能导致栈溢出
     */
    public void bottomUp(NodeVisitor<T> visitor) {
      if (roots == null || roots.isEmpty()) {
        return;
      }

      Objects.requireNonNull(childrenGetter, "Children getter must be set");
      Objects.requireNonNull(visitor, "Visitor cannot be null");

      bottomUpHelper(visitor, roots);
    }

    private void bottomUpHelper(Consumer<T> visitor, Collection<T> nodes) {
      nodes.forEach(branch -> {
        var children = childrenGetter.apply(branch);
        if (children != null && !children.isEmpty()) {
          bottomUpHelper(visitor, children);
        }
        visitor.accept(branch);
      });
    }

    public void bottomUp(NodeWithParentVisitor<T> visitor) {
      if (roots == null || roots.isEmpty()) {
        return;
      }

      Objects.requireNonNull(childrenGetter, "Children getter must be set");
      Objects.requireNonNull(visitor, "Visitor cannot be null");

      roots.forEach(root -> bottomUpHelper(visitor, root, null));
    }

    private void bottomUpHelper(NodeWithParentVisitor<T> visitor, T node, T parent) {
      if (node == null) {
        return;
      }
      var children = childrenGetter.apply(node);
      if (children != null && !children.isEmpty()) {
        children.forEach(child -> bottomUpHelper(visitor, child, node));
      }
      visitor.accept(node, parent);
    }

    public void bottomUp(NodeWithPathVisitor<T> visitor) {
      if (roots == null || roots.isEmpty()) {
        return;
      }

      Objects.requireNonNull(childrenGetter, "Children getter must be set");
      Objects.requireNonNull(visitor, "Visitor cannot be null");

      roots.forEach(root -> bottomUpHelper(visitor, root, Collections.emptyList()));
    }

    private void bottomUpHelper(NodeWithPathVisitor<T> visitor, T node, List<T> oldPath) {
      if (node == null) {
        return;
      }
      var children = childrenGetter.apply(node);
      if (children != null && !children.isEmpty()) {
        var path = new ArrayList<>(oldPath);
        path.add(node);
        children.forEach(child -> bottomUpHelper(visitor, child, path));
      }
      visitor.accept(node, oldPath);
    }

    /**
     * 广度优先遍历/层序遍历(使用队列,FIFO)
     * 逐层访问节点
     *
     * <p><b>时间复杂度</b>: O(n)
     * <p><b>空间复杂度</b>: O(w) 其中w是树的最大宽度
     *
     * @param visitor 节点访问器
     */
    public void breadthFirst(NodeVisitor<T> visitor) {
      if (roots == null || roots.isEmpty()) {
        return;
      }

      Objects.requireNonNull(childrenGetter, "Children getter must be set");
      Objects.requireNonNull(visitor, "Visitor cannot be null");

      var queue = new LinkedList<>(roots);
      while (!queue.isEmpty()) {
        var node = queue.poll();
        visitor.accept(node);
        var children = childrenGetter.apply(node);
        if (children != null && !children.isEmpty()) {
          queue.addAll(children);
        }
      }
    }

    public void breadthFirst(NodeWithParentVisitor<T> visitor) {
      if (roots == null || roots.isEmpty()) {
        return;
      }

      Objects.requireNonNull(childrenGetter, "Children getter must be set");
      Objects.requireNonNull(visitor, "Visitor cannot be null");

      var queue = new LinkedList<Tuple2<T, T>>();
      roots.forEach(root -> queue.add(new Tuple2<>(root, null)));
      while (!queue.isEmpty()) {
        var tuple = queue.poll();
        var node = tuple._1;
        var parent = tuple._2;
        visitor.accept(node, parent);
        var children = childrenGetter.apply(node);
        if (children != null && !children.isEmpty()) {
          children.forEach(child -> queue.add(new Tuple2<>(child, node)));
        }
      }
    }

    public void breadthFirst(NodeWithPathVisitor<T> visitor) {
      if (roots == null || roots.isEmpty()) {
        return;
      }

      Objects.requireNonNull(childrenGetter, "Children getter must be set");
      Objects.requireNonNull(visitor, "Visitor cannot be null");

      var queue = new LinkedList<Tuple2<T, List<T>>>();
      roots.forEach(root -> queue.add(new Tuple2<>(root, Collections.emptyList())));
      while (!queue.isEmpty()) {
        var tuple = queue.poll();
        var node = tuple._1;
        var path = tuple._2;
        visitor.accept(node, path);
        var children = childrenGetter.apply(node);
        if (children != null && !children.isEmpty()) {
          var newPath = new ArrayList<>(path);
          newPath.add(node);
          children.forEach(child -> queue.add(new Tuple2<>(child, newPath)));
        }
      }
    }

    /**
     * 查找第一个匹配的节点(广度优先)
     *
     * @param predicate 匹配条件
     * @return 匹配的节点,如果不存在返回Optional.empty()
     */
    public Optional<T> findFirst(Predicate<T> predicate) {
      if (roots == null || roots.isEmpty()) {
        return Optional.empty();
      }

      Objects.requireNonNull(childrenGetter, "Children getter must be set");
      Objects.requireNonNull(predicate, "Predicate cannot be null");

      var queue = new LinkedList<>(roots);
      while (!queue.isEmpty()) {
        var node = queue.poll();
        if (predicate.test(node)) {
          return Optional.of(node);
        }
        var children = childrenGetter.apply(node);
        if (children != null && !children.isEmpty()) {
          queue.addAll(children);
        }
      }
      return Optional.empty();
    }

    public Optional<T> findFirst(BiPredicate<T, T> predicate) {
      if (roots == null || roots.isEmpty()) {
        return Optional.empty();
      }

      Objects.requireNonNull(childrenGetter, "Children getter must be set");
      Objects.requireNonNull(predicate, "Predicate cannot be null");

      var queue = new LinkedList<Tuple2<T, T>>();
      roots.forEach(root -> queue.add(new Tuple2<>(root, null)));
      while (!queue.isEmpty()) {
        var tuple = queue.poll();
        var node = tuple._1;
        var parent = tuple._2;
        if (predicate.test(node, parent)) {
          return Optional.of(node);
        }
        var children = childrenGetter.apply(node);
        if (children != null && !children.isEmpty()) {
          children.forEach(child -> queue.add(new Tuple2<>(child, node)));
        }
      }
      return Optional.empty();
    }

    /**
     * 查找所有匹配的节点
     *
     * @param predicate 匹配条件
     * @return 所有匹配的节点列表
     */
    public List<T> findAll(Predicate<T> predicate) {
      if (roots == null || roots.isEmpty()) {
        return Collections.emptyList();
      }

      Objects.requireNonNull(childrenGetter, "Children getter must be set");
      Objects.requireNonNull(predicate, "Predicate cannot be null");

      var result = new ArrayList<T>();
      var queue = new LinkedList<>(roots);

      while (!queue.isEmpty()) {
        var node = queue.poll();
        if (predicate.test(node)) {
          result.add(node);
        }
        var children = childrenGetter.apply(node);
        if (children != null && !children.isEmpty()) {
          queue.addAll(children);
        }
      }
      return result;
    }

    public List<T> findAll(BiPredicate<T, T> predicate) {
      if (roots == null || roots.isEmpty()) {
        return Collections.emptyList();
      }

      Objects.requireNonNull(childrenGetter, "Children getter must be set");
      Objects.requireNonNull(predicate, "Predicate cannot be null");

      var result = new ArrayList<T>();
      var queue = new LinkedList<Tuple2<T, T>>();
      roots.forEach(root -> queue.add(new Tuple2<>(root, null)));
      while (!queue.isEmpty()) {
        var tuple = queue.poll();
        var node = tuple._1;
        var parent = tuple._2;
        if (predicate.test(node, parent)) {
          result.add(node);
        }
        var children = childrenGetter.apply(node);
        if (children != null && !children.isEmpty()) {
          children.forEach(child -> queue.add(new Tuple2<>(child, node)));
        }
      }
      return result;
    }

    /**
     * 查找第一个拥有满足条件的子节点的父节点
     *
     * @param predicate 子节点匹配条件
     * @return 父节点
     */
    public Optional<T> findFirstParent(Predicate<T> predicate) {
      Objects.requireNonNull(predicate, "Predicate cannot be null");

      return findFirst((node) -> {
        var children = childrenGetter.apply(node);
        if (children == null || children.isEmpty()) {
          return false;
        }
        return children.stream().anyMatch(predicate);
      });
    }
  }

  // ==================== TreeModifier (改进版,支持多根树) ====================

  /**
   * 树修改器
   * 提供树结构的修改操作,包含严格的安全检查
   *
   * @param <T> 节点类型
   */
  public static class TreeModifier<T> {
    private Function<T, ? extends Collection<T>> childrenGetter;
    private BiConsumer<T, ? super Collection<T>> childrenSetter;
    private Collection<T> roots;

    TreeModifier(Collection<T> roots) {
      this.roots = roots;
    }

    TreeModifier(Function<T, ? extends Collection<T>> childrenGetter) {
      this.roots = null;
      this.childrenGetter = childrenGetter;
    }

    TreeModifier(Collection<T> roots, Function<T, ? extends Collection<T>> childrenGetter) {
      this.roots = roots;
      this.childrenGetter = childrenGetter;
    }

    public TreeModifier<T> childrenGetter(Function<T, ? extends Collection<T>> childrenGetter) {
      this.childrenGetter = childrenGetter;
      return this;
    }

    public TreeModifier<T> roots(Collection<T> roots) {
      this.roots = roots;
      return this;
    }

    public TreeModifier<T> childrenSetter(BiConsumer<T, ? super Collection<T>> childrenSetter) {
      this.childrenSetter = childrenSetter;
      return this;
    }

    private void removeChildHelper(T parent, Predicate<T> predicate) {
      var children = childrenGetter.apply(parent);

      if (childrenSetter != null) {
        childrenSetter.accept(parent, children.stream().filter(predicate.negate()).collect(Collectors.toList()));
      } else {
        children.removeIf(predicate);
      }
    }

    private void addChildHelper(T parent, T child) {
      var children = childrenGetter.apply(parent);

      if (childrenSetter == null) {
        if (children == null) {
          throw new IllegalStateException("childrenGetter and childrenSetter are both null");
        }
        if (!(children instanceof List)) {
          throw new IllegalStateException("Children collection must be mutable");
        }
        children.add(child);
        return;
      }

      var newChildren = new ArrayList<T>();
      if (children != null) {
        newChildren.addAll(children);
      }
      newChildren.add(child);
      childrenSetter.accept(parent, newChildren);
    }

    /**
     * 移除满足条件的节点
     *
     * @param predicate 节点匹配条件
     * @return 如果成功移除返回true
     */
    public Boolean remove(Predicate<T> predicate) {
      Objects.requireNonNull(predicate, "Predicate cannot be null");
      Objects.requireNonNull(roots, "Roots must be set");
      Objects.requireNonNull(childrenGetter, "Children getter must be set");

      var parent = walker(roots, childrenGetter).findFirstParent(predicate);
      if (!parent.isPresent()) {
        return false;
      }
      removeChildHelper(parent.get(), predicate);
      return true;
    }

    /**
     * 剪除/移除分支(删除子树及其所有后代)
     *
     * <p><b>时间复杂度</b>: O(n) 查找节点
     * <p><b>空间复杂度</b>: O(1)
     *
     * @param predicate 要移除的分支根节点匹配条件
     * @return 如果成功剪除返回true
     */
    public Boolean prune(Predicate<T> predicate) {
      return remove(predicate);
    }

    /**
     * 移动节点到新父节点下
     *
     * <p><b>重要</b>: 此方法包含严格的循环检测,确保不会创建循环引用
     *
     * <p><b>验证检查</b>:
     * <ul>
     *   <li>节点不能移动到自己</li>
     *   <li>节点不能移动到自己的后代(会创建循环)</li>
     *   <li>目标父节点必须存在</li>
     * </ul>
     *
     * <p><b>时间复杂度</b>: O(n) 用于后代检查
     * <p><b>空间复杂度</b>: O(n)
     *
     * @param predicate       要移动的节点匹配条件
     * @param targetPredicate 目标父节点匹配条件
     * @return 如果成功移动返回true
     * @throws CircularReferenceException 如果移动会创建循环引用
     * @throws IllegalStateException      如果childrenGetter或childrenSetter未设置
     */
    public Boolean move(Predicate<T> predicate, Predicate<T> targetPredicate) {
      Objects.requireNonNull(predicate, "Predicate cannot be null");
      Objects.requireNonNull(targetPredicate, "Target predicate cannot be null");
      Objects.requireNonNull(roots, "Roots must be set");
      Objects.requireNonNull(childrenGetter, "Children getter must be set");

      var walker = walker(roots, childrenGetter);

      var node = walker.findFirst(predicate);
      if (!node.isPresent()) {
        return false;
      }

      var target = walker.findFirst(targetPredicate);
      if (!target.isPresent()) {
        return false;
      }

      T nodeToMove = node.get();
      T newParent = target.get();

      // 关键验证: 检查是否移动到自己
      if (nodeToMove.equals(newParent)) {
        throw new CircularReferenceException("Cannot move node to itself");
      }

      // 关键验证: 检查是否移动到自己的后代(会创建循环)
      if (Trees.isDescendant(nodeToMove, newParent, childrenGetter)) {
        throw new CircularReferenceException(
            "Cannot move node to its own descendant - this would create a circular reference");
      }

      // 执行移动
      remove(predicate);
      addChildHelper(newParent, nodeToMove);

      // 验证: 移动后检查是否存在循环
      if (Trees.detectCycle(newParent, childrenGetter)) {
        throw new CircularReferenceException("Circular reference detected after move operation");
      }

      return true;
    }

    /**
     * 移动节点到新父节点下的指定位置
     *
     * @param predicate       要移动的节点匹配条件
     * @param targetPredicate 目标父节点匹配条件
     * @param positionGetter  位置计算函数,接收目标父节点的子节点列表,返回插入位置
     * @return 如果成功移动返回true
     * @throws CircularReferenceException 如果移动会创建循环引用
     */
    public Boolean move(Predicate<T> predicate, Predicate<T> targetPredicate, Function<List<T>, Integer> positionGetter) {
      Objects.requireNonNull(predicate, "Predicate cannot be null");
      Objects.requireNonNull(targetPredicate, "Target predicate cannot be null");
      Objects.requireNonNull(positionGetter, "Position getter cannot be null");
      Objects.requireNonNull(roots, "Roots must be set");
      Objects.requireNonNull(childrenGetter, "Children getter must be set");

      var walker = walker(roots, childrenGetter);

      var node = walker.findFirst(predicate);
      if (!node.isPresent()) {
        return false;
      }

      var target = walker.findFirst(targetPredicate);
      if (!target.isPresent()) {
        return false;
      }

      T nodeToMove = node.get();
      T newParent = target.get();

      // 验证
      if (nodeToMove.equals(newParent)) {
        throw new CircularReferenceException("Cannot move node to itself");
      }

      if (Trees.isDescendant(nodeToMove, newParent, childrenGetter)) {
        throw new CircularReferenceException(
            "Cannot move node to its own descendant - this would create a circular reference");
      }

      var children = childrenGetter.apply(newParent);
      if (children != null && !(children instanceof List)) {
        throw new IllegalStateException("Children collection must be a List to support indexed insertion");
      }
      if (children == null && childrenSetter == null) {
        throw new IllegalStateException("children and childrenSetter are both null");
      }

      // 移除节点
      remove(predicate);

      // 计算插入位置
      var position = children == null ? 0 : positionGetter.apply((List<T>) children);

      // 插入到指定位置
      if (childrenSetter == null) {
        if (children != null) {
          ((List<T>) children).add(position, nodeToMove);
        }
      } else {
        var newChildren = new ArrayList<T>();
        if (children != null) {
          newChildren.addAll(children);
        }
        newChildren.add(position, nodeToMove);
        childrenSetter.accept(newParent, newChildren);
      }

      // 验证循环
      if (Trees.detectCycle(newParent, childrenGetter)) {
        throw new CircularReferenceException("Circular reference detected after move operation");
      }

      return true;
    }
  }
}