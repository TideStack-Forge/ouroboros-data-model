package com.ouroboros.util;

import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import io.vavr.*;

/**
 * Functional Programming Utils
 */
@SuppressWarnings("unused")
public class Functions {
  /**
   * 链式连接多个函数
   *
   * @param first 第一个函数
   * @return 链式连接的函数
   */
  public static <T, R> Chain<T, R> chain(Function<T, R> first) {
    return new Chain<>() {
      @Override
      public <R2> Chain<T, R2> next(Function<R, R2> nextFn) {
        return new Chain<>() {
          @Override
          public <R3> Chain<T, R3> next(Function<R2, R3> nextFn2) {
            return chain(t -> nextFn2.apply(end().apply(t)));
          }

          @Override
          public Function1<T, R2> end() {
            return t -> nextFn.apply(first.apply(t));
          }
        };
      }

      @Override
      public Function1<T, R> end() {
        return first::apply;
      }
    };
  }

  /**
   * 组合函数
   *
   * @param <T1> 函数1的输入类型，组合后的函数的输入类型
   * @param <R>  组合后的函数的输出类型
   * @return 组合后的函数
   */
  public static <T1, T2, R> Function1<T1, R> compose(Function<T1, T2> f1, Function<T2, R> f2) {
    return t1 -> f2.apply(f1.apply(t1));
  }

  /**
   * 组合函数
   *
   * @param <T1> 函数1的输入类型，组合后的函数的输入类型
   * @param <R>  组合后的函数的输出类型
   * @return 组合后的函数
   */
  public static <T1, T2, T3, R> Function1<T1, R> compose(Function<T1, T2> f1, Function<T2, T3> f2, Function<T3, R> f3) {
    return t1 -> f3.apply(f2.apply(f1.apply(t1)));
  }

  /**
   * 组合函数
   *
   * @param <T1> 函数1的输入类型，组合后的函数的输入类型
   * @param <R>  组合后的函数的输出类型
   * @return 组合后的函数
   */
  public static <T1, T2, T3, T4, R> Function1<T1, R> compose(Function<T1, T2> f1, Function<T2, T3> f2, Function<T3, T4> f3, Function<T4, R> f4) {
    return t1 -> f4.apply(f3.apply(f2.apply(f1.apply(t1))));
  }

  /**
   * 组合函数
   *
   * @param <T1> 函数1的输入类型，组合后的函数的输入类型
   * @param <R>  组合后的函数的输出类型
   * @return 组合后的函数
   */
  public static <T1, T2, T3, T4, T5, R> Function1<T1, R> compose(Function<T1, T2> f1, Function<T2, T3> f2, Function<T3, T4> f3, Function<T4, T5> f4, Function<T5, R> f5) {
    return t1 -> f5.apply(f4.apply(f3.apply(f2.apply(f1.apply(t1)))));
  }

  /**
   * 组合函数
   *
   * @param <T1> 函数1的输入类型，组合后的函数的输入类型
   * @param <R>  组合后的函数的输出类型
   * @return 组合后的函数
   */
  public static <T1, T2, T3, T4, T5, T6, R> Function1<T1, R> compose(Function<T1, T2> f1, Function<T2, T3> f2, Function<T3, T4> f3, Function<T4, T5> f4, Function<T5, T6> f5, Function<T6, R> f6) {
    return t1 -> f6.apply(f5.apply(f4.apply(f3.apply(f2.apply(f1.apply(t1))))));
  }

  /**
   * 组合函数
   *
   * @param <T1> 函数1的输入类型，组合后的函数的输入类型
   * @param <R>  组合后的函数的输出类型
   * @return 组合后的函数
   */
  public static <T1, T2, T3, T4, T5, T6, T7, R> Function1<T1, R> compose(Function<T1, T2> f1, Function<T2, T3> f2, Function<T3, T4> f3, Function<T4, T5> f4, Function<T5, T6> f5, Function<T6, T7> f6, Function<T7, R> f7) {
    return t1 -> f7.apply(f6.apply(f5.apply(f4.apply(f3.apply(f2.apply(f1.apply(t1)))))));
  }

