package com.jiang.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static com.jiang.constant.DynamicDataSourceConstant.MASTER;

/**
 * @author shijiang.luo
 * @description 数据源选择注释
 * @date 2020-09-10 23:19
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface DBSelected {

    /**
     *
     * 数据源名称
     *
     * @return
     */
    String value() default MASTER;

}
