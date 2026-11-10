#ifndef DISPLAY_MANAGER_H
#define DISPLAY_MANAGER_H

#include <Arduino.h>

/* LCD1602（I2C）显示：第一行温湿度，第二行状态。 */
namespace DisplayManager {

// 初始化 LCD（I2C 引脚、地址、背光、清屏）
void begin();

// 更新第一行：温度/湿度（读取失败显示 "--"）
void showEnv(float temperature, bool okT, float humidity, bool okH);

// 更新第二行：状态文本（LCD1602 仅支持 ASCII，用 "Idle"/"Help received"/"No network!" 等）
void showStatus(const String& status);

}  // namespace DisplayManager

#endif
