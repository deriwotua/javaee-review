[TOC]

## SpringIOC

[SpringIOC](../spring-ioc/README.md)

## spring注解

[spring annotation](../spring-annotation/README.md)

### 1.Spring 的 AOP 简介

#### 1.1 什么是 AOP 

`AOP` 为 Aspect Oriented Programming 的缩写，意思为面向切面编程，是**通过预编译方式和运行期动态代理**实现程序功能的统一维护的一种技术。

`AOP` 是 OOP 的延续，是软件开发中的一个热点，也是Spring框架中的一个重要内容，是`函数式编程`的一种衍生范型。利用`AOP`可以对业务逻辑的各个部分进行隔离，从而使得业务逻辑各部分之间的耦合度降低，提高程序的可重用性，同时提高了开发的效率。

#### 1.2 AOP 的作用及其优势

作用：在程序运行期间，在不修改源码的情况下对方法进行功能增强

优势：减少重复代码，提高开发效率，并且便于维护

#### 1.3 AOP 的底层实现

实际上，AOP 的底层是通过 Spring 提供的的`动态代理技术`实现的。在运行期间，Spring通过动态代理技术动态的生成代理对象，代理对象方法执行时进行增强功能的介入，在去调用目标对象的方法，从而完成功能的增强。

#### 1.4 AOP 的动态代理技术

常用的动态代理技术

JDK 代理 : `基于接口的动态代理技术`

cglib 代理：`基于父类的动态代理技术`

![](assets/图片1.png)

#### 1.5 JDK 的动态代理

①目标类接口

 ```java
package tk.deriwotua.proxy.jdk;

public interface TargetInterface {

    public void save();

}
 ```

②目标类

```java
package tk.deriwotua.proxy.jdk;

public class Target implements TargetInterface {
    public void save() {
        System.out.println("save running.....");
    }
}
```

③动态代理代码

```java
package tk.deriwotua.proxy.jdk;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class Advice {

    public void before(){
        System.out.println("前置增强....");
    }

    public void afterReturning(){
        System.out.println("后置增强....");
    }

}

public class ProxyTest {

    public static void main(String[] args) {
        //目标对象
        final Target target = new Target();
        //增强对象
        final Advice advice = new Advice();
        //返回值 就是动态生成的代理对象
        TargetInterface proxy = (TargetInterface) Proxy.newProxyInstance(
                target.getClass().getClassLoader(), //目标对象类加载器
                target.getClass().getInterfaces(), //目标对象相同的接口字节码对象数组
                new InvocationHandler() {
                    //调用代理对象的任何方法  实质执行的都是invoke方法
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        advice.before(); //前置增强
                        Object invoke = method.invoke(target, args);//执行目标方法
                        advice.afterReturning(); //后置增强
                        return invoke;
                    }
                }
        );

        //调用代理对象的方法
        proxy.save();
    }
}
```

④  调用代理对象的方法测试

```java
// 测试,当调用接口的任何方法时，代理对象的代码都无序修改
proxy.save();
```

![](assets/图片2.png)

#### 1.6 cglib 的动态代理

①目标类

```java
package tk.deriwotua.proxy.cglib;

public class Target {
    public void save() {
        System.out.println("save running.....");
    }
}
```

②动态代理代码

```java
package tk.deriwotua.proxy.cglib;

import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;

import java.lang.reflect.Method;

public class Advice {
    public void before(){
        System.out.println("前置增强....");
    }

    public void afterReturning(){
        System.out.println("后置增强....");
    }
}

public class ProxyTest {

    public static void main(String[] args) {

        //目标对象
        final Target target = new Target();
        //增强对象
        final Advice advice = new Advice();
        //返回值 就是动态生成的代理对象  基于cglib
        //1、创建增强器
        Enhancer enhancer = new Enhancer();
        //2、设置父类（目标）
        enhancer.setSuperclass(Target.class);
        //3、设置回调
        enhancer.setCallback(new MethodInterceptor() {
            public Object intercept(Object proxy, Method method, Object[] args, MethodProxy methodProxy) throws Throwable {
                advice.before(); //执行前置
                Object invoke = method.invoke(target, args);//执行目标
                advice.afterReturning(); //执行后置
                return invoke;
            }
        });
        //4、创建代理对象
        Target proxy = (Target) enhancer.create();
        proxy.save();
    }
}
```

③调用代理对象的方法测试

```java
//4、创建代理对象
Target proxy = (Target) enhancer.create();
proxy.save();
```

![](assets/图片3.png)

#### 1.7 AOP 相关概念

Spring 的 AOP 实现底层就是对上面的动态代理的代码进行了封装，封装后我们只需要对需要关注的部分进行代码编写，并通过配置的方式完成指定目标的方法增强。

在正式讲解 AOP 的操作之前，必须理解 AOP 的相关术语，常用的术语如下：

