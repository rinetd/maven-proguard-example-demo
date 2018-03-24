---
title: "Maven 环境下使用 proguard-maven-plugin 插件混淆你的源码"
date: 2018-03-24T14:05:37+08:00
create: 2018-03-24T14:05:37+08:00
categories: []
tags: [Maven]
---

 摘要: a、ProGuard（http://proguard.sourceforge.net/） 是比较出色的 Java 代码混淆工具，可以有效的保护与优化你的代码。当然这里说的保护是防止恶意抄袭，通过混淆造成反编译阅读困难。但逻辑与内容并不会加密，仔细分析还是可以获得一些信息。 b、proguard-maven-plugin 是 Maven 中的 ProGuard 插件，可以非常方便的在你做 Maven 打包时进行代码混淆。 c、本文重点介绍 Maven 环境下插件的配置（重点参数），与类路径加载资源问题。
一、场景介绍

两个工程 Project1，Project2（将被混淆的工程）。Project1 将通过 Maven 依赖配置的方式引用混淆后的 Project2。后面我会详细介绍 pom.xml 的配置。
![](http://static.oschina.net/uploads/space/2014/0820/120401_YGFK_569848.png)

二、Maven 配置
1、Project1 的 pom.xml

该 pom.xml 比较简单主要通过 classifier 来判断是否使用混淆的 Jar（Project2）
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>org.noahx.proguard.example</groupId>
    <artifactId>project1</artifactId>
    <version>1.0-SNAPSHOT</version>

    <dependencies>
        <dependency>
            <groupId>org.noahx.proguard.example</groupId>
            <artifactId>project2</artifactId>
            <classifier>pg</classifier> <!--如果不想依赖混淆的包，请注释掉该行-->
            <version>1.0-SNAPSHOT</version>
        </dependency>
    </dependencies>

</project>
```

2、Project2 的 pom.xml

pom.xml 中配置的 proguard-maven-plugin 来做混淆，详细说明见注释。
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>org.noahx.proguard.example</groupId>
    <artifactId>project2</artifactId>
    <version>1.0-SNAPSHOT</version>


    <build>
        <plugins>

            <plugin>
                <groupId>com.github.wvengen</groupId>
                <artifactId>proguard-maven-plugin</artifactId>
                <version>2.0.7</version>
                <executions>
                    <execution>
                        <phase>package</phase>
                        <goals>
                            <goal>proguard</goal>
                        </goals>
                    </execution>
                </executions>
                <configuration>
                    <attach>true</attach>
                    <attachArtifactClassifier>pg</attachArtifactClassifier>
                    <!-- attach 的作用是在 install 与 deploy 时将生成的 pg 文件也安装与部署 -->
                    <options> <!-- 详细配置方式参考 ProGuard 官方文档 -->
                        <!--<option>-dontobfuscate</option>-->
                        <option>-ignorewarnings</option> <!--忽略所有告警-->
                        <option>-dontshrink</option>   <!--不做 shrink -->
                        <option>-dontoptimize</option> <!--不做 optimize -->
                        <option>-dontskipnonpubliclibraryclasses</option>
                        <option>-dontskipnonpubliclibraryclassmembers</option>

                        <option>-repackageclasses org.noahx.proguard.example.project2.pg</option>
                        <!--平行包结构（重构包层次），所有混淆的类放在 pg 包下-->

                        <!-- 以下为 Keep，哪些内容保持不变，因为有一些内容混淆后（a,b,c）导致反射或按类名字符串相关的操作失效 -->

                        <option>-keep class **.package-info</option>
                        <!--保持包注解类-->

                        <option>-keepattributes Signature</option>
                        <!--JAXB NEED，具体原因不明，不加会导致 JAXB 出异常，如果不使用 JAXB 根据需要修改-->
                        <!-- Jaxb requires generics to be available to perform xml parsing and without this option ProGuard was not retaining that information after obfuscation. That was causing the exception above. -->

                        <option>-keepattributes SourceFile,LineNumberTable,*Annotation*</option>
                        <!--保持源码名与行号（异常时有明确的栈信息），注解（默认会过滤掉所有注解，会影响框架的注解）-->

                        <option>-keepclassmembers enum org.noahx.proguard.example.project2.** { *;}</option>
                        <!--保持枚举中的名子，确保枚举 valueOf 可以使用-->

                        <option>-keep class org.noahx.proguard.example.project2.bean.** { *;}</option>
                        <!--保持 Bean 类，（由于很多框架会对 Bean 中的内容做反射处理，请根据自己的业务调整） -->

                        <option>-keep class org.noahx.proguard.example.project2.Project2 { public void init(); public void
                            destroy(); }
                        </option>
                        <!-- 保持对外的接口性质类对外的类名与方法名不变 -->

                    </options>
                    <outjar>${project.build.finalName}-pg</outjar>
                    <libs>
                        <lib>${java.home}/lib/rt.jar</lib>
                    </libs>

                </configuration>
            </plugin>

         </plugins>
    </build>

</project>

```
三、Java 混淆前后内容比较

这里只比较 Project2 类的不同。其它类的比较，请大家使用 jd-gui 等反编译工具进行比较。
1、混淆前的 Project2 类
```
package org.noahx.proguard.example.project2;

import org.noahx.proguard.example.project2.dao.TestDao;
import org.noahx.proguard.example.project2.impl.User;

/**
 * Created by noah on 8/20/14.
 */
public class Project2 {

    public void init() {
        test1();
        test2();
    }

    private void test1() {
        Status on = Status.valueOf("On");
        switch (on) {
            case On: {

            }
            break;
            case Off: {

            }
            break;
        }
    }

    private void test2() {
        TestDao testDao=new TestDao();
        User user=new User();
        user.setUserid("abc");
        user.setPassword("pwd");
        user.setDescription("des");
        testDao.save(user);

    }

    private void test3() {
    }

    private void test4() {
    }

    private void throwException() {
        throw new RuntimeException("hello");
    }

    public void destroy() {
        test3();
        test4();
        throwException();
    }
}


2、混淆后的 Project2 类

所有没有指定 keep 的内容都变为了 a,b,c...，增大了阅读难度。

package org.noahx.proguard.example.project2;

import org.noahx.proguard.example.project2.pg.a;

public class Project2
{
  public void init()
  {
    b();
    c();
  }

  private void b() {
    b localb = b.valueOf("On");
    switch (a.a[localb.ordinal()])
    {
    case 1:
      break;
    case 2:
    }
  }

  private void c()
  {
    a locala = new a();
    org.noahx.proguard.example.project2.pg.b localb = new org.noahx.proguard.example.project2.pg.b();
    localb.a("abc");
    localb.b("pwd");
    localb.c("des");
    locala.a(localb);
  }

  private void d()
  {
  }

  private void e() {
  }

  public void a() {
    throw new RuntimeException("hello");
  }

  public void destroy() {
    d();
    e();
    a();
  }
}


四、类路径中资源加载问题

使用 ProGuard 产生的 Jar 包，会发生无法定位 Jar 中资源的问题。原因不详，我没有太深入研究。

使用 [类名].class.getResource()，Thread.currentThread().getContextClassLoader().getResource()，不论是否以“/”开头都返回 null。没有混淆的 Jar 是没有这个问题的。

我使用了一种直接读取 Jar 中内容的方式来解决。

final File jarFile = new File([类名].class.getProtectionDomain().getCodeSource().getLocation().getPath()); //定位类所在的 Jar 文件
            if(jarFile.isFile()) {
                final JarFile jar = new JarFile(jarFile);
                Enumeration<JarEntry> entries = jar.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    if (entry.getName().startsWith("org/noahx")) {
                        InputStream entryInputStream = jarFile.getInputStream(entry);  //遍历包中的内容来获得资源
                    }
                }
                jar.close();
            }

五、总结

使用 proguard-maven-plugin 插件，既保持了 Maven 的依赖模式，又满足了我的混淆需求。其它详细的参数配置，大家可以参考官方文档。

ProGuard 满足了我的需求。至于是好是坏，希望大家不要围绕这点做没有必要的争论，谢谢。 