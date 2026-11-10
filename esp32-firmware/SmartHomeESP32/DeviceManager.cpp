#include "DeviceManager.h"
#include "ConfigResolver.h"
#include <ESP32Servo.h>

static Servo windowServo;
static Servo doorServo;
// 风扇 PWM 用 ESP32PWM（与舵机共用同一套通道分配器），
// 避免直接 ledcAttach(自动分配) 与 ESP32Servo 内部 ledcAttachChannel(显式占通道) 抢通道 0。
static ESP32PWM fanPwm;

namespace DeviceManager {

void begin() {
  // 风扇（HR1124S 单通道 H 桥）：
  //   IN-/B（GPIO18）做 PWM 定速，IN+/A（GPIO19）拉低控方向。
  //   之前 IN+/A 做 PWM 时风扇反转，故换到 B 脚，实现正向。
  pinMode(PIN_FAN_A, OUTPUT);   // IN+/A 数字输出，固定低电平
  digitalWrite(PIN_FAN_A, LOW);
  fanPwm.attachPin(PIN_FAN_B, FAN_PWM_FREQ, 8);  // IN-/B PWM，8 位分辨率
  fanPwm.write(0);

  // 黄色 LED
  pinMode(PIN_LED, OUTPUT);
  digitalWrite(PIN_LED, LOW);

  // 蜂鸣器已改由 AlarmManager 负责（7-04 一键求助本地提醒）

  // 舵机：保持 attach 以维持门窗位置
  windowServo.attach(PIN_SERVO_WIN);
  doorServo.attach(PIN_SERVO_DOOR);
  windowServo.write(WINDOW_CLOSE_ANGLE);
  doorServo.write(DOOR_CLOSE_ANGLE);
}

static void fan(bool on) {
  if (on) {
    digitalWrite(PIN_FAN_A, LOW);   // IN+/A 低
    fanPwm.write(FAN_DUTY);         // IN-/B PWM → 正向慢速
  } else {
    digitalWrite(PIN_FAN_A, LOW);
    fanPwm.write(0);                // 停止
  }
}

static void led(bool on) {
  digitalWrite(PIN_LED, on ? HIGH : LOW);
}

static void window(bool open) {
  windowServo.write(open ? WINDOW_OPEN_ANGLE : WINDOW_CLOSE_ANGLE);
}

static void door(bool open) {
  doorServo.write(open ? DOOR_OPEN_ANGLE : DOOR_CLOSE_ANGLE);
}

bool execute(const String& deviceCode, const String& action) {
  bool on;
  if (action == "TURN_ON") on = true;
  else if (action == "TURN_OFF") on = false;
  else return false;  // 不支持的指令动作

  if (deviceCode == DEVICE_FAN)    { fan(on);    return true; }
  if (deviceCode == DEVICE_LED)    { led(on);    return true; }
  if (deviceCode == DEVICE_WINDOW) { window(on); return true; }
  if (deviceCode == DEVICE_DOOR)   { door(on);   return true; }
  return false;  // 未知设备编码
}

}  // namespace DeviceManager
