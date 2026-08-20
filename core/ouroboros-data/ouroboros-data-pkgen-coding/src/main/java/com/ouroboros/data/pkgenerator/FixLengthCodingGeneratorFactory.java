package com.ouroboros.data.pkgenerator;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.beans.factory.ObjectProvider;

import io.vavr.Tuple;
import io.vavr.Tuple2;

import com.ouroboros.data.exception.PrimaryKeyGenerationException;
import com.ouroboros.data.expression.DataExpressionEvaluator;
import com.ouroboros.data.model.PrimaryKeyGenerator;
import com.ouroboros.data.model.PrimaryKeyGeneratorFactory;

/**
 * 编码生成器工厂<br>
 * 生成器命名规则：
 * coding:常量${表达式}{日期时间占位}<0001>
 * <ul>
 * <li>常量：没有加打括号的会作为常量处理，通常是前缀或者分段式编码的连接符</li>
 * <li>表达式：上下文为当前数据记录</li>
 * <li>日期占位符：DateTimeFormatter的格式字符串，注意要使用yyyy代表4位数的年份，日期使用dd而不是DD</li>
 * <li>序列器：<0001>代表4位数序列器，序列器名默认取其他部分；若使用固定序列器名使用：<序列器名:0001>；序列器名支持表达式：<${表达式}:0001>；序列器支持指定步长，格式：<0001,2></li>
 * </ul>
 */
public class FixLengthCodingGeneratorFactory {
  CodingSequencer codingSequencer;

  public FixLengthCodingGeneratorFactory(ObjectProvider<CodingSequencer> codingSequencerObjectProvider) {
    SingletonHolder.INSTANCE = this;
    try {
      codingSequencer = codingSequencerObjectProvider.getIfAvailable();
    } catch (NoUniqueBeanDefinitionException e) {
      codingSequencer = codingSequencerObjectProvider.orderedStream()
          .findFirst()
          .orElse(null);
    }
  }

  public Optional<CodingPrimaryKeyGenerator> get(String generatorName) {
    if (StringUtils.isEmpty(generatorName) || codingSequencer == null) {
      return Optional.empty();
    }
    var template = generatorName.startsWith("coding:") ? generatorName.substring(7) : generatorName;
    return Optional.of(buildGenerator(template));
  }

