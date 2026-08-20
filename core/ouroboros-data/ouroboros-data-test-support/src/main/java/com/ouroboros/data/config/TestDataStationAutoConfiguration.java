package com.ouroboros.data.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.ouroboros.data.model.DataModelCenter;
import com.ouroboros.data.model.MigrationStrategy;
import com.ouroboros.data.model.TestModels;
import com.ouroboros.data.station.DataStation;
import com.ouroboros.data.station.DataStationProvider;
import com.ouroboros.data.station.SqlDataStation;
import com.ouroboros.data.station.StaticDataStationProvider;

/**
 * 测试 DataStation 自动配置
 * <p>
 * 自动创建基于 H2 的 SqlDataStation，并注册 TestModels 定义的模型。
 *
 * @since 1.0.0-beta.2
 */
@Configuration
@ConditionalOnClass(SqlDataStation.class)
public class TestDataStationAutoConfiguration {

  public static final String STATION_NAME = "test";

  /**
   * 创建测试用 DataStation
   * <p>
   * 使用 MigrationStrategy.AUTO 自动创建表结构。
   *
   * @param dataSource Spring 自动配置的数据源
   * @return SqlDataStation 实例
   */
  @Bean
  @ConditionalOnMissingBean(name = "testDataStation")
  public DataStation<?> testDataStation(DataSource dataSource) {
    SqlDataStation station = new SqlDataStation(
        STATION_NAME,
        dataSource,
        TestModels.allMetas(),
        MigrationStrategy.AUTO
    );
    // 执行迁移，创建表结构
    station.migrate();
    return station;
  }

  /**
   * 注册 DataStation 到 StaticDataStationProvider
   * <p>
   * 同时刷新 DataModelCenter，确保 populate 功能可以正确解析关联模型。
   *
   * @param testDataStation 测试 DataStation
   * @return DataStationProvider 实例
   */
  @Bean
  @ConditionalOnMissingBean(DataStationProvider.class)
  public DataStationProvider testDataStationProvider(@Qualifier("testDataStation") DataStation<?> testDataStation) {
    StaticDataStationProvider.register(STATION_NAME, testDataStation);
    // 刷新 DataModelCenter，使 RelatedValue.getReferenceModel() 能正确解析关联模型
    DataModelCenter.refresh();
    return new StaticDataStationProvider();
  }
}