  /**
   * 组合函数
   *
   * @param <T1> 函数1的输入类型，组合后的函数的输入类型
   * @param <R>  组合后的函数的输出类型
   * @return 组合后的函数
   */
  public static <T1, T2, T3, T4, T5, T6, T7, T8, R> Function1<T1, R> compose(Function<T1, T2> f1, Function<T2, T3> f2, Function<T3, T4> f3, Function<T4, T5> f4, Function<T5, T6> f5, Function<T6, T7> f6, Function<T7, T8> f7, Function<T8, R> f8) {
    return t1 -> f8.apply(f7.apply(f6.apply(f5.apply(f4.apply(f3.apply(f2.apply(f1.apply(t1))))))));
  }

  /**
   * 串联函（特殊的组合函数）
   *
   * @param linkup 连接器函数
   * @param fns    函数列表
   * @param <T>    函数的输入类型
   * @param <R>    组合后的函数的输出类型
   * @return 组合后的函数
   */
  @SafeVarargs
  public static <T, R> Function1<T, R> pipe(Function<R, T> linkup, Function<T, R>... fns) {
    if (fns == null || fns.length == 0) {
      throw new IllegalArgumentException("fns can not be empty");
    }
    Function1<T, R> chainedFn = fns[0]::apply;
    for (int i = 1; i < fns.length; i++) {
      chainedFn = compose(chainedFn, linkup, fns[i]);
    }
    return chainedFn;
  }

  /**
   * 串联函数（特殊的组合函数）
   *
   * @param fns 函数列表
   * @param <T> 函数的输入类型和输出类型
   * @return 组合后的函数
   */
  @SafeVarargs
  public static <T> Function1<T, T> pipe(Function<T, T>... fns) {
    Function1<T, T> chainedFn = fns[0]::apply;
    for (int i = 1; i < fns.length; i++) {
      chainedFn = compose(chainedFn, fns[i]);
    }
    return chainedFn;
  }

  /**
   * 序列执行，等价于 Clojure 中的 -> 宏
   */
  public static <T> Function0<T> through(T arg) {
    return () -> arg;
  }

  /**
   * 序列执行，等价于 Clojure 中的 -> 宏
   */
  public static <T, R> Function0<R> through(T arg, Function<T, R> fn) {
    return () -> fn.apply(arg);
  }

  /**
   * 序列执行，等价于 Clojure 中的 -> 宏
   */
  @SafeVarargs
  public static <T, R> Function0<R> through(T arg, Function<R, T> linkup, Function<T, R>... fns) {
    if (fns == null || fns.length == 0) {
      throw new IllegalArgumentException("fns can not be empty");
    }
    Function1<T, R> chainedFn = pipe(linkup, fns);
    return () -> chainedFn.apply(arg);
  }

  /**
   * 序列执行，等价于 Clojure 中的 -> 宏
   */
  @SafeVarargs
  public static <T> Function0<T> through(T arg, Function<T, T>... fns) {
    if (fns == null || fns.length == 0) {
      throw new IllegalArgumentException("fns can not be empty");
    }
    Function1<T, T> chainedFn = pipe(fns);
    return () -> chainedFn.apply(arg);
  }

  /**
   * 序列执行，等价于 Clojure 中的 -> 宏
   */
  public static <T1, T2, R> Function0<R> through(T1 arg, Function<T1, T2> fn1, Function<T2, R> fn2) {
    var composedFn = compose(fn1, fn2);
    return () -> composedFn.apply(arg);
  }

  /**
   * 序列执行，等价于 Clojure 中的 -> 宏
   */
  public static <T1, T2, T3, R> Function0<R> through(T1 arg, Function<T1, T2> fn1, Function<T2, T3> fn2, Function<T3, R> fn3) {
    var composedFn = compose(fn1, fn2, fn3);
    return () -> composedFn.apply(arg);
  }

  /**
   * 序列执行，等价于 Clojure 中的 -> 宏
   */
  public static <T1, T2, T3, T4, R> Function0<R> through(T1 arg, Function<T1, T2> fn1, Function<T2, T3> fn2, Function<T3, T4> fn3, Function<T4, R> fn4) {
    var composedFn = compose(fn1, fn2, fn3, fn4);
    return () -> composedFn.apply(arg);
  }

  /**
   * 序列执行，等价于 Clojure 中的 -> 宏
   */
  public static <T1, T2, T3, T4, T5, R> Function0<R> through(T1 arg, Function<T1, T2> fn1, Function<T2, T3> fn2, Function<T3, T4> fn3, Function<T4, T5> fn4, Function<T5, R> fn5) {
    var composedFn = compose(fn1, fn2, fn3, fn4, fn5);
    return () -> composedFn.apply(arg);
  }

