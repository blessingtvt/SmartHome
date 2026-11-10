#ifndef DEVICE_MANAGER_H
#define DEVICE_MANAGER_H

#include <Arduino.h>

/* 执行器控制：风扇、LED、窗户舵机、门舵机。 */
namespace DeviceManager {

// 初始化各执行器（安全态：全关 / 门窗回关闭位）
void begin();

// 执行一条指令：deviceCode + action(TURN_ON/TURN_OFF)。
// 返回 true=执行成功（反馈 SUCCESS），false=未知设备或动作（反馈 FAILED）。
bool execute(const String& deviceCode, const String& action);

}  // namespace DeviceManager

#endif
