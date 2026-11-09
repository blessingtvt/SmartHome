# 面向老年人的智能家居主动感知系统

ESP32 采集家庭环境信息，Spring Boot 微服务处理后端业务，Vue 家属端查看与控制。核心链路：
**环境感知 → 数据上传 → 条件判断 → 协同决策 → 设备执行 → 状态反馈**，以及 **一键求助 → 告警生成 → 家属查看处理**。

## 技术栈

| 层次 | 技术 |
|---|---|
| 语言 | Java 17 |
| 框架 | Spring Boot 3.2.4 |
| Web | Spring Web |
| ORM | MyBatis-Plus 3.5.7 |
| 数据库 | MySQL 8（本机 127.0.0.1:3306，库 `smart_home_db`） |
| 微服务 | Spring Cloud 2023.0.1 / Spring Cloud Alibaba 2023.0.1.0（阶段6接入） |
| 网关 | Spring Cloud Gateway（阶段6接入） |
| 注册发现 | Nacos 2.3.2（阶段6接入） |
| 前端 | Vue 3 + Vite + Element Plus（阶段7） |
| 硬件 | ESP32 + Arduino（阶段7） |

## 模块划分

| 模块 | 服务 | 端口 | 职责 |
|---|---|---|---|
| smart-home-common | — | — | 统一响应、通用异常、常量、工具类 |
| environment-service | S1 | 8081 | 环境数据接收、校验、存储、查询 |
| rule-service | S2 | 8082 | 自动化规则管理、条件判断、联动触发 |
| device-service | S3 | 8083 | 设备信息、状态与控制指令管理 |
| alarm-service | S4 | 8084 | 环境异常告警、一键求助管理 |
| gateway-service | G0 | 8080 | 统一 API 入口（阶段6启用） |

## 目录结构

```
smart-home/
├── pom.xml                父工程（版本管理）
├── smart-home-common/     共享基础模块
├── environment-service/   S1
├── rule-service/          S2
├── device-service/        S3
├── alarm-service/         S4
├── gateway-service/       G0（占位）
└── database/schema.sql    建表脚本
```

## 数据库

- 连接：`127.0.0.1:3306/smart_home_db`，账号 `smart_home` / `123456`，字符集 utf8mb4。
- 建表：执行 `database/schema.sql`（脚本可重复执行，先删后建）。
- 设备引用统一使用 `device_code` 字符串（非数字主键），以遵守服务间数据隔离。

## 构建与启动

```bash
# 编译整个工程
mvn clean install

# 单独启动某个服务
mvn -pl environment-service spring-boot:run
```

也可在 IntelliJ IDEA 中打开根 `pom.xml`，导入为 Maven 多模块项目，直接运行各服务的 `*Application` 启动类。

## 开发进度

- [x] 阶段1：项目骨架、版本管理、common 模块、各服务启动入口、建表脚本
- [ ] 阶段2：S1 环境服务
- [ ] 阶段3：S3 设备服务
- [ ] 阶段4：S4 告警服务
- [ ] 阶段5：S2 规则服务
- [ ] 阶段6：G0 网关 + Nacos
- [ ] 阶段7：ESP32 + Vue 前端联调
