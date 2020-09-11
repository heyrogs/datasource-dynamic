# 动态代理笔记

## 1. 前言

现在使用 SpringBoot 的 aspect 实现动态数据源的切换， 这里出现一个问题：aspect 不能拦截到 Class 和 Inteface 上的注解， 所以想要了解下
动态代理中有没有好的解决方案。 首先动态代理涉及到的场景一般如下：

* 动态数据源切换
* 方法调用时长统计
* 在已有方法前后添加新的逻辑
* 统一对方法做事务处理
* 方法前后日志统一输出处理
* 对某类函数抛出的异常做统一处理
* 对函数访问的权限校验

动态代理首先是代理， 代理有：静态代理 和 动态代理。

## 2. 代理模式 及 静态代理

### 2.1 代理模式说明

代理模式， 及调用者不需要跟实际的对象接触， 只跟代理打交道。 典型的案例就是生活中租房时你只需要和中介接触， 而不用和房东打交道是一个道理。

更具体点， 看下图：

![代理模式图](http://assets.processon.com/chart_image/5f5b8a24e0b34d6f59ef17f0.png)

说明： 这里接口是 ISubject ， 实现类是 RealSubject 和 Proxy， 客户端 client 调用了通过接口调用 operator() 方法，由 Proxy 代理调用
RealSubject 的 operator(). 

与生活案例对应， 这里 client 就是我们， operator() 就是要租房这个需求， 而 Proxy 就是中介， 最后 RealSubject 就是房东。


### 2.2 静态代理

按照上面的代理描述， 这里在代码编译阶段就决定 Proxy 而不是运行阶段决定， 这是静态代理。 

代码实现：

先定义一个具体的操作接口 

```java
package com.jiang.proxy;

/**
 * @author shijiang.luo
 * @description
 * @date 2020-09-11 22:57
 */
public interface SubjectService {

    void operator();

}
```

然后分别定义 Proxy 和 RealSubject 实现类

RealSubject:
```java
package com.jiang.proxy;

/**
 * @author shijiang.luo
 * @description
 * @date 2020-09-11 22:58
 */
public class RealSubjectServiceImpl implements SubjectService {

    @Override
    public void operator() {
        System.out.println("这里是房东。");
    }
}
```

Proxy - 
这里处理代理之外还实现了对 operator() 调用时长的统计

```java
package com.jiang.proxy;

import org.springframework.util.StopWatch;

/**
 * @author shijiang.luo
 * @description
 * @date 2020-09-11 22:58
 */
public class SubjectProxy implements SubjectService {

    private RealSubjectServiceImpl realSubjectService;

    public SubjectProxy(RealSubjectServiceImpl realSubjectService){
        this.realSubjectService = realSubjectService;
    }

    /**
     *
     * 对 operator() 调用记时
     *
     */
    @Override
    public void operator() {

        StopWatch stopWatch = new StopWatch();

        stopWatch.start("使用时间");

        realSubjectService.operator();

        stopWatch.stop();

        System.out.println(stopWatch.prettyPrint());
    }

}
```

最调用在 Client 实现

```java
package com.jiang.proxy;

/**
 * @author shijiang.luo
 * @description
 * @date 2020-09-11 23:07
 */
public class Client {

    /**
     *
     * 客户端代码调用
     *
     * @param args
     */
    public static void main(String[] args) {

        SubjectService subjectService = new SubjectProxy(new RealSubjectServiceImpl());

        subjectService.operator();
    }

}
```

贴出的实现结果：
```
这里是房东。
StopWatch '': running time = 313370 ns
---------------------------------------------
ns         %     Task name
---------------------------------------------
000313370  100%  使用时间
```

### 2.3 静态代理的局限性

静态代理可以简单实现函数调用时长和在函数前后打印日志等简单需求。 但是其也存在局限性， 试想一下：如果我的 SubjectService 有100个方法， 我要
使用代理模式对这100个方法实现时长调用统计，那么我要实现必须要实现100个接口， 然后在 Proxy 中声明100个 StopWatch 对象， 那么代码看起来将会
非常臃肿， 我们需要进一步优化， 如何只需要一个公共函数来实现代理？ 那么这时就可以使用动态代理了。


## 3. 动态代理



