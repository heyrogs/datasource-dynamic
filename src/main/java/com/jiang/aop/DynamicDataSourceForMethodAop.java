package com.jiang.aop;

import com.jiang.annotation.DBSelected;
import com.jiang.context.DynamicDataSourceContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * @author shijiang.luo
 * @description 定义数据源切面
 * @date 2020-09-10 23:23
 */
@Slf4j
@Aspect
@Component
public class DynamicDataSourceForMethodAop {

    @Pointcut("@annotation(com.jiang.annotation.DBSelected)")
    public void dataSourcePointCut(){}

    @Around("dataSourcePointCut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable{
        String dynamicKey = getAnnotation(joinPoint).value();
        log.info("当前数据源: {}", dynamicKey);
        DynamicDataSourceContextHolder.setContextKey(dynamicKey);
        try {
            return joinPoint.proceed();
        }finally {
            DynamicDataSourceContextHolder.removeContextKey();
        }
    }

    /**
     *
     * 根据类或方法获取注解
     *
     * @param joinPoint
     * @return
     */
    private DBSelected getAnnotation(ProceedingJoinPoint joinPoint){
        Class<?> clazz = joinPoint.getTarget().getClass();
        MethodSignature methodSignature = (MethodSignature)joinPoint.getSignature();
        return Optional.ofNullable(clazz.getAnnotation(DBSelected.class))
                .orElse(methodSignature.getMethod().getAnnotation(DBSelected.class));
    }

}