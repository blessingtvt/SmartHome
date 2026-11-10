#include "NetworkManager.h"
#include "ConfigResolver.h"
#include <WiFi.h>

namespace NetManager {

static bool wasConnected = false;
static unsigned long lastReconnectMs = 0;
static const unsigned long RECONNECT_INTERVAL_MS = 15000;

void begin() {
  WiFi.mode(WIFI_STA);
  // 后台自动重连已开启；begin() 发起首次连接（非阻塞，不等待结果）
  WiFi.setAutoReconnect(true);
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  Serial.print(F("连接 Wi-Fi: "));
  Serial.println(WIFI_SSID);
  wasConnected = false;
  lastReconnectMs = 0;
}

bool ensureConnected() {
  if (WiFi.status() == WL_CONNECTED) {
    if (!wasConnected) {
      wasConnected = true;
      Serial.print(F("已连接, 本机 IP = "));
      Serial.println(WiFi.localIP());
    }
    return true;
  }

  if (wasConnected) {
    wasConnected = false;
    Serial.println(F("Wi-Fi 已断开，后台自动重连中..."));
  }
  // 非阻塞重连：每 15s 触发一次强制重连，其余时间交给 IDF 后台自动重连。
  // 不再用 while 阻塞等待，避免卡死主循环。
  if (millis() - lastReconnectMs >= RECONNECT_INTERVAL_MS) {
    lastReconnectMs = millis();
    WiFi.disconnect();
    WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  }
  return false;
}

bool isConnected() {
  return WiFi.status() == WL_CONNECTED;
}

String localIP() {
  return isConnected() ? WiFi.localIP().toString() : String("");
}

}  // namespace NetManager