  /**
   * 序列执行，等价于 Clojure 中的 -> 宏
   */
  public static <T1, T2, T3, T4, T5, T6, R> Function0<R> through(T1 arg, Function<T1, T2> fn1, Function<T2, T3> fn2, Function<T3, T4> fn3, Function<T4, T5> fn4, Function<T5, T6> fn5, Function<T6, R> fn6) {
    var composedFn = compose(fn1, fn2, fn3, fn4, fn5, fn6);
    return () -> composedFn.apply(arg);
  }

  /**
   * 序列执行，等价于 Clojure 中的 -> 宏
   */
  public static <T1, T2, T3, T4, T5, T6, T7, R> Function0<R> through(T1 arg, Function<T1, T2> fn1, Function<T2, T3> fn2, Function<T3, T4> fn3, Function<T4, T5> fn4, Function<T5, T6> fn5, Function<T6, T7> fn6, Function<T7, R> fn7) {
    var composedFn = compose(fn1, fn2, fn3, fn4, fn5, fn6, fn7);
    return () -> composedFn.apply(arg);
  }

  /**
   * 序列执行，等价于 Clojure 中的 -> 宏
   */
  public static <T1, T2, T3, T4, T5, T6, T7, T8, R> Function0<R> through(T1 arg, Function<T1, T2> fn1, Function<T2, T3> fn2, Function<T3, T4> fn3, Function<T4, T5> fn4, Function<T5, T6> fn5, Function<T6, T7> fn6, Function<T7, T8> fn7, Function<T8, R> fn8) {
    var composedFn = compose(fn1, fn2, fn3, fn4, fn5, fn6, fn7, fn8);
    return () -> composedFn.apply(arg);
  }

  /**
   * 偏函数（缩减参数数量）
   *
   * @param fn 原始函数
   * @return 偏函数
   */
  public static <T1, T2, R> Function1<T2, R> partial(BiFunction<T1, T2, R> fn, T1 t1) {
    return (T2 t2) -> fn.apply(t1, t2);
  }

  /**
   * 偏函数（缩减参数数量）
   *
   * @param fn 原始函数
   * @return 偏函数
   */
  public static <T1, T2, T3, R> Function1<T3, R> partial(Function3<T1, T2, T3, R> fn, T1 t1, T2 t2) {
    return (T3 t3) -> fn.apply(t1, t2, t3);
  }

  /**
   * 偏函数（缩减参数数量）
   *
   * @param fn 原始函数
   * @return 偏函数
   */
  public static <T1, T2, T3, R> Function2<T2, T3, R> partial(Function3<T1, T2, T3, R> fn, T1 t1) {
    return (T2 t2, T3 t3) -> fn.apply(t1, t2, t3);
  }

  /**
   * 偏函数（缩减参数数量）
   *
   * @param fn 原始函数
   * @return 偏函数
   */
  public static <T1, T2, T3, T4, R> Function1<T4, R> partial(Function4<T1, T2, T3, T4, R> fn, T1 t1, T2 t2, T3 t3) {
    return (T4 t4) -> fn.apply(t1, t2, t3, t4);
  }

  /**
   * 偏函数（缩减参数数量）
   *
   * @param fn 原始函数
   * @return 偏函数
   */
  public static <T1, T2, T3, T4, R> Function2<T3, T4, R> partial(Function4<T1, T2, T3, T4, R> fn, T1 t1, T2 t2) {
    return (T3 t3, T4 t4) -> fn.apply(t1, t2, t3, t4);
  }

  /**
   * 偏函数（缩减参数数量）
   *
   * @param fn 原始函数
   * @return 偏函数
   */
  public static <T1, T2, T3, T4, R> Function3<T2, T3, T4, R> partial(Function4<T1, T2, T3, T4, R> fn, T1 t1) {
    return (T2 t2, T3 t3, T4 t4) -> fn.apply(t1, t2, t3, t4);
  }

  /**
   * 偏函数，右起（缩减参数数量）
   *
   * @param fn 原始函数
   * @return 偏函数
   */
  public static <T1, T2, R> Function1<T1, R> partialRight(BiFunction<T1, T2, R> fn, T2 t2) {
    return (T1 t1) -> fn.apply(t1, t2);
  }

  /**
   * 偏函数，右起（缩减参数数量）
   *
   * @param fn 原始函数
   * @return 偏函数
   */
  public static <T1, T2, T3, R> Function1<T1, R> partialRight(Function3<T1, T2, T3, R> fn, T2 t2, T3 t3) {
    return (T1 t1) -> fn.apply(t1, t2, t3);
  }

