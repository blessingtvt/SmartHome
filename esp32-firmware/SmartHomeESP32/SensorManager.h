#ifndef SENSOR_MANAGER_H
#define SENSOR_MANAGER_H

#include <Arduino.h>

/* 传感器采集：温湿度、PIR、MQ-2（数字阈值）、水滴（模拟 ADC）。 */
namespace SensorManager {

// 初始化各传感器
void begin();

// 温度(℃)：成功返回 true 并写入 out，失败返回 false
bool readTemperature(float &out);

// 湿度(%)：成功返回 true 并写入 out，失败返回 false
bool readHumidity(float &out);

// PIR：true = 检测到活动，false = 无活动（不是跌倒检测）
bool readPir();

// MQ-2 数字阈值：true = 超过阈值（DOUT 拉低），false = 未超过阈值
// 注意：仅为数字阈值，非真实燃气浓度；模拟原始值需 AOUT 接带 ADC 的引脚。
bool readGasThreshold();

// 水滴感应：返回 S 引脚模拟原始值（0~4095，ADC1，GPIO34）。
// 注意：这是原始 ADC 读数，不是降雨量(mm)；阈值判断（如 >1500 视为有雨）由后端规则负责。
int readWater();

}  // namespace SensorManager

#endif
