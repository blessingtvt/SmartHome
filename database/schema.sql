-- =====================================================================
-- 智能家居主动感知系统 数据库结构
-- 数据库: smart_home_db   字符集: utf8mb4
--
-- 数据归属原则（见《软件开发总规》）：单 MySQL 实例，按服务划分表归属，
-- 服务之间不得直接操作其他服务负责的表。
--
-- 关于设备引用：ESP32 / 前端只持有「设备编码字符串 device_code」，
-- 数字主键 device.id 仅在 S3 内部有意义。为遵守数据隔离、避免 S1/S4
-- 越界读取 S3 的 device 表，environment_data / device_command / alarm
-- 均使用 device_code 字符串引用设备（而非 device.id 数字外键）。
--
-- 本脚本可重复执行（先 DROP 后 CREATE）。
-- =====================================================================

SET NAMES utf8mb4;

-- 按依赖顺序先删后建（先删引用方，再删被引用方）
DROP TABLE IF EXISTS `alarm`;
DROP TABLE IF EXISTS `device_command`;
DROP TABLE IF EXISTS `rule_execution_log`;
DROP TABLE IF EXISTS `automation_rule`;
DROP TABLE IF EXISTS `environment_data`;
DROP TABLE IF EXISTS `device`;
DROP TABLE IF EXISTS `room`;
DROP TABLE IF EXISTS `family`;

-- ---------------------------------------------------------------------
-- family 家庭信息表（归属 S1）
-- ---------------------------------------------------------------------
CREATE TABLE `family` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '家庭编号',
    `family_name` VARCHAR(100) NOT NULL                COMMENT '家庭名称',
    `address`     VARCHAR(255)          DEFAULT NULL   COMMENT '家庭地址',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '家庭信息表';

-- ---------------------------------------------------------------------
-- room 房间信息表（归属 S1）
-- ---------------------------------------------------------------------
CREATE TABLE `room` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '房间编号',
    `family_id`   BIGINT       NOT NULL                COMMENT '所属家庭编号',
    `room_name`   VARCHAR(100) NOT NULL                COMMENT '房间名称',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_room_family_id` (`family_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '房间信息表';

