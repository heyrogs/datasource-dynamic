# 前言

很多场景我们都有使用多数据源的习惯， 然后都有些啥场景呢？ 

* 读写分离
* 主从数据库

## 1. 项目环境

### 1.1 项目环境

* JDK8
* SpringBoot 
* AOP
* Mybatis
* 数据库：MySQL

### 1.2 具体依赖

具体maven依赖如下:

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <dependency>
        <groupId>mysql</groupId>
        <artifactId>mysql-connector-java</artifactId>
        <scope>runtime</scope>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-configuration-processor</artifactId>
        <optional>true</optional>
    </dependency>
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
        <exclusions>
            <exclusion>
                <groupId>org.junit.vintage</groupId>
                <artifactId>junit-vintage-engine</artifactId>
            </exclusion>
        </exclusions>
    </dependency>

    <dependency>
        <groupId>org.mybatis.spring.boot</groupId>
        <artifactId>mybatis-spring-boot-starter</artifactId>
        <version>1.3.2</version>
    </dependency>

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-aop</artifactId>
    </dependency>
</dependencies>
```

### 1.3 包结构分析

- com.jiang
  - annotation - 自定义注解
  - aop - 切面处理，自定义注解具体处理逻辑
  - config - SpringBoot 自定义配置
  - constant - 常量
  - context - 动态数据定义（DynamicDataSource）和 动态数据源上下文切换（DynamicDataSourceContextHolder）
  - dao - DAO
  - entity - ENTITY
  - service - SERVICE

## 3. 具体实现 

### 3.1 YAML 配置

首先得配置自己所有的数据源信息，例如我定义的主从结构的数据源(master 和 slave), 具体定义如下:

```yaml
# DataSource
spring:
  datasource:
    master:
      jdbc-url: jdbc:mysql://myserver:8505/db01?useSSL=false&serverTimezone=GMT%2B8&characterEncoding=UTF-8
      username: root
      password: root
      driver-class-name: com.mysql.cj.jdbc.Driver
    slave:
      jdbc-url: jdbc:mysql://myserver:8505/db02?useSSL=false&serverTimezone=GMT%2B8&characterEncoding=UTF-8
      username: root
      password: root
      driver-class-name: com.mysql.cj.jdbc.Driver
```

### 3.2 数据源配置

这里使用SpringBoot里最简单的配置， 如果想配置 Druid 数据源，可以按常规配置即可。

DynamicDataSourceConfig.class

```java
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
}
```

描述: 

* @Configuration: 声明当前类是配置类
* @MapperScan: Mybatis 对应的 mapper 接口扫描配置
* @EnableAutoConfiguration: 自动装配定义，这里使用 exclude 作用是取消数据源的自动装配，以实现下面的自定义配置
* @ConfigurationProperties(prefix = "spring.datasource.master": 从yaml中获取前缀为 spring.datasource.master的配置信息

### 3.3 自定义动态数据源

需要继承 AbstractRoutingDataSource 类，使用其路由策略来实现对当前数据源的获取

```java
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
```

### 3.4 定义动态数据源上下文

因为 SpringBoot 是使用单线程实现接口访问，所以这里可以使用 ThreadLocal 解决线程问题

DynamicDataSourceContextHolder.java

```java
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
```

### 3.5 在配置类配置动态数据源

定义好动态数据源后，需要在配置类配置动态数据源来指定当前线程使用的数据源

只需要添加如下方法即可:

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


这里使用 @Primary 来指定优先注入 dynamicDataSource()， 保证了数据源可以实现动态切换。

完整的配置类信息为：

```java
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
```

> 注意： 这里设置设置目标数据源使用的是 Map 结构。

### 3.6 测试数据源是否成功切换

到这里多数据基本已经配置成功， 接下来需要创建一个 DAO 来测试一下.

代码如下:

```java
package com.jiang.dao;

import com.jiang.entity.dto.UserDTO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author shijiang.luo
 * @description
 * @date 2020-09-10 22:48
 */
@Repository
public interface UserDao {

