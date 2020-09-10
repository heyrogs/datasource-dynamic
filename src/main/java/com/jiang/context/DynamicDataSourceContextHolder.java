package com.jiang.context;

import java.util.Optional;

import static com.jiang.constant.DynamicDataSourceConstant.MASTER;

/**
 * @author shijiang.luo
 * @description 动态获取数据源
 * @date 2020-09-10 22:33
 */
public class DynamicDataSourceContextHolder {

    private static final ThreadLocal<String> DATASOURCE_KEY_CONTEXT_HOLDER = new ThreadLocal();

    /**
     *
     * 设置或切换数据源
     *
     * @param key
     */
    public static void setContextKey(String key){
        DATASOURCE_KEY_CONTEXT_HOLDER.set(key);
    }

    /**
     *
     * 获取数据源名称
     *
     * @return
     */
    public static String getContextKey(){

        return Optional.ofNullable(DATASOURCE_KEY_CONTEXT_HOLDER.get()).orElse(MASTER);
    }

    /**
     *
     * 删除当前数据源名称
     *
     */
    public static void removeContextKey(){
        DATASOURCE_KEY_CONTEXT_HOLDER.remove();
    }


}