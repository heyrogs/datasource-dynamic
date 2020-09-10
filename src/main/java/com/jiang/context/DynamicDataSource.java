package com.jiang.context;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

/**
 * @author shijiang.luo
 * @description 动态数据源类
 * @date 2020-09-10 22:23
 */
public class DynamicDataSource extends AbstractRoutingDataSource {

    /**
     *
     * 路由策略
     *
     * @return
     */
    @Override
    protected Object determineCurrentLookupKey() {

        return DynamicDataSourceContextHolder.getContextKey();
    }
}