# 在线挂号系统后端

## 技术栈

- Java 17
- Spring Boot 3.3
- Spring Web
- Spring Data JPA
- MySQL 8
- JWT

本项目采用 Spring Boot 单体架构，未引入 Spring Cloud，原因是业务规模较小，采用单体架构可以降低部署和理解成本，避免过度设计。

## 启动步骤

1. 创建并初始化数据库：

```bash
mysql -u root -p < 数据库初始化脚本/init.sql
```

如果本机 `mysql` 命令不在 PATH，可使用项目内置初始化工具：

```bash
mvn -q -DskipTests test-compile exec:java "-Dexec.mainClass=com.hospital.registration.DbInitializer" "-Dexec.classpathScope=test" "-Dexec.args=你的MySQL密码"
```

2. 修改数据库连接：

打开 `src/main/resources/application.yml`，按本机 MySQL 配置修改：

```yaml
spring:
  datasource:
    username: root
    password: 123456
```

也可以通过环境变量覆盖：

```bash
DB_HOST=localhost
DB_PORT=3306
DB_NAME=hospital_registration
DB_USERNAME=root
DB_PASSWORD=你的MySQL密码
```

3. 启动后端：

```bash
mvn spring-boot:run
```

服务地址：

```text
http://localhost:8080
```

## 演示账号

患者：

```text
13900000000 / 123456
```

管理员：

```text
18800000000 / admin123
```

## 关键业务规则

- 同一就诊人同一科室同一天只能存在一条有效预约。
- 提交预约时对号源行加悲观锁，避免并发抢最后一个号导致超卖。
- 预约成功后 30 分钟内可取消，超过后不可取消。
- 取消预约会释放对应号源。
- 提交预约支持 `idempotencyKey`，用于防止前端重复点击或网络重试造成重复提交。
