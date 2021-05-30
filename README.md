# mybatis-3
对Mybatis的源码分析，博文链接：https://blog.csdn.net/lx1315998513/article/details/117393672
@[TOC](从一次简单的Mybatis源码分析开启你的源码分析之路)

# 一、概述

## （一）什么是Mybatis

MyBatis是一款优秀的持久层（ORM）框架，但并不是一个完整的ORM框架，ORM是Object和Relation之间的映射，包括Object->Relation和Relation->Object两方面。Hibernate是个完整的ORM框架，而MyBatis完成的是Relation->Object，也就是其所说的data mapper framework。

```html
ORM框架 -> 用于实现面向对象编程语言里不同类型系统的数据之间的转换
Java对象（字段、类型）  映射（类型字段名一一对应）->  数据库表（字段、类型）
```



## （二）Mybatis能做些什么

它支持自定义 SQL、存储过程以及高级映射。MyBatis 免除了几乎所有的 JDBC 代码以及设置参数和获取结果集的工作。MyBatis 可以通过简单的 XML 或注解来配置和映射原始类型、接口和 Java POJO（Plain Old Java Objects，普通老式 Java 对象）为数据库中的记录。



# 二、分析过程

## （一）准备

从官方github下载源码：https://github.com/mybatis/mybatis-3

```html
https://github.com/mybatis/mybatis-3.git
```

从作者的github下载源码：https://github.com/1315998513/mybatis-3

```html
https://github.com/1315998513/mybatis-3.git
```

参考Mybatis官方文档：https://mybatis.net.cn/configuration.html
后续用到的一些配置详解，只拿了一部分出来，详情参考官方文档-配置（mybatis-config.xml）：

**属性（properties）**
这些属性可以在外部进行配置，并可以进行动态替换。你既可以在典型的 Java 属性文件中配置这些属性，也可以在 properties 元素的子元素中设置。例如：

```xml
driver=com.mysql.jdbc.Driver
url=jdbc:mysql://127.0.0.1:3306/lastpass?useUnicode=true&characterEncoding=utf-8&serverTimezone=GMT%2B8
username=root
password=123456
```

设置好的属性可以在整个配置文件中用来替换需要动态配置的属性值。比如:

```xml
<dataSource type="POOLED">
  <property name="driver" value="${driver}"/>
  <property name="url" value="${url}"/>
  <property name="username" value="${username}"/>
  <property name="password" value="${password}"/>
</dataSource>
```

**环境配置（environments）**
MyBatis 可以配置成适应多种环境，这种机制有助于将 SQL 映射应用于多种数据库之中， 现实情况下有多种理由需要这么做。例如，开发、测试和生产环境需要有不同的配置；或者想在具有相同 Schema 的多个生产数据库中使用相同的 SQL 映射。还有许多类似的使用场景。
environments 元素定义了如何配置环境。

```xml
<environments default="development">
  <environment id="development">
    <transactionManager type="JDBC">
      <property name="..." value="..."/>
    </transactionManager>
    <dataSource type="POOLED">
      <property name="driver" value="${driver}"/>
      <property name="url" value="${url}"/>
      <property name="username" value="${username}"/>
      <property name="password" value="${password}"/>
    </dataSource>
  </environment>
</environments
```

**数据源（dataSource）**
dataSource 元素使用标准的 JDBC 数据源接口来配置 JDBC 连接对象的资源。
大多数 MyBatis 应用程序会按示例中的例子来配置数据源。**虽然数据源配置是可选的，但如果要启用延迟加载特性，就必须配置数据源。**
有三种内建的数据源类型（也就是 type="[UNPOOLED|POOLED|JNDI]"）：
POOLED– 这种数据源的实现利用“池”的概念将 JDBC 连接对象组织起来，避免了创建新的连接实例时所必需的初始化和认证时间。 这种处理方式很流行，能使并发 Web 应用快速响应请求。

**映射器（mappers）**
定义 SQL 映射语句。 但首先，我们需要告诉 MyBatis 到哪里去找到这些语句。 在自动查找资源方面，Java 并没有提供一个很好的解决方案，所以最好的办法是直接告诉 MyBatis 到哪里去找映射文件。 你可以使用相对于类路径的资源引用，或完全限定资源定位符（包括 file:/// 形式的 URL），或类名和包名（**Mapper加载的四种方法，包加载优先级最高，后续在源码中体现**）等。



## （二）宏观分析

以一个小demo大致的演示Mybatis的执行流程
结构图：
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210530015231288.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2x4MTMxNTk5ODUxMw==,size_16,color_FFFFFF,t_70)
1.jdbc.properties