-- ---------------------------------------------------------------------
-- device 设备信息表（归属 S3）
-- ---------------------------------------------------------------------
CREATE TABLE `device` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '设备主键（S3 内部使用）',
    `device_code`   VARCHAR(50)  NOT NULL                COMMENT '设备编码（唯一，ESP32/前端使用）',
    `device_name`   VARCHAR(100) NOT NULL                COMMENT '设备名称',
    `device_type`   VARCHAR(50)  NOT NULL                COMMENT '设备类型',
    `room_id`       BIGINT                DEFAULT NULL   COMMENT '所属房间编号（逻辑引用 S1.room）',
    `online_status` VARCHAR(20)  NOT NULL DEFAULT 'OFFLINE' COMMENT '在线状态',
    `device_status` VARCHAR(20)  NOT NULL DEFAULT 'OFF'     COMMENT '运行状态',
    `create_time`           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `last_heartbeat_time`   DATETIME              DEFAULT NULL   COMMENT '最后心跳/活动时间（ESP32 网络连接状态判定）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_device_code` (`device_code`),
    KEY `idx_device_room_id` (`room_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '设备信息表';

-- ---------------------------------------------------------------------
-- environment_data 环境数据表（归属 S1）
-- ---------------------------------------------------------------------
CREATE TABLE `environment_data` (
    `id`           BIGINT        NOT NULL AUTO_INCREMENT COMMENT '数据记录编号',
    `device_code`  VARCHAR(50)   NOT NULL                COMMENT '数据来源设备编码',
    `temperature`  DECIMAL(5, 2)          DEFAULT NULL   COMMENT '温度(℃)',
    `humidity`     DECIMAL(5, 2)          DEFAULT NULL   COMMENT '湿度(%)',
    `gas_raw`      INT                     DEFAULT NULL   COMMENT 'MQ-2 原始读数',
    `pir_triggered` TINYINT(1)            DEFAULT NULL   COMMENT 'PIR 是否触发(0/1)',
    `collect_time` DATETIME      NOT NULL                COMMENT '采集时间',
    PRIMARY KEY (`id`),
    KEY `idx_env_device_time` (`device_code`, `collect_time`),
    KEY `idx_env_collect_time` (`collect_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '环境数据表';

-- ---------------------------------------------------------------------
-- automation_rule 自动化规则表（归属 S2）
-- ---------------------------------------------------------------------
CREATE TABLE `automation_rule` (
    `id`               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '规则编号',
    `rule_name`        VARCHAR(100) NOT NULL                COMMENT '规则名称',
    `trigger_type`     VARCHAR(50)  NOT NULL                COMMENT '触发类型',
    `condition_config` JSON         NOT NULL                COMMENT '触发条件配置',
    `action_config`    JSON         NOT NULL                COMMENT '执行动作配置',
    `enabled`          TINYINT(1)   NOT NULL DEFAULT 1      COMMENT '是否启用(1/0)',
    `create_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '自动化规则表';

-- ---------------------------------------------------------------------
-- rule_execution_log 规则执行记录表（归属 S2）
-- ---------------------------------------------------------------------
CREATE TABLE `rule_execution_log` (
    `id`               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '记录编号',
    `rule_id`          BIGINT       NOT NULL                COMMENT '对应规则编号',
    `trigger_value`    VARCHAR(100)          DEFAULT NULL   COMMENT '触发时数据值',
    `execution_result` VARCHAR(50)  NOT NULL                COMMENT '判断或执行结果',
    `execution_time`   DATETIME     NOT NULL                COMMENT '执行时间',
    PRIMARY KEY (`id`),
    KEY `idx_rule_log_rule_id` (`rule_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '规则执行记录表';

-- ---------------------------------------------------------------------
-- device_command 设备控制指令表（归属 S3）
-- ---------------------------------------------------------------------
CREATE TABLE `device_command` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '指令编号',
    `command_code`   VARCHAR(50)  NOT NULL                COMMENT '指令编码（唯一）',
    `device_code`    VARCHAR(50)  NOT NULL                COMMENT '目标设备编码',
    `action`         VARCHAR(50)  NOT NULL                COMMENT '控制动作(TURN_ON/TURN_OFF/SET_LEVEL)',
    `source`         VARCHAR(20)  NOT NULL                COMMENT '控制来源(RULE/REMOTE)',
    `status`         VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT '指令状态(PENDING/SUCCESS/FAILED)',
    `result_message` VARCHAR(255)          DEFAULT NULL   COMMENT '执行结果说明',
    `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `finish_time`    DATETIME              DEFAULT NULL   COMMENT '完成时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_command_code` (`command_code`),
    KEY `idx_command_device_status` (`device_code`, `status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '设备控制指令表';

-- ---------------------------------------------------------------------
-- alarm 告警记录表（归属 S4）
-- ---------------------------------------------------------------------
CREATE TABLE `alarm` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '告警编号',
    `alarm_code`    VARCHAR(50)  NOT NULL                COMMENT '告警编码（唯一）',
    `alarm_type`    VARCHAR(50)  NOT NULL                COMMENT '告警类型(ENV_ABNORMAL/MANUAL_HELP)',
    `device_code`   VARCHAR(50)           DEFAULT NULL   COMMENT '相关设备编码',
    `alarm_content` VARCHAR(255) NOT NULL                COMMENT '告警内容',
    `alarm_time`    DATETIME     NOT NULL                COMMENT '告警发生时间',
    `status`        VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT '处理状态(PENDING/PROCESSING/PROCESSED)',
    `handle_time`   DATETIME              DEFAULT NULL   COMMENT '处理时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_alarm_code` (`alarm_code`),
    KEY `idx_alarm_time` (`alarm_time`),
    KEY `idx_alarm_status` (`status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '告警记录表';
