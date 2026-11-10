/*
 * 面向老年人的智能家居主动感知系统 —— ESP32 固件
 * 子任务 7-02（环境上报）+ 7-03（指令执行）+ 7-04（一键求助）+ 7-05（心跳与稳定性）
 *
 * 7-05 实现：
 *   - 每 30s 上报设备心跳 POST /api/v1/devices/{deviceCode}/heartbeat
 *   - Wi-Fi 非阻塞连接/重连（去掉原来最多 15s 的阻塞等待）
 *   - 指令去重：内存环形缓存记录本会话已执行 commandId，
 *     避免"物理动作已执行但结果反馈失败"后再次执行；重试仅重发反馈。
 *   - 明确局限：去重缓存在内存中，重启后失效，非持久化 exactly-once。
 *
 * 依赖库：DHT sensor library、ArduinoJson(6.x)、ESP32Servo、LiquidCrystal_I2C
 */

#include "ConfigResolver.h"
#include "NetworkManager.h"
#include "SensorManager.h"
#include "DeviceManager.h"
#include "DisplayManager.h"
#include "AlarmManager.h"
#include "ApiClient.h"

const unsigned long REPORT_INTERVAL_MS = 30000;     // 环境上报周期
const unsigned long POLL_INTERVAL_MS = 5000;        // 指令轮询周期
const unsigned long HEARTBEAT_INTERVAL_MS = 30000;  // 心跳周期（< 后端离线阈值 90s）

// 需要轮询待执行指令的执行器设备编码（须与 device 表一致）
const char* ACTUATOR_CODES[] = { DEVICE_FAN, DEVICE_LED, DEVICE_WINDOW, DEVICE_DOOR };
const int ACTUATOR_COUNT = sizeof(ACTUATOR_CODES) / sizeof(ACTUATOR_CODES[0]);

// ---- 指令去重缓存（内存，第一版） ----
// 记录本会话已执行过的 commandId 及其结果；环形覆盖。
// 作用：反馈失败时指令仍是 PENDING，下次轮询会再次出现——命中缓存则跳过物理执行、只重发反馈。
// 局限：重启即清空；容量有限（满后最旧被覆盖）。非持久化，不保证跨重启 exactly-once。
struct ExecutedEntry {
  long commandId;
  char status[8];  // "SUCCESS" / "FAILED"
};
const int EXECUTED_CACHE_SIZE = 16;
ExecutedEntry executedCache[EXECUTED_CACHE_SIZE];
int executedCacheHead = 0;

unsigned long lastReportMs = 0;
unsigned long lastPollMs = 0;
unsigned long lastHeartbeatMs = 0;

void setup() {
  Serial.begin(115200);
  delay(200);
  Serial.println(F(""));
  Serial.println(F("===== 智能家居 ESP32 (7-02~7-05) ====="));
  SensorManager::begin();
  DeviceManager::begin();
  DisplayManager::begin();
  AlarmManager::begin();
  NetManager::begin();   // 非阻塞：仅发起连接，后台自动重连
  DisplayManager::showStatus("Idle");
}

void loop() {
  bool connected = NetManager::ensureConnected();

  if (connected) {
    unsigned long now = millis();
    if (now - lastReportMs >= REPORT_INTERVAL_MS) {
      lastReportMs = now;
      reportEnvironment();
    }
    if (now - lastPollMs >= POLL_INTERVAL_MS) {
      lastPollMs = now;
      pollCommands();
    }
    if (now - lastHeartbeatMs >= HEARTBEAT_INTERVAL_MS) {
      lastHeartbeatMs = now;
      ApiClient::reportHeartbeat();
    }
  }

  // 按键扫描 + 蜂鸣器状态机：无论联网与否都运行
  AlarmManager::tick();

  if (!connected) {
    delay(500);  // 未联网降速，避免空转（WiFi 在后台自动重连）
  }
}

void reportEnvironment() {
  float temperature = 0, humidity = 0;
  bool okT = SensorManager::readTemperature(temperature);
  bool okH = SensorManager::readHumidity(humidity);
  bool pir = SensorManager::readPir();
  int water = SensorManager::readWater();
  if (!okT) Serial.println(F("[采集] 温度读取失败"));
  if (!okH) Serial.println(F("[采集] 湿度读取失败"));
  DisplayManager::showEnv(temperature, okT, humidity, okH);
  ApiClient::reportEnvironment(okT, temperature, okH, humidity, pir, water);
}

void pollCommands() {
  for (int i = 0; i < ACTUATOR_COUNT; i++) {
    handlePending(String(ACTUATOR_CODES[i]));
    delay(300);  // 拉散连接，避免瞬间并发导致 connection refused
  }
}

// 查 commandId 是否已执行过，返回其结果状态；未找到返回空串
const char* findExecuted(long commandId) {
  for (int i = 0; i < EXECUTED_CACHE_SIZE; i++) {
    if (executedCache[i].commandId == commandId) return executedCache[i].status;
  }
  return "";
}

// 记录一条已执行指令（环形覆盖最旧）
void rememberExecuted(long commandId, const char* status) {
  executedCache[executedCacheHead].commandId = commandId;
  strncpy(executedCache[executedCacheHead].status, status, sizeof(executedCache[0].status) - 1);
  executedCache[executedCacheHead].status[sizeof(executedCache[0].status) - 1] = '\0';
  executedCacheHead = (executedCacheHead + 1) % EXECUTED_CACHE_SIZE;
}

void handlePending(const String& deviceCode) {
  DynamicJsonDocument doc(4096);
  if (!ApiClient::fetchPending(deviceCode, doc)) return;

  JsonArray arr = doc["data"].as<JsonArray>();
  if (arr.isNull()) return;

  for (JsonVariant cmd : arr) {
    long commandId = cmd["id"].as<long>();
    const char* action = cmd["action"] | "";
    if (commandId <= 0 || strlen(action) == 0) continue;

    const char* cached = findExecuted(commandId);
    const char* status;
    const char* msg;
    if (strlen(cached) > 0) {
      // 已执行过（上次反馈可能失败）：跳过物理执行，仅重发反馈
      status = cached;
      msg = "已执行，重试反馈";
      Serial.printf("[指令] deviceCode=%s commandId=%ld 已执行过，重发反馈(=%s)\n",
                    deviceCode.c_str(), commandId, status);
    } else {
      Serial.printf("[指令] 执行 deviceCode=%s action=%s commandId=%ld\n",
                    deviceCode.c_str(), action, commandId);
      bool ok = DeviceManager::execute(deviceCode, action);
      status = ok ? "SUCCESS" : "FAILED";
      msg = ok ? "执行成功" : "未知设备或动作";
      rememberExecuted(commandId, status);
    }
    ApiClient::reportCommandResult(commandId, deviceCode, status, msg);
  }
}