  private CodingPrimaryKeyGenerator buildGenerator(String codingTemplate) {
    final Pattern expressionPattern = Pattern.compile("(?!<)(\\$\\{[^}]+})");
    final Pattern sequencePattern = Pattern.compile("<(([^:]+):)?([0-9]+),?(\\d+)?>");
    final Pattern dateTimePattern = Pattern.compile("(?!\\$)\\{([^}]+)}");
    BiFunction<String, Map<String, Object>, String> expressionApplier;
    BiFunction<String, Map<String, Object>, String> sequenceNextApplier;
    BiFunction<String, Map<String, Object>, String> sequencePeekApplier;
    BiFunction<String, Map<String, Object>, String> dateTimeApplier;
    BiFunction<String, Map<String, Object>, String> sequenceNameGenerator;

    // 从 codingTemplate 中提取所有表达式
    var exprMatcher = expressionPattern.matcher(codingTemplate);
    final List<Tuple2<String, String>> expressions = new ArrayList<>();
    while (exprMatcher.find()) {
      expressions.add(Tuple.of(exprMatcher.group(), exprMatcher.group(1)));
    }
    expressionApplier = (template, context) -> {
      // TODO: 存在一定隐患，当序列化器定义在前面，并且序列化器的序列化名与表达式的占位符重合时，会导致序列化器的序列化名被表达式替换
      String result = template;
      for (var tuple : expressions) {
        var placeHolder = tuple._1;
        var expr = tuple._2;
        var expressionValue = DataExpressionEvaluator.eval(expr, context)
            .map(String::valueOf)
            .getOrElseThrow(e -> e instanceof RuntimeException re ? re : new RuntimeException(e));
        result = result.replace(placeHolder, expressionValue);
      }
      return result;
    };

    // 从 codingTemplate 中提取所有日期时间格式化字符串
    var dateTimeMatcher = dateTimePattern.matcher(codingTemplate);
    List<Tuple2<String, String>> dateTimeList = new ArrayList<>();
    while (dateTimeMatcher.find()) {
      dateTimeList.add(Tuple.of(dateTimeMatcher.group(), dateTimeMatcher.group(1)));
    }
    dateTimeApplier = (template, context) -> {
      String result = template;
      for (var tuple : dateTimeList) {
        var placeHolder = tuple._1;
        var format = tuple._2;
        var dateTimeValue = LocalDateTime.now().format(DateTimeFormatter.ofPattern(format));
        result = result.replace(placeHolder, dateTimeValue);
      }
      return result;
    };

    // 从 codingTemplate 中提取序列器名和长度
    var sequenceMatcher = sequencePattern.matcher(codingTemplate);
    // 如果没有序列器，则直接返回模板
    if (!sequenceMatcher.find()) {
      sequenceNextApplier = (template, context) -> template;
      sequencePeekApplier = (template, context) -> template;
    }
    // 否则提取序列器名和长度，并建立序列器（目前只支持单个序列器）
    else {
      String sequenceNameStr = sequenceMatcher.group(2);
      String sequenceStr = sequenceMatcher.group(3);
      int sequenceLength = sequenceStr.length();
      Long sequenceStart = Long.parseLong(sequenceStr);
      Long sequenceStep = Optional.ofNullable(sequenceMatcher.group(4)).map(Long::parseLong).orElse(1L);

      // 如果没有序列器名，则使用其他部分作为序列器名
      if (sequenceNameStr == null) {
        sequenceNameGenerator = (template, context) -> template.replace(sequenceMatcher.group(), "");
      } else {
        // 如果序列器名是表达式，则使用表达式解析器
        if (sequenceNameStr.startsWith("${") && sequenceNameStr.endsWith("}")) {
          sequenceNameGenerator = (template, context) -> DataExpressionEvaluator.eval(sequenceNameStr, context)
              .map(Object::toString)
              .getOrElseThrow(e -> e instanceof RuntimeException re ? re : new RuntimeException(e));
        }
        // 否则使用固定序列器名
        else {
          sequenceNameGenerator = (template, context) -> sequenceNameStr;
        }
      }

      sequenceNextApplier = (template, context) -> {
        String sequenceName = sequenceNameGenerator.apply(template, context);
        Long next = codingSequencer.next(sequenceName, sequenceStep, sequenceStart);
        return template.replace(sequenceMatcher.group(), String.format("%0" + sequenceLength + "d", next));
      };
      sequencePeekApplier = (template, context) -> {
        String sequenceName = sequenceNameGenerator.apply(template, context);
        Long next = codingSequencer.peek(sequenceName, sequenceStep, sequenceStart);
        return template.replace(sequenceMatcher.group(), String.format("%0" + sequenceLength + "d", next));
      };
    }

    return new CodingPrimaryKeyGenerator() {
      @Override
      public String peek(Map<String, Object> record) {
        try {
          String result = codingTemplate;
          result = expressionApplier.apply(result, record);
          result = dateTimeApplier.apply(result, record);
          result = sequencePeekApplier.apply(result, record);
          return result;
        } catch (Exception e) {
          throw new PrimaryKeyGenerationException("预生成主键失败，原因：" + e.getMessage(), codingTemplate, e);
        }
      }

      @Override
      public String next(Map<String, Object> record) {
        try {
          String result = codingTemplate;
          result = expressionApplier.apply(result, record);
          result = dateTimeApplier.apply(result, record);
          result = sequenceNextApplier.apply(result, record);
          return result;
        } catch (Exception e) {
          throw new PrimaryKeyGenerationException("生成主键失败，原因：" + e.getMessage(), codingTemplate, e);
        }
      }
    };
  }

  public static class SingletonHolder implements PrimaryKeyGeneratorFactory {
    private static FixLengthCodingGeneratorFactory INSTANCE;

    @Override
    public Optional<PrimaryKeyGenerator<?>> get(String generatorName) {
      if (StringUtils.isEmpty(generatorName) || !generatorName.startsWith("coding:")) {
        return Optional.empty();
      }
      var template = generatorName.substring(7);
      return Optional.ofNullable(INSTANCE)
          .flatMap(i -> i.get(template))
          .map(g -> (PrimaryKeyGenerator<?>) g);
    }
  }
}
