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

![代理模式图](./main/resources/static/代理模式.png)

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

动态代理的关键 - 反射

### 3.1 java 的反射机制

Java 反射机制就是代码在运行状态中， 可以动态地获取类的属性和方法， 也可以操作这个类对象的方法和属性， 这种功能就叫反射。

使用反射可以动态生成类对象， 而不用像日常一样（静态）new 对象生成。 也可以动态地去执行对象的方法。

案例：

先定义某个类和它的方法

```java
package com.jiang.reflect;

/**
 * @author shijiang.luo
 * @description
 * @date 2020-09-12 00:03
 */
public class ReflectService {

    public void doSomething(){
        System.out.println("reflect service...");
    }

}
```

通过反射创建对象和调用方法
```java
package com.jiang.reflect;

import java.lang.reflect.Method;

/**
 * @author shijiang.luo
 * @description
 * @date 2020-09-12 00:05
 */
public class ReflectRunner {

    /**
     *
     * 使用反射动态生成 ReflectService 对象， 并且使用 invoke 执行 doSomething 方法
     *
     * @param args
     */
    public static void main(String[] args) throws Exception{
        // 加载类
        Class<?> clazz = Class.forName("com.jiang.reflect.ReflectService");
        // 生成类对象
        Object classObject = clazz.getConstructor().newInstance();
        // 调用类对象
        Method method = clazz.getMethod("doSomething");
        method.invoke(classObject);
    }

}
```

从代码上可知， 只要需要知道类路径和方法名就可以执行方法了。

### 3.2 JDK 动态代理

JDK 默认提供了动态代理的实现

实现步骤：

1. 实现接口 InvocationHandler , 重写方法 invoke()；

2. 使用 java.lang.reflect.Proxy 类；

```java
package com.jiang.proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * @author shijiang.luo
 * @description JDK 提供动态代理
 * 具体实现步骤：
 * 1. 实现 InvocationHandler, 由他来实现 invoke 方法， 执行代理函数
 * 2. 使用 Proxy 类
 * @date 2020-09-12 00:27
 */
public class JdkProxyHandler implements InvocationHandler {

    private Object targetObject;

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        System.out.println("jdk dynamic proxy start ");
        Object result = method.invoke(targetObject, args);
        System.out.println("jdk dynamic proxy ending ");
        return result;
    }

    /**
    * 使用 Proxy 类
    * @param targetObject
    * @return 
    */
    public Object createProxy(Object targetObject){
        this.targetObject = targetObject;
        return Proxy.newProxyInstance(targetObject.getClass().getClassLoader()
                ,targetObject.getClass().getInterfaces(), this);
    }
}
```

在前面代理模式的代码上，实现代理， 具体调用类如下：

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

        JdkProxyHandler jdkProxyHandler = new JdkProxyHandler();
        subjectService = (SubjectService)jdkProxyHandler.createProxy(new RealSubjectServiceImpl());
        subjectService.operator();
    }

}
```

JDK 提供的代理有个明显的缺点： 必须是实现了接口的类才能代理， 未能实现接口的类不能被代理到。 那么这里要实现单个类代理的话， 必须得使用上 cglib，


### 3.3 cglib 动态代理

cglib： 能通过继承来实现对象代理。

实现步骤：

1. 实现 MethodInterceptor 接口， 重写 intercept() 方法；

2. 使用 Enhancer 设置委托类并将 interceptor 作为回调使用；

代码案例:

```java
package com.jiang.cglib;

import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;

import java.lang.reflect.Method;

/**
 *
 * @author shijiang.luo
 * @description: cglib实现动态代理
 * CGLIB：可以解决JDK代理的弊端， 即只能代理实现了接口的类， 不能代理未实现接口的类
 * @date 2020-09-12 20:26
 *
 */
public class CglibProxyInterceptor implements MethodInterceptor {

    @Override
    public Object intercept(Object o, Method method, Object[] args, MethodProxy methodProxy)
            throws Throwable {
        System.out.println("=====>>>>> 开始代理类");
        Object result = methodProxy.invokeSuper(o, args);
        System.out.println("=====>>>>> 结束代理类");
        return result;
    }

                              //    \\
                             // .创. \\
                            //  .建.  \\
                           //   .代.   \\
                          //    .理.    \\
                         //     ...      \\
                        //    |=|=|=|     \\

    /**
     *
     * 这里通过 Enhancer 设置委托类为父类 （setsuperclass）
     * 并把 intercept 方法作为回调函数
     * @param clazz
     * @param <T>
     * @return
     */
    public static <T> T createProxy(Class<T> clazz){
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(clazz);
        enhancer.setCallback(new CglibProxyInterceptor());
        return (T) enhancer.create();
    }

}
```

创建一个单独的类， 用来展示 cglib 的功能

```java
package com.jiang.cglib;

/**
 * @author shijiang.luo
 * @description
 * @date 2020-09-12 22:56
 */
public class CglibService {

    public void doSomething(){
        System.out.println("牛批。。。。。");
    }

}
```

最终客户端调用实现

```java
package com.jiang.cglib;

/**
 * @author shijiang.luo
 * @description
 * @date 2020-09-12 22:57
 */
public class CglibClient {

    public static void main(String[] args) {
        CglibService cglibService = CglibProxyInterceptor.createProxy(CglibService.class);
        cglibService.doSomething();
    }
}
```