- `Target`目标对象：代理的目标对象

- `Proxy`代理：一个类被 AOP 织入增强后，就产生一个结果代理类

- `Joinpoint`连接点：所谓连接点是指那些被拦截到的点。在spring中,这些点指的是方法，因为spring只支持方法类型的连接点

- `Pointcut`切入点：所谓切入点是指要对哪些 `Joinpoint` 进行拦截的定义

- `Advice`通知/ 增强：所谓通知是指拦截到 `Joinpoint` 之后所要做的事情就是通知

- `Aspect`切面：是切入点和通知（引介）的结合

- `Weaving`织入：是指把增强应用到目标对象来创建新的代理对象的过程。spring采用动态代理织入，而AspectJ采用编译期织入和类装载期织入

#### 1.8 AOP 开发明确的事项

##### 1)需要编写的内容

- 编写核心业务代码（目标类的目标方法）

- 编写切面类，切面类中有通知(增强功能方法)

- 在配置文件中，配置织入关系，即将哪些通知与哪些连接点进行结合

##### 2）AOP 技术实现的内容

Spring 框架监控切入点方法的执行。一旦监控到切入点方法被运行，使用代理机制，动态创建目标对象的代理对象，根据通知类别，在代理对象的对应位置，将通知对应的功能织入，完成完整的代码逻辑运行。

##### 3）AOP 底层使用哪种代理方式

在 spring 中，框架会根据目标类是否实现了接口来决定采用哪种动态代理的方式。

#### 1.9 知识要点

- aop：面向切面编程

- aop底层实现：基于JDK的动态代理 和 基于Cglib的动态代理

- aop的重点概念：

        `Pointcut`（切入点）：被增强的方法
        
        `Advice`（通知/ 增强）：封装增强业务逻辑的方法
        
        `Aspect`（切面）：切点+通知
        
        `Weaving`（织入）：将切点与通知结合的过程

- 开发明确事项：

        谁是切点（切点表达式配置）
        
        谁是通知（切面类中的增强方法）
        
        将切点和通知进行织入配置

### 2. 基于 XML 的 AOP 开发

#### 2.1 快速入门

①导入 AOP 相关坐标

②创建目标接口和目标类（内部有切点）

③创建切面类（内部有增强方法）

④将目标类和切面类的对象创建权交给 spring

⑤在 applicationContext.xml 中配置织入关系

⑥测试代码





①导入 AOP 相关坐标

```xml
<!--导入spring的context坐标，context依赖aop-->
<dependency>
  <groupId>org.springframework</groupId>
  <artifactId>spring-context</artifactId>
  <version>5.0.5.RELEASE</version>
</dependency>
<!-- aspectj的织入 -->
<dependency>
  <groupId>org.aspectj</groupId>
  <artifactId>aspectjweaver</artifactId>
  <version>1.8.13</version>
</dependency>
```

②创建目标接口和目标类（内部有切点）

```java
public interface TargetInterface {
    public void save();
}

public class Target implements TargetInterface {
    @Override
    public void save() {
        System.out.println("Target running....");
    }
}
```

③创建切面类（内部有增强方法）

```java
package tk.deriwotua.aop;

import org.aspectj.lang.ProceedingJoinPoint;

public class MyAspect {

    public void before(){
        System.out.println("前置增强..........");
    }

    public void afterReturning(){
        System.out.println("后置增强..........");
    }

    //Proceeding JoinPoint:  正在执行的连接点===切点
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        System.out.println("环绕前增强....");
        Object proceed = pjp.proceed();//切点方法
        System.out.println("环绕后增强....");
        return proceed;
    }

    public void afterThrowing(){
        System.out.println("异常抛出增强..........");
    }

    public void after(){
        System.out.println("最终增强..........");
    }

}
```

④将目标类和切面类的对象创建权交给 spring

```xml
<!--目标对象-->
<bean id="target" class="tk.deriwotua.aop.Target"></bean>

<!--切面对象-->
<bean id="myAspect" class="tk.deriwotua.aop.MyAspect"></bean>
```

⑤在 applicationContext.xml 中配置织入关系

导入aop命名空间

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xmlns:aop="http://www.springframework.org/schema/aop"
       xsi:schemaLocation="
       http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd
       http://www.springframework.org/schema/aop http://www.springframework.org/schema/aop/spring-aop.xsd">
    
    <!--目标对象-->
    <bean id="target" class="tk.deriwotua.aop.Target"></bean>

    <!--切面对象-->
    <bean id="myAspect" class="tk.deriwotua.aop.MyAspect"></bean>
</beans>
```

⑤在 applicationContext.xml 中配置织入关系

配置切点表达式和前置增强的织入关系

```xml