```xml
driver=com.mysql.jdbc.Driver
url=jdbc:mysql://127.0.0.1:3306/lastpass?useUnicode=true&characterEncoding=utf-8&serverTimezone=GMT%2B8
username=root
password=123456
```

2.mybatis-config.xml

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE configuration
  PUBLIC "-//mybatis.org//DTD Config 3.0//EN"
  "http://mybatis.org/dtd/mybatis-3-config.dtd">
<configuration>
  <properties resource="jdbc.properties"/>
  <!-- 配置数据库连接：连接驱动、url、username、password -->
  <environments default="development">
    <environment id="development">
      <transactionManager type="JDBC"/>
      <dataSource type="POOLED">
        <property name="driver" value="${driver}"/>
        <property name="url" value="${url}"/>
        <property name="username" value="${username}"/>
        <property name="password" value="${password}"/>
      </dataSource>
    </environment>
  </environments>
  <mappers>
    <mapper resource="mapper/UserMapper.xml"/>
  </mappers>
</configuration>
```

3.bean -> User

```java
public class User {
  private int id;
  private String name;
  private String password;
  ..getter setter
  ..toString
}
```

4.mapper -> UserMapper

```java
public interface UserMapper {
  User selectUser(@Param("id") Integer id);
}
```

5.mapper.xml -> mapper/UserMapper.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper
    PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
    "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="org.mybatis.example.UserMapper">

  <select id="selectUser" resultType="org.apache.ibatis.demo.User">
    select * from user where id = #{id}
  </select>

</mapper>
```

6.MybatisMain

```java
public class MybatisMain {
  public static void main(String[] args) throws IOException {

    String resource = "mybatis-config.xml";
    InputStream inputStream = Resources.getResourceAsStream(resource);

    SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);

    SqlSession session = sqlSessionFactory.openSession();

    User user = session.selectOne("org.mybatis.example.UserMapper.selectUser", 1);
    System.out.println(user);

  }
}
```

**总结：** 在main方法中，可以直观的看到，整个执行流程是：根据路径从配置文件读取流，构建会话工厂，通过会话工厂构建sql会话 **（SqlSession底层是通过调用Execute（执行器）真正执行Java与数据库交互）**，SqlSession调用selectOne方法，指定执行的sql，传入参数。
**执行结果：**
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210530022935419.png)



## （三）微观分析

```java
    SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
    
    SqlSession session = sqlSessionFactory.openSession();
    
    User user = session.selectOne("org.mybatis.example.UserMapper.selectUser", 1);
```

对上面的三段代码进行底层的debug分析，在这过程中解决三个问题：

```html
1.mybatis是如何获取数据库配置？
2.mybatis是如何执行sql语句？
3.mybatis是如何执行数据库的？
```

**终于来到了本文的重点部分，断点调试，用到的几个命令：**

```html
F7：进入当前方法；	F8：进入下一行；	F9：进入下一断点
Ctrl+指向方法：进入方法查看
Ctrl+Alt+左方向键：返回上一指向方法处
还有很多用到的，详情百度...
```

#### Mybatis是如何获取数据库配置？

