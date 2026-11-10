#include "AlarmManager.h"
#include "ConfigResolver.h"
#include "DisplayManager.h"
#include "ApiClient.h"
#include <ESP32Servo.h>   // 提供 ESP32PWM，与舵机/风扇共用通道分配器，避免抢 LEDC 通道

static ESP32PWM buzzerPwm;

// ---- 按键消抖状态 ----
static int  lastReading   = HIGH;   // 空闲电平（active LOW 时为 HIGH）
static unsigned long lastChangeMs = 0;
static int  stableState   = HIGH;
static int  idleLevel     = HIGH;   // 未按下时的电平，由 begin() 按接线方式设定

// ---- 蜂鸣器状态机 ----
static int  beepRemaining = 0;
static bool beepOn        = false;
static unsigned long beepPhaseMs = 0;
static int  beepOnMs      = 0;
static int  beepOffMs     = 0;

// ---- 求助结果停留后自动复位 ----
static unsigned long statusResetMs = 0;
static const unsigned long HELP_STATUS_HOLD_MS = 5000;  // 结果停留 5 秒后回"待命"

namespace AlarmManager {

void begin() {
#if HELP_BUTTON_ACTIVE_LOW
  pinMode(PIN_BTN_HELP, INPUT_PULLUP);
  idleLevel = HIGH;
#else
  pinMode(PIN_BTN_HELP, INPUT_PULLDOWN);
  idleLevel = LOW;
#endif
  lastReading = idleLevel;
  stableState = idleLevel;

  // 无源蜂鸣器：attach 一个 PWM 音调（先静音）
  buzzerPwm.attachPin(PIN_BUZZER, BUZZER_TONE_HZ, 8);
  buzzerPwm.write(0);
}

// 按键消抖：返回 true 表示"刚刚稳定按下一次"（单次按压只触发一次）
static bool readButton() {
  int reading = digitalRead(PIN_BTN_HELP);
  if (reading != lastReading) {
    lastChangeMs = millis();
    lastReading = reading;
  }
  if (millis() - lastChangeMs > BUTTON_DEBOUNCE_MS) {
    if (reading != stableState) {
      stableState = reading;
      if (stableState != idleLevel) return true;  // 稳定为按下电平
    }
  }
  return false;
}

// 启动一段蜂鸣：times 次，每次 onMs 响 / offMs 停
static void startBeep(int times, int onMs, int offMs) {
  beepRemaining = times;
  beepOnMs = onMs;
  beepOffMs = offMs;
  beepOn = true;
  buzzerPwm.write(BUZZER_DUTY);
  beepPhaseMs = millis();
}

// 蜂鸣器状态机：非阻塞推进
static void tickBeep() {
  if (beepRemaining <= 0) return;
  unsigned long now = millis();
  if (beepOn) {
    if (now - beepPhaseMs >= (unsigned long)beepOnMs) {
      buzzerPwm.write(0);
      beepOn = false;
      beepPhaseMs = now;
    }
  } else {
    if (now - beepPhaseMs >= (unsigned long)beepOffMs) {
      beepRemaining--;
      if (beepRemaining <= 0) {
        buzzerPwm.write(0);          // 结束，静音
      } else {
        buzzerPwm.write(BUZZER_DUTY);
        beepOn = true;
        beepPhaseMs = now;
      }
    }
  }
}

// 处理一次有效按压：发送求助并据结果联动 LCD + 蜂鸣器
static void handleHelpPress() {
  DisplayManager::showStatus("Sending help...");
  HelpResult r = ApiClient::requestHelp();

  switch (r) {
    case HELP_OK:
      DisplayManager::showStatus("Help received");
      startBeep(2, 100, 100);   // 两短声 = 成功
      break;
    case HELP_NETWORK_ERROR:
      DisplayManager::showStatus("No network!");
      startBeep(3, 250, 150);   // 三长声 = 失败
      break;
    case HELP_SERVER_REJECT:
      DisplayManager::showStatus("Server reject");
      startBeep(3, 250, 150);
      break;
  }
  statusResetMs = millis() + HELP_STATUS_HOLD_MS;
}

void tick() {
  if (readButton()) {
    handleHelpPress();
  }

  tickBeep();

  // 结果停留数秒后自动回 Idle
  if (statusResetMs != 0 && millis() >= statusResetMs) {
    statusResetMs = 0;
    DisplayManager::showStatus("Idle");
  }
}

}  // namespace AlarmManager