<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xmlns:aop="http://www.springframework.org/schema/aop"
       xsi:schemaLocation="
       http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd
       http://www.springframework.org/schema/aop http://www.springframework.org/schema/aop/spring-aop.xsd">
    
    <!--目标对象-->
    <bean id="target" class="tk.deriwotua.aop.Target"></bean>

    <!--切面对象-->
    <bean id="myAspect" class="tk.deriwotua.aop.MyAspect"></bean>

    <aop:config>
        <!--引用myAspect的Bean为切面对象-->
        <aop:aspect ref="myAspect">
            <!--配置Target的method方法执行时要进行myAspect的before方法前置增强-->
            <aop:before method="before" pointcut="execution(public void tk.deriwotua.aop.Target.save())"></aop:before>
        </aop:aspect>
    </aop:config>
</beans>
```

⑥测试代码

```java
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:applicationContext.xml")
public class AopTest {
    @Autowired
    private TargetInterface target;
    @Test
    public void test1(){
        target.method();
    }
}
```

⑦测试结果

![](assets/图片4.png)

#### 2.2 XML 配置 AOP 详解

##### 1) 切点表达式的写法

表达式语法：

```java
execution([修饰符] 返回值类型 包名.类名.方法名(参数))
```

- 访问修饰符可以省略

- 返回值类型、包名、类名、方法名可以使用星号*  代表任意

- 包名与类名之间一个点 . 代表当前包下的类，两个点 .. 表示当前包及其子包下的类

- 参数列表可以使用两个点 .. 表示任意个数，任意类型的参数列表

例如：

```xml
<!--配置织入：告诉spring框架 哪些方法(切点)需要进行哪些增强(前置、后置...)-->
<aop:config>
    <!--声明切面-->
    <aop:aspect ref="myAspect">
        <!--抽取切点表达式-->
        <aop:pointcut id="myPointcut" expression="execution(* tk.deriwotua.aop.*.*(..))"></aop:pointcut>
        <!--切面：切点+通知-->
        <!--<aop:before method="before" pointcut="execution(public void Target.save())"/>-->
        <!--<aop:before method="before" pointcut="execution(* tk.deriwotua.aop.*.*(..))"/>-->
		<!-- 后置通知 增强方法切入点之后执行 -->        
        <!--<aop:after-returning method="afterReturning" pointcut="execution(* tk.deriwotua.aop.*.*(..))"/>-->
        <!--<aop:around method="around" pointcut="execution(* tk.deriwotua.aop.*.*(..))"/>-->
		<!-- 异常通知 指定方法出现异常时执行 -->
        <!--<aop:after-throwing method="afterThrowing" pointcut="execution(* tk.deriwotua.aop.*.*(..))"/>
        <aop:after method="after" pointcut="execution(* tk.deriwotua.aop.*.*(..))"/>
        <aop:after method="after" pointcut="execution(* *..*.*(..))"/>-->
		<!-- 环绕通知 指定方法切入点方法之前之后执行 -->        
        <aop:around method="around" pointcut-ref="myPointcut"/>
		<!-- 最终通知 增强方法执行是否有异常都会执行 -->          
        <aop:after method="after" pointcut-ref="myPointcut"/>

    </aop:aspect>
</aop:config>
```

##### 2) 通知的类型

通知的配置语法：

```xml
<aop:通知类型 method="切面类中方法名" pointcut="切点表达式"></aop:通知类型>
```

![](assets/图片5.png)

##### 3) 切点表达式的抽取

当多个增强的切点表达式相同时，可以将切点表达式进行抽取，在增强中使用 pointcut-ref 属性代替 pointcut 属性来引用抽取后的切点表达式。

```xml
<aop:config>
    <!--引用myAspect的Bean为切面对象-->
    <aop:aspect ref="myAspect">
        <aop:pointcut id="myPointcut" expression="execution(* tk.deriwotua.aop.*.*(..))"/>
        <aop:before method="before" pointcut-ref="myPointcut"></aop:before>
    </aop:aspect>
</aop:config>
```

#### 2.3 知识要点

- aop织入的配置

```xml
<aop:config>
    <aop:aspect ref="切面类">
        <aop:before method="通知方法名称" pointcut="切点表达式"></aop:before>
    </aop:aspect>
</aop:config>
```

- 通知的类型：前置通知、后置通知、环绕通知、异常抛出通知、最终通知
- 切点表达式的写法：

```xml
execution([修饰符] 返回值类型 包名.类名.方法名(参数))
```

### 3.基于注解的 AOP 开发

#### 3.1 快速入门

基于注解的aop开发步骤：

①创建目标接口和目标类（内部有切点）

②创建切面类（内部有增强方法）

③将目标类和切面类的对象创建权交给 spring

④在切面类中使用注解配置织入关系

⑤在配置文件中开启组件扫描和 AOP 的自动代理

⑥测试



①创建目标接口和目标类（内部有切点）

```java
public interface TargetInterface {
    public void save();
}

