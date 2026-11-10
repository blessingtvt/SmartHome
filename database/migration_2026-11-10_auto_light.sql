-- =====================================================================
-- 增量迁移：阶段四 pir_light 自动照明
-- 日期：2026-11-10
--
-- 1) rule_execution_log 新增 device_exec_result 列（区分「命令创建成功」与「设备实际执行成功」）
-- 2) 新增 auto_light_state 表（S2 归属，持久化每个照明设备的最近 PIR 活动时间）
--
-- 说明：本脚本为增量迁移，可重复执行（IF NOT EXISTS / IF COLUMN 不存在才加）。
-- 严禁对已上线数据库重新执行 schema.sql 的 DROP 部分。
-- =====================================================================

SET NAMES utf8mb4;

-- 1) 规则执行日志：新增设备实际执行结果列（仅当列不存在时添加）
-- MySQL 8 支持 ADD COLUMN IF NOT EXISTS 之外的判空方式：这里用存储过程式预判不必要，
-- 直接提供 ALTER；重复执行会报 Duplicate column，属预期。若需幂等请在客户端判断。
ALTER TABLE `rule_execution_log`
    ADD COLUMN `device_exec_result` VARCHAR(20) DEFAULT NULL COMMENT '设备实际执行结果(SUCCESS/FAILED，PENDING不回填)' AFTER `failure_reason`;

-- 2) 自动照明状态表（归属 S2）
CREATE TABLE IF NOT EXISTS `auto_light_state` (
    `id`                 BIGINT       NOT NULL AUTO_INCREMENT COMMENT '记录编号',
    `device_code`        VARCHAR(50)  NOT NULL                COMMENT '照明设备编码',
    `last_activity_time` DATETIME              DEFAULT NULL   COMMENT '最近一次 PIR 活动时间',
    `update_time`        DATETIME     NOT NULL                COMMENT '最后更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_auto_light_device` (`device_code`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '自动照明状态表(归属 S2)';