  /**
   * 偏函数，右起（缩减参数数量）
   *
   * @param fn 原始函数
   * @return 偏函数
   */
  public static <T1, T2, T3, R> Function2<T1, T2, R> partialRight(Function3<T1, T2, T3, R> fn, T3 t3) {
    return (T1 t1, T2 t2) -> fn.apply(t1, t2, t3);
  }

  /**
   * 偏函数，右起（缩减参数数量）
   *
   * @param fn 原始函数
   * @return 偏函数
   */
  public static <T1, T2, T3, T4, R> Function1<T1, R> partialRight(Function4<T1, T2, T3, T4, R> fn, T2 t2, T3 t3, T4 t4) {
    return (T1 t1) -> fn.apply(t1, t2, t3, t4);
  }

  /**
   * 偏函数，右起（缩减参数数量）
   *
   * @param fn 原始函数
   * @return 偏函数
   */
  public static <T1, T2, T3, T4, R> Function2<T1, T2, R> partialRight(Function4<T1, T2, T3, T4, R> fn, T3 t3, T4 t4) {
    return (T1 t1, T2 t2) -> fn.apply(t1, t2, t3, t4);
  }

  /**
   * 偏函数，右起（缩减参数数量）
   *
   * @param fn 原始函数
   * @return 偏函数
   */
  public static <T1, T2, T3, T4, R> Function3<T1, T2, T3, R> partialRight(Function4<T1, T2, T3, T4, R> fn, T4 t4) {
    return (T1 t1, T2 t2, T3 t3) -> fn.apply(t1, t2, t3, t4);
  }

  /**
   * 转换成 vavr 扩展的函数
   */
  public static <R> Function0<R> f(Supplier<R> supplier) {
    return supplier::get;
  }

  /**
   * 转换成 vavr 扩展的函数
   */
  public static <T, R> Function1<T, R> f(Function<T, R> fn) {
    return fn::apply;
  }

  /**
   * 转换成 vavr 扩展的函数
   */
  public static <T1, T2, R> Function2<T1, T2, R> f(BiFunction<T1, T2, R> fn) {
    return fn::apply;
  }

  /**
   * 转换成 vavr 扩展的函数
   */
  public static <T> Predicate<T> p(Function<T, Boolean> fn) {
    return (T t) -> (boolean) fn.apply(t);
  }

  /**
   * 链式组合Predicate
   */
  public static <T1, T2> Predicate<T1> p(Function<T1, T2> fn1, Function<T2, Boolean> fn2) {
    var composedFn = compose(fn1, fn2);
    return t1 -> (boolean) composedFn.apply(t1);
  }

  /**
   * 链式组合Predicate
   */
  public static <T1, T2, T3> Predicate<T1> p(Function<T1, T2> fn1, Function<T2, T3> fn2, Function<T3, Boolean> fn3) {
    var composedFn = compose(fn1, fn2, fn3);
    return t1 -> (boolean) composedFn.apply(t1);
  }

  /**
   * 链式组合Predicate
   */
  public static <T1, T2, T3, T4> Predicate<T1> p(Function<T1, T2> fn1, Function<T2, T3> fn2, Function<T3, T4> fn3, Function<T4, Boolean> fn4) {
    var composedFn = compose(fn1, fn2, fn3, fn4);
    return t1 -> (boolean) composedFn.apply(t1);
  }

  /**
   * 链式组合Predicate
   */
  public static <T1, T2, T3, T4, T5> Predicate<T1> p(Function<T1, T2> fn1, Function<T2, T3> fn2, Function<T3, T4> fn3, Function<T4, T5> fn4, Function<T5, Boolean> fn5) {
    var composedFn = compose(fn1, fn2, fn3, fn4, fn5);
    return t1 -> (boolean) composedFn.apply(t1);
  }

  /**
   * 链式组合Predicate
   */
  public static <T1, T2, T3, T4, T5, T6> Predicate<T1> p(Function<T1, T2> fn1, Function<T2, T3> fn2, Function<T3, T4> fn3, Function<T4, T5> fn4, Function<T5, T6> fn5, Function<T6, Boolean> fn6) {
    var composedFn = compose(fn1, fn2, fn3, fn4, fn5, fn6);
    return t1 -> (boolean) composedFn.apply(t1);
  }

