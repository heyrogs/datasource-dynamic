package com.jiang.aop;

import com.jiang.annotation.DBSelected;
import com.jiang.context.DynamicDataSourceContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.AfterReturningAdvice;
import org.springframework.aop.MethodBeforeAdvice;

import java.lang.reflect.Method;

/**
 * @author shijiang.luo
 * @description 定义环绕增强服务
 * @date 2020-09-13 13:10
 */
@Slf4j
public class TypeAroundAdvice implements MethodBeforeAdvice, AfterReturningAdvice {

    @Override
    public void afterReturning(Object returnValue, Method method, Object[] args, Object target)
            throws Throwable {
    }

    /**
     *
     * 对类和接口上的注解实现解析
     *
     * @param method
     * @param args
     * @param target
     * @throws Throwable
     */
    @Override
    public void before(Method method, Object[] args, Object target)
            throws Throwable {

        DBSelected annotation = target.getClass().getAnnotation(DBSelected.class);
        if(null == annotation){
            log.info("@DBSelect注解不能放在接口上 !");
            return;
        }
        log.info("当前数据源: {}", annotation.value());
        DynamicDataSourceContextHolder.setContextKey(annotation.value());
    }
}