public class Target implements TargetInterface {
    public void save() {
        System.out.println("save running.....");
        //int i = 1/0;
    }
}
```

②创建切面类（内部有增强方法)

```java
public class MyAspect {
    //前置增强方法
    public void before(){
        System.out.println("前置代码增强.....");
    }
}
```

③将目标类和切面类的对象创建权交给 spring

```java
@Component("target")
public class Target implements TargetInterface {
    @Override
    public void method() {
        System.out.println("Target running....");
    }
}
@Component("myAspect")
public class MyAspect {
    public void before(){
        System.out.println("前置代码增强.....");
    }
}
```

④在切面类中使用注解配置织入关系

```java
package tk.deriwotua.anno;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;

@Component("myAspect")
@Aspect //标注当前MyAspect是一个切面类
public class MyAspect {

    //配置前置通知
    //@Before("execution(* tk.deriwotua.anno.*.*(..))")
    public void before(){
        System.out.println("前置增强..........");
    }

    public void afterReturning(){
        System.out.println("后置增强..........");
    }

    //Proceeding JoinPoint:  正在执行的连接点===切点
    @Around("execution(* tk.deriwotua.anno.*.*(..))")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        System.out.println("环绕前增强....");
        Object proceed = pjp.proceed();//切点方法
        System.out.println("环绕后增强....");
        return proceed;
    }

    public void afterThrowing(){
        System.out.println("异常抛出增强..........");
    }

    @After("execution(* tk.deriwotua.anno.*.*(..))")
    public void after(){
        System.out.println("最终增强..........");
    }

    //定义切点表达式
    @Pointcut("execution(* tk.deriwotua.anno.*.*(..))")
    public void pointcut(){}

}
```

⑤在配置文件中开启组件扫描和 AOP 的自动代理

```xml
<!--组件扫描-->
<context:component-scan base-package="com.itheima.aop"/>

<!--aop的自动代理-->
<aop:aspectj-autoproxy></aop:aspectj-autoproxy>
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xmlns:aop="http://www.springframework.org/schema/aop"
       xmlns:context="http://www.springframework.org/schema/context"
       xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd
       http://www.springframework.org/schema/aop http://www.springframework.org/schema/aop/spring-aop.xsd
       http://www.springframework.org/schema/context http://www.springframework.org/schema/context/spring-context.xsd">

    <!--组件扫描-->
    <context:component-scan base-package="tk.deriwotua.anno"/>

    <!--aop自动代理-->
    <aop:aspectj-autoproxy/>

</beans>
```

⑥测试代码

```java
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:applicationContext.xml")
public class AopTest {
    @Autowired
    private TargetInterface target;
    @Test
    public void test1(){
        target.method();
    }
}
```

⑦测试结果

![](assets/图片6.png)

#### 3.2 注解配置 AOP 详解

##### 1) 注解通知的类型

通知的配置语法：@通知注解(“切点表达式")

![](assets/图片7.png)

##### 2) 切点表达式的抽取

同 xml配置
aop 一样，我们可以将切点表达式抽取。抽取方式是在切面内定义方法，在该方法上使用@Pointcut注解定义切点表达式，然后在在增强注解中进行引用。具体如下：

```java
package tk.deriwotua.anno;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;

@Component("myAspect")
@Aspect //标注当前MyAspect是一个切面类
public class MyAspect {

    //Proceeding JoinPoint:  正在执行的连接点===切点
    //@Around("execution(* tk.deriwotua.anno.*.*(..))")
    @Around("pointcut()")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        System.out.println("环绕前增强....");
        Object proceed = pjp.proceed();//切点方法
        System.out.println("环绕后增强....");
        return proceed;
    }

    //@After("execution(* tk.deriwotua.anno.*.*(..))")
    @After("tk.deriwotua.anno.MyAspect.pointcut()")
    public void after(){
        System.out.println("最终增强..........");
    }

    //定义切点表达式
    @Pointcut("execution(* tk.deriwotua.anno.*.*(..))")
    public void pointcut(){}

}
```

#### 3.3 知识要点

- 注解aop开发步骤

①使用@Aspect标注切面类

②使用@通知注解标注通知方法

③在配置文件中配置aop自动代理`<aop:aspectj-autoproxy/>`



- 通知注解类型

![](assets/图片8.png)

## spring jdbctemplate

[spring jdbctemplate](../spring-jdbctemplate/README.md)

## SpringMVC web环境集成

[SpringMVC web环境集成](../springmvc-quickstart/README.md)

## SpringMVC请求和响应

[SpringMVC请求和响应](../springmvc-request-response/README.md)

## SpringMVC拦截器

[SpringMVC拦截器](../springmvc-interceptor/README.md)

## Spring与SpringMVC整合

[Spring与SpringMVC整合](../spring-springmvc/README.md)