  /**
   * 链式组合Predicate
   */
  public static <T1, T2, T3, T4, T5, T6, T7> Predicate<T1> p(Function<T1, T2> fn1, Function<T2, T3> fn2, Function<T3, T4> fn3, Function<T4, T5> fn4, Function<T5, T6> fn5, Function<T6, T7> fn6, Function<T7, Boolean> fn7) {
    var composedFn = compose(fn1, fn2, fn3, fn4, fn5, fn6, fn7);
    return t1 -> (boolean) composedFn.apply(t1);
  }

  /**
   * 链式组合Predicate
   */
  public static <T1, T2, T3, T4, T5, T6, T7, T8> Predicate<T1> p(Function<T1, T2> fn1, Function<T2, T3> fn2, Function<T3, T4> fn3, Function<T4, T5> fn4, Function<T5, T6> fn5, Function<T6, T7> fn6, Function<T7, T8> fn7, Function<T8, Boolean> fn8) {
    var composedFn = compose(fn1, fn2, fn3, fn4, fn5, fn6, fn7, fn8);
    return t1 -> (boolean) composedFn.apply(t1);
  }

  /**
   * 参数Tuple化
   */
  public static <T, R> Function1<Tuple1<T>, R> tupled(Function<T, R> fn) {
    return t -> fn.apply(t._1);
  }

  /**
   * 参数Tuple化
   */
  public static <T1, T2, R> Function1<Tuple2<T1, T2>, R> tupled(BiFunction<T1, T2, R> fn) {
    return t -> fn.apply(t._1, t._2);
  }

  /**
   * 参数Tuple化
   */
  public static <T1, T2, T3, R> Function1<Tuple3<T1, T2, T3>, R> tupled(Function3<T1, T2, T3, R> fn) {
    return t -> fn.apply(t._1, t._2, t._3);
  }

  /**
   * 参数Tuple化
   */
  public static <T1, T2, T3, T4, R> Function1<Tuple4<T1, T2, T3, T4>, R> tupled(Function4<T1, T2, T3, T4, R> fn) {
    return t -> fn.apply(t._1, t._2, t._3, t._4);
  }

  /**
   * 参数Tuple化
   */
  public static <T1, T2, T3, T4, T5, R> Function1<Tuple5<T1, T2, T3, T4, T5>, R> tupled(Function5<T1, T2, T3, T4, T5, R> fn) {
    return t -> fn.apply(t._1, t._2, t._3, t._4, t._5);
  }

  /**
   * 参数Tuple化
   */
  public static <T1, T2, T3, T4, T5, T6, R> Function1<Tuple6<T1, T2, T3, T4, T5, T6>, R> tupled(Function6<T1, T2, T3, T4, T5, T6, R> fn) {
    return t -> fn.apply(t._1, t._2, t._3, t._4, t._5, t._6);
  }

  /**
   * 参数Tuple化
   */
  public static <T1, T2, T3, T4, T5, T6, T7, R> Function1<Tuple7<T1, T2, T3, T4, T5, T6, T7>, R> tupled(Function7<T1, T2, T3, T4, T5, T6, T7, R> fn) {
    return t -> fn.apply(t._1, t._2, t._3, t._4, t._5, t._6, t._7);
  }

  /**
   * 参数Tuple化
   */
  public static <T1, T2, T3, T4, T5, T6, T7, T8, R> Function1<Tuple8<T1, T2, T3, T4, T5, T6, T7, T8>, R> tupled(Function8<T1, T2, T3, T4, T5, T6, T7, T8, R> fn) {
    return t -> fn.apply(t._1, t._2, t._3, t._4, t._5, t._6, t._7, t._8);
  }

  /**
   * Tuple 参数展开
   */
  public static <T1, T2, R> Function2<T1, T2, R> untupled2(Function<Tuple2<T1, T2>, R> fn) {
    return (t1, t2) -> fn.apply(Tuple.of(t1, t2));
  }

  /**
   * Tuple 参数展开
   */
  public static <T1, T2, T3, R> Function3<T1, T2, T3, R> untupled3(Function<Tuple3<T1, T2, T3>, R> fn) {
    return (t1, t2, t3) -> fn.apply(Tuple.of(t1, t2, t3));
  }

