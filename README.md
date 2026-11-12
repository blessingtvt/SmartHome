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
├── gateway-service/       G0 网关
├── database/              建表与迁移脚本
├── frontend/              Vue 3 家属端（独立子项目）
├── esp32-firmware/        ESP32 固件（独立子项目）
└── docs/                  项目文档
```

## 数据库

- 连接：`127.0.0.1:3306/smart_home_db`，账号 `smart_home` / `<密码>`，字符集 utf8mb4。
- 建表：执行 `database/schema.sql`（脚本可重复执行，先删后建）。
- 设备引用统一使用 `device_code` 字符串（非数字主键），以遵守服务间数据隔离。

## 使用说明

### 环境依赖

| 依赖 | 版本 / 路径 |
|---|---|
| JDK | 17（本机 `C:\Program Files\Microsoft\jdk-17.0.13.11-hotspot`） |
| MySQL | Windows 服务 `MySQL84`，端口 3306 |
| Nacos | `D:\nacos`，端口 8848（standalone） |
| Node.js / npm | 18+ / 10+（前端） |
| Arduino IDE | 需 ESP32 板卡支持（硬件固件，可选） |

### 启动顺序

**MySQL → Nacos → 后端 S1~S4 → 网关 → 前端 → ESP32**

### 1. 数据库初始化（仅首次）

先执行 `database/schema.sql`（先删后建），再按日期顺序执行 `database/migration_*.sql` 迁移脚本（见「数据库」一节）。

### 2. 启动 Nacos

```powershell
$env:JAVA_HOME = 'C:\Program Files\Microsoft\jdk-17.0.13.11-hotspot'
& 'D:\nacos\bin\startup.cmd' -m standalone
```

### 3. 构建并启动后端

```bash
# 编译整个工程
mvn clean package -DskipTests

# 依次启动（各开一个终端，网关最后启动）
java -jar environment-service/target/environment-service-0.0.1-SNAPSHOT.jar   # S1 8081
java -jar rule-service/target/rule-service-0.0.1-SNAPSHOT.jar                 # S2 8082
java -jar device-service/target/device-service-0.0.1-SNAPSHOT.jar             # S3 8083
java -jar alarm-service/target/alarm-service-0.0.1-SNAPSHOT.jar               # S4 8084
java -jar gateway-service/target/gateway-service-0.0.1-SNAPSHOT.jar           # G0 8080
```

也可在 IntelliJ IDEA 中打开根 `pom.xml`，导入为 Maven 多模块项目，直接运行各服务的 `*Application` 启动类。

### 4. 启动前端

```bash
cd frontend
npm install   # 首次
npm run dev
```

### 5. 启动 ESP32（可选，硬件端）

用 Arduino IDE 打开 `esp32-firmware/SmartHomeESP32`，将 `config.example.h` 复制为 `config.h` 并填写 Wi-Fi、`SERVER_HOST`（后端电脑局域网 IPv4，不能用 127.0.0.1）、`SERVER_PORT=8080`、`DEVICE_CODE`，再编译烧录上电。

### 访问地址

| 对象 | 地址 |
|---|---|
| 家属端前端（dev） | http://localhost:5173 |
| 统一网关 | http://127.0.0.1:8080 |
| Nacos 控制台 | http://127.0.0.1:8848/nacos（nacos/nacos） |
| MySQL | 127.0.0.1:3306（库 `smart_home_db`） |

> 完整使用说明（含家属端五页面、ESP32 接线、接口速查、常见问题排查）见 `docs/使用说明书.docx`。

## 开发进度

- [x] 阶段1：项目骨架、版本管理、common 模块、各服务启动入口、建表脚本
- [x] 阶段2：S1 环境服务（环境数据接收、校验、存储、查询）
- [x] 阶段3：S3 设备服务（设备 CRUD、心跳、离线判定、指令管理）
- [x] 阶段4：S4 告警服务（告警创建、一键求助、状态流转、分页查询）
- [x] 阶段5：S2 规则服务（规则管理、条件评估、自动联动）
- [x] 阶段6：G0 网关 + Nacos（服务注册发现、统一路由）
- [x] 阶段7：ESP32 硬件接入 + Vue 家属端 + 端到端联调

## 后续开发计划

- [ ] 引入 JUnit 与接口自动化测试（MockMvc / Testcontainers），建立回归基线
- [ ] 服务间调用由直连 IP 改为 Nacos 服务发现（`lb://` + OpenFeign/RestClient），支持多实例与故障转移
- [ ] 接入 MQ-2 模拟量采集（换用带 ADC 的引脚或外部 ADC 模块）并做燃气浓度标定
- [ ] 补齐外设接入：水滴传感器、智能语音模块、SK6812 RGB、MFRC522 RFID、LCD1602
- [ ] ESP32 指令去重改为持久化（RTC / NVS 存储已执行指令 ID），保证重启不重复执行
- [ ] 前端引入 WebSocket / SSE 推送，降低轮询延迟（当前约 30s 轮询）
- [ ] 统一数据库迁移管理（Flyway / Liquibase），解决脚本幂等与版本管理
- [ ] 完善「异常与断网」场景测试（Wi-Fi 断开、Nacos/Gateway 宕机、HTTP 超时、ESP32 重启），形成 7-06 全链路测试报告
