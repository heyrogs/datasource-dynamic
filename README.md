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

@Configuration: 声明当前类是配置类
@MapperScan: Mybatis 对应的 mapper 接口扫描配置
@EnableAutoConfiguration: 自动装配定义，这里使用 exclude 作用是取消数据源的自动装配，以实现下面的自定义配置
@ConfigurationProperties(prefix = "spring.datasource.master": 从yaml中获取前缀为 spring.datasource.master的配置信息

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