  /**
   * Tuple 参数展开
   */
  public static <T1, T2, T3, T4, R> Function4<T1, T2, T3, T4, R> untupled4(Function<Tuple4<T1, T2, T3, T4>, R> fn) {
    return (t1, t2, t3, t4) -> fn.apply(Tuple.of(t1, t2, t3, t4));
  }

  /**
   * Tuple 参数展开
   */
  public static <T1, T2, T3, T4, T5, R> Function5<T1, T2, T3, T4, T5, R> untupled5(Function<Tuple5<T1, T2, T3, T4, T5>, R> fn) {
    return (t1, t2, t3, t4, t5) -> fn.apply(Tuple.of(t1, t2, t3, t4, t5));
  }

  /**
   * Tuple 参数展开
   */
  public static <T1, T2, T3, T4, T5, T6, R> Function6<T1, T2, T3, T4, T5, T6, R> untupled6(Function<Tuple6<T1, T2, T3, T4, T5, T6>, R> fn) {
    return (t1, t2, t3, t4, t5, t6) -> fn.apply(Tuple.of(t1, t2, t3, t4, t5, t6));
  }

  /**
   * Tuple 参数展开
   */
  public static <T1, T2, T3, T4, T5, T6, T7, R> Function7<T1, T2, T3, T4, T5, T6, T7, R> untupled7(Function<Tuple7<T1, T2, T3, T4, T5, T6, T7>, R> fn) {
    return (t1, t2, t3, t4, t5, t6, t7) -> fn.apply(Tuple.of(t1, t2, t3, t4, t5, t6, t7));
  }

  /**
   * Tuple 参数展开
   */
  public static <T1, T2, T3, T4, T5, T6, T7, T8, R> Function8<T1, T2, T3, T4, T5, T6, T7, T8, R> untupled8(Function<Tuple8<T1, T2, T3, T4, T5, T6, T7, T8>, R> fn) {
    return (t1, t2, t3, t4, t5, t6, t7, t8) -> fn.apply(Tuple.of(t1, t2, t3, t4, t5, t6, t7, t8));
  }

  /**
   * 参数 Entry 化
   */
  public static <T1, T2, R> Function1<Map.Entry<T1, T2>, R> toEntryMapper(BiFunction<T1, T2, R> fn) {
    return t -> fn.apply(t.getKey(), t.getValue());
  }

  /**
   * 连接两个Optional返回的函数
   */
  public static <T, R> Function1<T, Optional<R>> concat(Function<T, Optional<R>> fn1, Function<T, Optional<R>> fn2) {
    return t -> {
      var r1 = fn1.apply(t);
      if (r1.isPresent()) {
        return r1;
      }
      return fn2.apply(t);
    };
  }

  // 链式接口
  public interface Chain<T, R> {

    /**
     * 连接并返回下个节点
     *
     * @return 下一个链
     */
    <R2> Chain<T, R2> next(Function<R, R2> fn);

    /**
     * 以偏函数的方式连接并返回下个节点
     *
     * @param fn 下个节点的函数
     * @return 下一个链
     */
    default <T1, R2> Chain<T, R2> partial(BiFunction<T1, R, R2> fn, T1 t1) {
      return next(Functions.partial(fn, t1));
    }


    /**
     * 以偏函数的方式连接并返回下个节点
     *
     * @param fn 下个节点的函数
     * @return 下一个链
     */
    default <T1, T2, R2> Chain<T, R2> partial(Function3<T1, T2, R, R2> fn, T1 t1, T2 t2) {
      return next(Functions.partial(fn, t1, t2));
    }

    /**
     * 以偏函数（右）的方式连接并返回下个节点
     *
     * @param fn 下个节点的函数
     * @return 下个链
     */
    default <T2, R2> Chain<T, R2> partialRight(BiFunction<R, T2, R2> fn, T2 t2) {
      return next(Functions.partialRight(fn, t2));
    }

    /**
     * 以偏函数（右）的方式连接并返回下个节点
     *
     * @param fn 下个节点的函数
     * @return 下个链
     */
    default <T2, T3, R2> Chain<T, R2> partialRight(Function3<R, T2, T3, R2> fn, T2 t2, T3 t3) {
      return next(Functions.partialRight(fn, t2, t3));
    }

    /**
     * 结束链，返回函数
     *
     * @return 链式调用的函数
     */
    Function1<T, R> end();

    default Function0<R> end(T arg) {
      return () -> this.apply(arg);
    }

    /**
     * 调用函数链
     */
    default R apply(T t) {
      return end().apply(t);
    }
  }
}
