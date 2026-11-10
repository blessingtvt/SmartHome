-- =====================================================================
-- 增量迁移：阶段五 内置自动化规则种子
-- 日期：2026-11-10
--
-- 预置 3 条内置规则（pir_light 已在阶段四入库，本脚本只补后两条）：
--   1) temperature_fan：温度 > 30℃ → FAN_001 开启（TURN_ON）
--   2) water_window：   水滴原始值 >= 100 连续确认 3 次 → WINDOW_001 关闭（TURN_OFF）
--
-- 幂等：仅当同名规则不存在时插入，可重复执行。
-- 严禁对已上线数据库重新执行 schema.sql 的 DROP 部分。
-- =====================================================================

SET NAMES utf8mb4;

INSERT INTO `automation_rule` (`rule_name`, `trigger_type`, `condition_config`, `action_config`, `enabled`)
SELECT 'temperature_fan', 'TEMPERATURE',
       '{"metric":"temperature","operator":">","threshold":30}',
       '{"actionType":"DEVICE_COMMAND","deviceCode":"FAN_001","action":"TURN_ON"}',
       1
WHERE NOT EXISTS (SELECT 1 FROM `automation_rule` WHERE `rule_name` = 'temperature_fan');

INSERT INTO `automation_rule` (`rule_name`, `trigger_type`, `condition_config`, `action_config`, `enabled`)
SELECT 'water_window', 'WATER',
       '{"metric":"water","operator":">=","threshold":100,"confirmCount":3}',
       '{"actionType":"DEVICE_COMMAND","deviceCode":"WINDOW_001","action":"TURN_OFF"}',
       1
WHERE NOT EXISTS (SELECT 1 FROM `automation_rule` WHERE `rule_name` = 'water_window');
