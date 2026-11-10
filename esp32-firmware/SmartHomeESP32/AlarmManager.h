#ifndef ALARM_MANAGER_H
#define ALARM_MANAGER_H

#include <Arduino.h>

/* 一键求助：按键消抖扫描、蜂鸣器本地提醒、LCD 状态联动。 */
namespace AlarmManager {

// 初始化按键（内部上拉/下拉）与无源蜂鸣器（PWM 音调，先静音）
void begin();

// 非阻塞扫描：按键消抖、触发求助、蜂鸣器状态机、状态自动复位。
// 由主循环每轮调用；仅在"有效按压"时内部会做一次阻塞的 HTTP 请求（最多 5s）。
void tick();

}  // namespace AlarmManager

#endif