    @Insert("insert into user(name, age) value (#{name}, #{age})")
    Integer insertUser(UserDTO user);

    @Select("select * from user")
    List<UserDTO> findList();
}

```

编写测试方法:

```java
package com.jiang;

import com.jiang.context.DynamicDataSourceContextHolder;
import com.jiang.dao.UserDao;
import com.jiang.entity.dto.UserDTO;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.Assert;

import java.util.List;

import static com.jiang.constant.DynamicDataSourceConstant.SLAVE1;

@Slf4j
@SpringBootTest
class DynamicDatasourceApplicationTests {

    @Autowired
    UserDao userDao;

    @Test
    void slave(){
        DynamicDataSourceContextHolder.setContextKey(SLAVE1);
        UserDTO userDTO = new UserDTO("ceshi", 20);
        Assert.isTrue(userDao.insertUser(userDTO) > 0, "insert sucess");
    }

}
```

这里使用代码 `DynamicDataSourceContextHolder.setContextKey(SLAVE1);` 来实现数据源切换。

## 4. 使用 AOP 解决多次手动选择数据源问题

AOP 就是基于 JDK动态代理 和 cglib 实现的， 若类有实现接口则使用 JDK， 没有则使用 cglib。

点击 [个人jdk动态代理 和 cglib 学习日记](https://github.com/luoshijiang/datasource-dynamic/blob/master/HELP_PROXY.md) 进入了解。

### 4.1 自定义一个注解，以注解的方式显示数据源切换

自定义一个数据源选择注解：DBSelected

```java
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
```

* @Retention(RetentionPolicy.RUNTIME)： 表示该注解在运行时使用
* @Target({ElementType.METHOD, ElementType.TYPE})： 表示该注解作用范围是：方法、类和接口上。

### 4.2 编写 AOP 实现注解的解析

使用 AOP 先拦截我自定义的注解，然后对注解所作用的方法进行切面处理。具体逻辑为:

1. 先获取注解上的 value().
2. 使用 `DynamicDataSourceContextHolder.setContextKey(dynamicKey);` 实现动态数据源的切换。

```java
package com.jiang.aop;

import com.jiang.annotation.DBSelected;
import com.jiang.context.DynamicDataSourceContextHolder;
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
@Aspect
@Component
public class DynamicDataSourceAop {

    @Pointcut("@annotation(com.jiang.annotation.DBSelected)")
    public void dataSourcePointCut(){}

