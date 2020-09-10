package com.jiang.config;

import com.jiang.context.DynamicDataSource;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

import static com.jiang.constant.DynamicDataSourceConstant.MASTER;
import static com.jiang.constant.DynamicDataSourceConstant.SLAVE1;


/**
 * @author shijiang.luo
 * @description 动态数据源配置
 * @date 2020-09-10 21:58
 */
@Configuration
@MapperScan(basePackages = "com.jiang.dao")
@EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class})
public class DynamicDataSourceConfig {

    @ConfigurationProperties(prefix = "spring.datasource.master")
    @Bean(MASTER)
    public DataSource masterDataSource() {
        return DataSourceBuilder.create().build();
    }

    @ConfigurationProperties(prefix = "spring.datasource.slave")
    @Bean(SLAVE1)
    public DataSource slave1DataSource() {
        return DataSourceBuilder.create().build();
    }

    /**
     * `@Primary` 该注释作用是优先从该数据源获取
     *
     * @return
     */
    @Primary
    @Bean
    public DataSource dynamicDataSource() {
        Map<Object, Object> targetDataSource = new HashMap();
        targetDataSource.put(MASTER, masterDataSource());
        targetDataSource.put(SLAVE1, slave1DataSource());
        // 设置动态数据源
        DynamicDataSource dynamicDataSource = new DynamicDataSource();
        dynamicDataSource.setTargetDataSources(targetDataSource);
        dynamicDataSource.setDefaultTargetDataSource(masterDataSource());
        return dynamicDataSource;
    }

}