1.F7进调
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210530024818631.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2x4MTMxNTk5ODUxMw==,size_16,color_FFFFFF,t_70)
2.F7进调
![在这里插入图片描述](https://img-blog.csdnimg.cn/2021053002541291.png)
3.这里创建了一个用于解析配置内容的对象XMLConfigBuilder，F7进入parse（）看看解析具体的实现
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210530030244430.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2x4MTMxNTk5ODUxMw==,size_16,color_FFFFFF,t_70)
4.F7进入parseConfiguration（）
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210530030334677.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2x4MTMxNTk5ODUxMw==,size_16,color_FFFFFF,t_70)
5.可以看到该方法是对xml配置文件中的 configuration进行解析，F7进environmentsElement（）查看对environments部分的解析
![在这里插入图片描述](https://img-blog.csdnimg.cn/2021053003060944.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2x4MTMxNTk5ODUxMw==,size_16,color_FFFFFF,t_70)
6.F7进dataSourceElement（）查看对dataSource部分的解析
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210530030903788.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2x4MTMxNTk5ODUxMw==,size_16,color_FFFFFF,t_70)
7.getChildrenAsProperties（）拿到Properties中的数据封装成Properties对象
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210530031052454.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2x4MTMxNTk5ODUxMw==,size_16,color_FFFFFF,t_70)
8.F7进入resolveClass（）
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210530031209374.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2x4MTMxNTk5ODUxMw==,size_16,color_FFFFFF,t_70)
9.F7进入resolveAlias（）
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210530031243333.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2x4MTMxNTk5ODUxMw==,size_16,color_FFFFFF,t_70)
10.F7进入resolveAlias（）
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210530031313733.png)
11.根据字符串“POOLED”转小写，从typeAliases这个map中拿到对应的对象“org.apache.ibatis.datasource.pooled.PooledDataSourceFactory”
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210530110032682.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2x4MTMxNTk5ODUxMw==,size_16,color_FFFFFF,t_70)
12.一直F8往下调回到上面第6步的时候，过了setEnvironment（）后，可以看到数据源等配置已经被赋值到configuration全局配置类中了
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210530110639740.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2x4MTMxNTk5ODUxMw==,size_16,color_FFFFFF,t_70)
13.F8下一步直到返回到第5步，F7进入mapperElement（），这部分是对mappers的解析
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210530110755641.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2x4MTMxNTk5ODUxMw==,size_16,color_FFFFFF,t_70)
14.mapper加载的4中方式，package优先级最高，这里创建了XMLMapperBuilder用于解析mapper部分的xml标签，F7进入parse()
![在这里插入图片描述](https://img-blog.csdnimg.cn/202105301111406.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2x4MTMxNTk5ODUxMw==,size_16,color_FFFFFF,t_70)
15.F7进入configurationElement
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210530111218976.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2x4MTMxNTk5ODUxMw==,size_16,color_FFFFFF,t_70)
16.F7进入到buildStatementFromContext（）
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210530111410378.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2x4MTMxNTk5ODUxMw==,size_16,color_FFFFFF,t_70)
17.此时查看刚刚解析的select标签然后传参进来的list，可以看到解析到的select标签id，返回值类型，sql，F7进入到
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210530111600417.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2x4MTMxNTk5ODUxMw==,size_16,color_FFFFFF,t_70)
18.F7进入buildStatementFromContext（）
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210530111617311.png)
19.F7进入parseStatementNode（）
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210530112350501.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2x4MTMxNTk5ODUxMw==,size_16,color_FFFFFF,t_70)
20.这部分是对标签中解析到的select标签id，返回值类型，sql等等属性进行封装到builderAssistant中，往下查看
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210530113228212.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2x4MTMxNTk5ODUxMw==,size_16,color_FFFFFF,t_70)
21.这部分是对sql部分进行解析，F7进入createSqlSource（）
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210530113726662.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2x4MTMxNTk5ODUxMw==,size_16,color_FFFFFF,t_70)
22.F7进入parseScriptNode（）
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210530113810862.png)
23.F7进入RawSqlSource（）
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210530113951943.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2x4MTMxNTk5ODUxMw==,size_16,color_FFFFFF,t_70)
24.F7进入this（）构造函数，F7进入parse（）
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210530114030291.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2x4MTMxNTk5ODUxMw==,size_16,color_FFFFFF,t_70)
25.F7进入parse（）
![在这里插入图片描述](https://img-blog.csdnimg.cn/2021053011433461.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2x4MTMxNTk5ODUxMw==,size_16,color_FFFFFF,t_70)
26.这个方法是对sql进行语法解析，将#{xxx}转换为"?"，往下拉查看![在这里插入图片描述](https://img-blog.csdnimg.cn/2021053011452436.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2x4MTMxNTk5ODUxMw==,size_16,color_FFFFFF,t_70)
27.转换后
![在这里插入图片描述](https://img-blog.csdnimg.cn/2021053011461229.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2x4MTMxNTk5ODUxMw==,size_16,color_FFFFFF,t_70)
28.F8下一步直到返回到第23步，可以看到返回的sql已被转换
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210530114743626.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2x4MTMxNTk5ODUxMw==,size_16,color_FFFFFF,t_70)
29.F8下一步直到返回到第21步，这部分是拿到定义的返回值类型
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210530114918321.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2x4MTMxNTk5ODUxMw==,size_16,color_FFFFFF,t_70)
30.F7进入addMappedStatement（）
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210530115406220.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2x4MTMxNTk5ODUxMw==,size_16,color_FFFFFF,t_70)
31.往下查看
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210530115424907.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2x4MTMxNTk5ODUxMw==,size_16,color_FFFFFF,t_70)
32.解析到的mapper被封装到configuration中
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210530115453418.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2x4MTMxNTk5ODUxMw==,size_16,color_FFFFFF,t_70)
**总结：** 好呢，这就完结了呢，new SqlSessionFactoryBuilder().build(inputStream)做的事就是把xml里面的所有配置解析成Java对象。

