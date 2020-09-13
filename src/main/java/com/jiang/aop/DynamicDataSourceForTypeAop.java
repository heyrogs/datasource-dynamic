package com.jiang.aop;

import com.jiang.annotation.DBSelected;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.aop.Advice;
import org.springframework.aop.Advisor;
import org.springframework.aop.Pointcut;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.aop.support.annotation.AnnotationMatchingPointcut;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

/**
 * @author shijiang.luo
 * @description
 * @date 2020-09-13 13:01
 */
@Slf4j
@Component
public class DynamicDataSourceForTypeAop {

    @Bean
    public Advisor dataSourceAdvisor(){
        Pointcut pointcut = new AnnotationMatchingPointcut(DBSelected.class, true);
        Advice advice = new TypeAroundAdvice();
        return new DefaultPointcutAdvisor(pointcut, advice);
    }
}