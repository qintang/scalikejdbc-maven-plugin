# scalikejdbc-maven-plugin
 [https://github.com/qintang/scalikejdbc-maven-plugin][4]

## 自动生成scalikejdbc domain scala对象
生成对象如下:
```scala
package com

import org.joda.time.DateTime
import scalikejdbc._

/**
 * QArea is a Querydsl query type for QArea
 */
case class QArea(
        var id: String
,         var name: String
,         var parentId: Option[String]
,         var `type`: Option[Byte]
,         var zip: Option[String]
)

object QArea extends SQLSyntaxSupport[QArea] {

  override val columns = Seq("id", "name", "parent_id", "type", "zip")
  def apply(c: SyntaxProvider[QArea])(rs: WrappedResultSet): QArea = apply(c.resultName)(rs)

  def apply(c: ResultName[QArea])(rs: WrappedResultSet): QArea = new QArea(  
id = rs.get[String](c.id)
, name = rs.get[String](c.name)
, parentId = rs.get[Option[String]](c.parentId)
, `type` = rs.get[Option[Byte]](c.`type`)
, zip = rs.get[Option[String]](c.zip)

    )
  }
```
#如何使用
## `注意`由于现在没有发布到中央maven库,源代码下载编译安装到本地
```shell
git clone https://github.com/qintang/scalikejdbc-maven-plugin.git 
cd scalikejdbc-maven-plugin
mvn clean install
```

## 1.在项目中引入插件
```xml
<plugin>
    <groupId>com.maoren.plugin</groupId>
    <artifactId>scalikejdbc-maven-plugin</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <configuration>
        <modelPackage>com.demo.ccms.node.query.domain</modelPackage>
        <jdbcDriver>com.mysql.jdbc.Driver</jdbcDriver>
        <jdbcUrl><![CDATA[jdbc:mysql://localhost:3306/ccms6?useUnicode=true&characterEncoding=utf8&autoReconnect=true&allowMultiQueries=true]]></jdbcUrl>
        <jdbcUser>root</jdbcUser>
        <jdbcPassword>root</jdbcPassword>
    </configuration>
    <dependencies>
        <dependency>
            <groupId>mysql</groupId>
            <artifactId>mysql-connector-java</artifactId>
            <version>5.1.32</version>
        </dependency>
    </dependencies>
    <executions>
        <execution>
            <phase>generate-sources</phase>
            <goals>
                <goal>generate-domain-models</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```
## 2.配置选项
* modelDirectory 源代码生成路径
  * 默认配置:${project.basedir}/target/generated-sources/scala
* modelPackage 生成对象包路径
  * 必须配置
* domainDesc 领域对象h2建表sql文件
  * 默认路径:${project.basedir}/src/main/resources/domain-desc.sql
  * 如果配置jdbc,优先使用jdbc配置
* jdbcDriver,jdbcUrl,jdbcUser,jdbcPassword 配置domain存在的数据库
  * 如果配置了此选项,优先使用jdbc中的对象,如果未配置或者配置为null,使用domainDesc 中的配置项
  
## 3.执行goal
```shell
mvn scalikejdbc-maven-plugin:generate-domain-models
```
## 生成如下对象
![ ][1]

参考
== 
  * [scalikejdbc-async ][2] scalikejdbc 异步sql执行
  * [querydsl-sql-maven-plugin][3] querydsl domain 生成插件

[1]: ./doc/img/generate.png
[2]: https://github.com/scalikejdbc/scalikejdbc-async
[3]: https://github.com/smith61/querydsl-sql-maven-plugin.git
[4]: https://github.com/qintang/scalikejdbc-maven-plugin