```java
	/**
     * 一、解析数据库配置封装到 Configuration 对象
     * 1.SqlSessionFactoryBuilder().build(inputStream)
     *  2.XMLConfigBuilder.prase()
     *    3.parseConfiguration()
     *      4.environmentsElement()
     *        5.dataSourceElement()
     *          6.configuration.setEnvironment(environmentBuilder.build())
     * 二、解析Mapper封装到 Configuration 对象
     * 1.mapperElement()
     *  2.XMLMapperBuilder.prase()
     *    3.configurationElement()
     *      4.buildStatementFromContext()
     *        5.buildStatementFromContext()
     *          6.parseStatementNode()
     *            7.addMappedStatement()
     *              8.configuration.addMappedStatement(statement)
     */
```

#### 2.Mybatis是如何执行sql语句？

1.F7进入openSession（）
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210530120336478.png)
2.F7进入openSessionFromDataSource（）
![在这里插入图片描述](https://img-blog.csdnimg.cn/2021053012031719.png)
3.F7进入newExecutor（）
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210530120421188.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2x4MTMxNTk5ODUxMw==,size_16,color_FFFFFF,t_70)
4.这部分是判断创建哪一个执行器，默认创建SimpleExecutor（），Ctrl到SIMPLE查看
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210530120641160.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2x4MTMxNTk5ODUxMw==,size_16,color_FFFFFF,t_70)
5.创建执行器的三种类型
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210530120701373.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2x4MTMxNTk5ODUxMw==,size_16,color_FFFFFF,t_70)
6.回退到第4步，Ctrl查看defaultExecutorType
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210530120725858.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2x4MTMxNTk5ODUxMw==,size_16,color_FFFFFF,t_70)
7.回退到第4步，可以看到这里创建了一级缓存
![在这里插入图片描述](https://img-blog.csdnimg.cn/2021053012083989.png)
8.Ctrl查看cacheEnabled，默认是true
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210530120853439.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2x4MTMxNTk5ODUxMw==,size_16,color_FFFFFF,t_70)
9.回退到第1步
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210530121028194.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2x4MTMxNTk5ODUxMw==,size_16,color_FFFFFF,t_70)
**总结：** mybaits是通过创建一个executor执行器来执行sql的，其底层是对jdbc的封装，下面会看到。

```java
    /**
     * 执行器初始化，构建sqlSession，通过SqlSession得到Mapper接口对象进行数据库操作
     * 1.openSessionFromDataSource()
     *  2.configuration.newExecutor()
     *    3.new SimpleExecutor()
     */
```

#### 3.Mybatis是如何执行数据库的？