    @Around("dataSourcePointCut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable{
        String dynamicKey = getAnnotation(joinPoint).value();
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
``` 

编写完成，就可以编写 Service 来测试注解是否有效了。

### 4.3 编写具体的 Service 

这里使用两个注解，分别是使用默认值和选择 SLAVE 数据源的注解。

```java

package com.jiang.service.impl;

import com.jiang.annotation.DBSelected;
import com.jiang.dao.UserDao;
import com.jiang.entity.dto.UserDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import static com.jiang.constant.DynamicDataSourceConstant.SLAVE1;

/**
 * @author shijiang.luo
 * @description
 * @date 2020-09-10 23:44
 */
@Service
public class UserServiceImpl {

    @Autowired
    private UserDao userDao;

    /**
     *
     * 通过 master 数据库获取数据
     *
     * @return
     */
    @DBSelected
    public List<UserDTO> getListByMaster(){
        return userDao.findList();
    }

    /**
     *
     * 通过 slave 数据库获取数据
     *
     * @return
     */
    @DBSelected(SLAVE1)
    public List<UserDTO> getLlistBySlave(){
        return userDao.findList();
    }

}

```

### 4.4 编写测试方法

```java
package com.jiang;

import com.jiang.dao.UserDao;
import com.jiang.entity.dto.UserDTO;
import com.jiang.service.impl.UserServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.Assert;

import java.util.List;

@Slf4j
@SpringBootTest
class DynamicDatasourceApplicationTests {

    @Autowired
    UserServiceImpl userService;

    @Test
    void contextLoads() {
        List<UserDTO> userList = userService.getListByMaster();
        log.info("master: {}", userList);
        Assert.notEmpty(userList, "users is empty !");
        userList = userService.getLlistBySlave();
        log.info("slave: {}", userList);
        Assert.notEmpty(userList, "user is empty");
    }

}
```

### 4.5 具体测试结果

```text
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::        (v2.3.3.RELEASE)

2020-09-13 01:25:17.770  INFO 35140 --- [           main] c.j.DynamicDatasourceApplicationTests    : Starting DynamicDatasourceApplicationTests on Server with PID 35140 (started by shijiang in /Users/shijiang/programmer/develop/project/datasource-dynamic)
2020-09-13 01:25:17.773  INFO 35140 --- [           main] c.j.DynamicDatasourceApplicationTests    : No active profile set, falling back to default profiles: default
2020-09-13 01:25:22.202  INFO 35140 --- [           main] o.s.s.concurrent.ThreadPoolTaskExecutor  : Initializing ExecutorService 'applicationTaskExecutor'
2020-09-13 01:25:23.733  INFO 35140 --- [           main] c.j.DynamicDatasourceApplicationTests    : Started DynamicDatasourceApplicationTests in 6.944 seconds (JVM running for 10.764)
2020-09-13 01:25:24.630  INFO 35140 --- [           main] com.zaxxer.hikari.HikariDataSource       : HikariPool-1 - Starting...
2020-09-13 01:25:25.849  INFO 35140 --- [           main] com.zaxxer.hikari.HikariDataSource       : HikariPool-1 - Start completed.
2020-09-13 01:25:25.985  INFO 35140 --- [           main] c.j.DynamicDatasourceApplicationTests    : master: [{id=1, name='master', age=20}]
2020-09-13 01:25:25.985  INFO 35140 --- [           main] com.zaxxer.hikari.HikariDataSource       : HikariPool-2 - Starting...
2020-09-13 01:25:26.749  INFO 35140 --- [           main] com.zaxxer.hikari.HikariDataSource       : HikariPool-2 - Start completed.
2020-09-13 01:25:26.809  INFO 35140 --- [           main] c.j.DynamicDatasourceApplicationTests    : slave: [{id=1, name='slave', age=20}, {id=2, name='ceshi', age=20}]
2020-09-13 01:25:26.847  INFO 35140 --- [extShutdownHook] o.s.s.concurrent.ThreadPoolTaskExecutor  : Shutting down ExecutorService 'applicationTaskExecutor'
2020-09-13 01:25:26.848  INFO 35140 --- [extShutdownHook] com.zaxxer.hikari.HikariDataSource       : HikariPool-2 - Shutdown initiated...
2020-09-13 01:25:26.869  INFO 35140 --- [extShutdownHook] com.zaxxer.hikari.HikariDataSource       : HikariPool-2 - Shutdown completed.
2020-09-13 01:25:26.870  INFO 35140 --- [extShutdownHook] com.zaxxer.hikari.HikariDataSource       : HikariPool-1 - Shutdown initiated...
2020-09-13 01:25:27.201  INFO 35140 --- [extShutdownHook] com.zaxxer.hikari.HikariDataSource       : HikariPool-1 - Shutdown completed.
```

## 5 解决类上不能解析注解问题

使用 AnnotationMatchingPointcut 和 DefaultPointcutAdvisor 修补 aspect 不能拦截类上加注释问题。

具体实现如下:

```java
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
```

实现接口 MethodBeforeAdvice, AfterReturningAdvice 来自定义 TypeAroundAdvice 

```java
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
            log.info("注解不能放在接口上 !");
            return;
        }
        log.info("当前数据源: {}", annotation.value());
        DynamicDataSourceContextHolder.setContextKey(annotation.value());
    }
}
```

定义好之后，需要在类上加注释，然后在测试，这里将不再贴出具体测试结果。


## 6 遗留问题

接口上不能存放注解，目前我所了解应该是接口不能被代理成一个对象的问题。