1.F7进入selectOne（）![在这里插入图片描述](https://img-blog.csdnimg.cn/20210530121219506.png)
2.F7进入selectList（）
![在这里插入图片描述](https://img-blog.csdnimg.cn/2021053012123797.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2x4MTMxNTk5ODUxMw==,size_16,color_FFFFFF,t_70)
3.F7进入selectList（）
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210530121415780.png)
4.F7进入query（）
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210530121452623.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2x4MTMxNTk5ODUxMw==,size_16,color_FFFFFF,t_70)
5.F7进入query（）![在这里插入图片描述](https://img-blog.csdnimg.cn/20210530121552992.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2x4MTMxNTk5ODUxMw==,size_16,color_FFFFFF,t_70)
6.F7进入query（）
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210530121659958.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2x4MTMxNTk5ODUxMw==,size_16,color_FFFFFF,t_70)
7.F7进入到queryFromDatabase（）
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210530121831883.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2x4MTMxNTk5ODUxMw==,size_16,color_FFFFFF,t_70)
8.F7进入doQuery（）
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210530121954710.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2x4MTMxNTk5ODUxMw==,size_16,color_FFFFFF,t_70)
9.F7进入query（）
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210530122748645.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2x4MTMxNTk5ODUxMw==,size_16,color_FFFFFF,t_70)
10.F7进入handleResultSets（）
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210530122923464.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2x4MTMxNTk5ODUxMw==,size_16,color_FFFFFF,t_70)
11.F7进入getFirstResultSet（）
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210530122955707.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2x4MTMxNTk5ODUxMw==,size_16,color_FFFFFF,t_70)
12.F7进入ResultSetWrapper（）
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210530123140942.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2x4MTMxNTk5ODUxMw==,size_16,color_FFFFFF,t_70)
13.F7进入super（）初始化，会创建三个List数组
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210530123211928.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2x4MTMxNTk5ODUxMw==,size_16,color_FFFFFF,t_70)
14.创建三个list数组，分别用于存放数据表列名、Java对象字段类型名、数据表字段类型名
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210530123255589.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2x4MTMxNTk5ODUxMw==,size_16,color_FFFFFF,t_70)
15.ORM框架的本质：不同系统数据类型之间的映射
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210530123743773.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2x4MTMxNTk5ODUxMw==,size_16,color_FFFFFF,t_70)
16.F8下一步直到返回到第11步，F7进入handleResultSet（）
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210530123904337.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2x4MTMxNTk5ODUxMw==,size_16,color_FFFFFF,t_70)
17.F7进入handleRowValues（）![在这里插入图片描述](https://img-blog.csdnimg.cn/20210530125024794.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2x4MTMxNTk5ODUxMw==,size_16,color_FFFFFF,t_70)
18.F7进入handleRowValuesForSimpleResultMap（）
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210530125053775.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2x4MTMxNTk5ODUxMw==,size_16,color_FFFFFF,t_70)
19.F7进入getRowValue（）
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210530125235680.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2x4MTMxNTk5ODUxMw==,size_16,color_FFFFFF,t_70)
20.F7进入applyAutomaticMappings（）
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210530125302985.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2x4MTMxNTk5ODUxMw==,size_16,color_FFFFFF,t_70)
21.这部分是逐个根据映射拿到对应数据封装成对象，F7进入getResult（）
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210530125329462.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2x4MTMxNTk5ODUxMw==,size_16,color_FFFFFF,t_70)
22.F7进入getNullableResult（）
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210530125620208.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2x4MTMxNTk5ODUxMw==,size_16,color_FFFFFF,t_70)
23.getNullableResult（）主要就是将返回值作为Integer返回。
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210530125657662.png)
24.遍历完成后
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210530130212246.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2x4MTMxNTk5ODUxMw==,size_16,color_FFFFFF,t_70)
结果如下：
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210530130816615.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2x4MTMxNTk5ODUxMw==,size_16,color_FFFFFF,t_70)
**总结：**

```java
    /**
     * 底层调用执行器查询数据：先开启本地缓存，在本地缓存中查找为空后，去数据库查找，然后把查询到的数据设置到缓存，在进行数据的映射、返回
     * 1.selectOne()
     *  2.selectList() -> selectList()
     *    3.executor.query()  -> createCacheKey() 创建缓存
     *      4.query() -> query()
     *        5.queryFromDatabase()
     *          6.doQuery()
     *            7.handleResultSets()
     *              8.getFirstResultSet()
     *                9.getResultSet()
     *                  10.ResultSetWrapper()
     *                  创建三个list数组，分别用于存放 数据表列名、Java对象字段类型名、数据表字段类型名
     *                  columnNames.add(configuration.isUseColumnLabel() ? metaData.getColumnLabel(i) : metaData.getColumnName(i));
     *                  jdbcTypes.add(JdbcType.forCode(metaData.getColumnType(i)));
     *                  classNames.add(metaData.getColumnClassName(i));
     *                    11.handleResultSet()
     *                      12.handleRowValues()
     *                        13.handleRowValuesForNestedResultMap
     *                          14.getRowValue()
     *                            15.applyAutomaticMappings() //自动映射
     */
```

## （四）图解分析

经过微观分析，整个Mybatis的完整执行流程图，如下：

```html
原图连接：https://processon.com/view/5cc57a59e4b059e20a0c21c0
```

![在这里插入图片描述](https://img-blog.csdnimg.cn/20210530023345420.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2x4MTMxNTk5ODUxMw==,size_16,color_FFFFFF,t_70)
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210530023350429.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2x4MTMxNTk5ODUxMw==,size_16,color_FFFFFF,t_70)


**架构图：**
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210530131147826.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2x4MTMxNTk5ODUxMw==,size_16,color_FFFFFF,t_70)

# 三、END

通过这一次简单的分析，对mybatis有了更深的认知呢，分析过程中省略了很多部分，并不全面，有兴趣的朋友可以自己去调试查看，这里对各个方法属性的解释也很少，因为都可以从名称语言上知道它们的作用，直接翻译，点进去看看实现吧~
欢迎留言指正、探讨、建议~
**参考资料：**

```html
https://www.bilibili.com/video/BV1kt4y1e7iM?from=search&seid=9805382726736153233
https://mybatis.net.cn/logging.html
```
