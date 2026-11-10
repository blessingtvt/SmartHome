#include "ApiClient.h"
#include "ConfigResolver.h"
#include <HTTPClient.h>

namespace ApiClient {

void reportEnvironment(bool okT, float temperature, bool okH, float humidity, bool pirTriggered, int water) {
  if (strcmp(SERVER_HOST, "CHANGE_ME") == 0) {
    Serial.println(F("[上报] SERVER_HOST 未配置"));
    return;
  }

  DynamicJsonDocument doc(256);
  doc["deviceCode"] = DEVICE_CODE;
  if (okT) doc["temperature"] = temperature;
  if (okH) doc["humidity"] = humidity;
  doc["pirTriggered"] = pirTriggered;
  if (water >= 0) doc["water"] = water;  // 水滴感应原始 ADC 值
  // gasRaw：暂省略（MQ-2 无模拟原始值）；timestamp：暂省略（后端按服务端时间）

  char body[256];
  serializeJson(doc, body);

  HTTPClient http;
  http.setTimeout(5000);
  http.begin(SERVER_HOST, SERVER_PORT, "/api/v1/environment/readings");
  http.addHeader("Content-Type", "application/json");

  Serial.print(F("[上报] "));
  Serial.println(body);

  int httpCode = http.POST(body);
  if (httpCode <= 0) {
    Serial.printf("[上报] HTTP 失败: %s\n", http.errorToString(httpCode).c_str());
    http.end();
    return;
  }
  String payload = http.getString();
  http.end();

  if (httpCode != 200) {
    Serial.printf("[上报] HTTP 状态码异常: %d\n", httpCode);
    return;
  }

  DynamicJsonDocument resp(1024);
  DeserializationError err = deserializeJson(resp, payload);
  if (err) {
    Serial.printf("[上报] 响应解析失败: %s\n", err.c_str());
    return;
  }
  int code = resp["code"] | 0;
  if (code == 200) {
    Serial.println(F("[上报] 成功"));
  } else {
    Serial.printf("[上报] 服务端 code=%d: %s\n", code, resp["message"] | "");
  }
}

bool fetchPending(const String& deviceCode, DynamicJsonDocument& out) {
  if (strcmp(SERVER_HOST, "CHANGE_ME") == 0) {
    Serial.println(F("[指令] SERVER_HOST 未配置"));
    return false;
  }

  HTTPClient http;
  http.setTimeout(5000);
  String path = "/api/v1/device-commands/pending?deviceCode=" + deviceCode;
  http.begin(SERVER_HOST, SERVER_PORT, path.c_str());

  int httpCode = http.GET();
  if (httpCode <= 0) {
    Serial.printf("[指令] pending 查询 HTTP 失败: %s\n", http.errorToString(httpCode).c_str());
    http.end();
    return false;
  }
  String payload = http.getString();
  http.end();

  if (httpCode != 200) {
    Serial.printf("[指令] pending 查询 HTTP 状态码异常: %d\n", httpCode);
    return false;
  }

  DeserializationError err = deserializeJson(out, payload);
  if (err) {
    Serial.printf("[指令] pending 响应解析失败: %s\n", err.c_str());
    return false;
  }
  if ((out["code"] | 0) != 200) {
    Serial.printf("[指令] pending 服务端 code=%d: %s\n", out["code"] | 0, out["message"] | "");
    return false;
  }
  return true;
}

bool reportCommandResult(long commandId, const String& deviceCode,
                         const String& status, const String& resultMessage) {
  if (strcmp(SERVER_HOST, "CHANGE_ME") == 0) {
    Serial.println(F("[反馈] SERVER_HOST 未配置"));
    return false;
  }

  DynamicJsonDocument doc(256);
  doc["deviceCode"] = deviceCode;
  doc["status"] = status;
  if (resultMessage.length() > 0) doc["resultMessage"] = resultMessage;

  char body[256];
  serializeJson(doc, body);

  HTTPClient http;
  http.setTimeout(5000);
  String path = "/api/v1/device-commands/" + String(commandId) + "/result";
  http.begin(SERVER_HOST, SERVER_PORT, path.c_str());
  http.addHeader("Content-Type", "application/json");

  int httpCode = http.POST(body);
  if (httpCode <= 0) {
    Serial.printf("[反馈] HTTP 失败: %s\n", http.errorToString(httpCode).c_str());
    http.end();
    return false;
  }
  String payload = http.getString();
  http.end();

  if (httpCode != 200) {
    Serial.printf("[反馈] HTTP 状态码异常: %d\n", httpCode);
    return false;
  }

  DynamicJsonDocument resp(1024);
  DeserializationError err = deserializeJson(resp, payload);
  if (err) {
    Serial.printf("[反馈] 响应解析失败: %s\n", err.c_str());
    return false;
  }
  int code = resp["code"] | 0;
  Serial.printf("[反馈] code=%d: %s\n", code, resp["message"] | "");
  return code == 200;
}

HelpResult requestHelp() {
  if (strcmp(SERVER_HOST, "CHANGE_ME") == 0) {
    Serial.println(F("[求助] SERVER_HOST 未配置"));
    return HELP_NETWORK_ERROR;
  }

  DynamicJsonDocument doc(128);
  doc["deviceCode"] = DEVICE_CODE;
  // content 省略：服务端使用默认文案"老人一键求助"

  char body[128];
  serializeJson(doc, body);

  HTTPClient http;
  http.setTimeout(5000);
  http.begin(SERVER_HOST, SERVER_PORT, "/api/v1/alarms/help");
  http.addHeader("Content-Type", "application/json");

  Serial.print(F("[求助] "));
  Serial.println(body);

  int httpCode = http.POST(body);
  if (httpCode <= 0) {
    // 网络层失败（连接被拒 / 超时 / DNS 失败等）：请求未送达，绝不误报成功
    Serial.printf("[求助] 网络异常，未送达服务端: %s\n", http.errorToString(httpCode).c_str());
    http.end();
    return HELP_NETWORK_ERROR;
  }
  String payload = http.getString();
  http.end();

  if (httpCode != 200) {
    Serial.printf("[求助] HTTP 状态码异常: %d\n", httpCode);
    return HELP_SERVER_REJECT;
  }

  DynamicJsonDocument resp(1024);
  DeserializationError err = deserializeJson(resp, payload);
  if (err) {
    Serial.printf("[求助] 响应解析失败: %s\n", err.c_str());
    return HELP_SERVER_REJECT;
  }
  int code = resp["code"] | 0;
  if (code == 200) {
    Serial.println(F("[求助] 成功受理"));
    return HELP_OK;
  }
  Serial.printf("[求助] 服务端 code=%d: %s\n", code, resp["message"] | "");
  return HELP_SERVER_REJECT;
}

bool reportHeartbeat() {
  if (strcmp(SERVER_HOST, "CHANGE_ME") == 0) {
    Serial.println(F("[心跳] SERVER_HOST 未配置"));
    return false;
  }

  HTTPClient http;
  http.setTimeout(5000);
  String path = "/api/v1/devices/" + String(DEVICE_CODE) + "/heartbeat";
  http.begin(SERVER_HOST, SERVER_PORT, path.c_str());

  int httpCode = http.POST("");  // 无请求体
  if (httpCode <= 0) {
    Serial.printf("[心跳] HTTP 失败: %s\n", http.errorToString(httpCode).c_str());
    http.end();
    return false;
  }
  String payload = http.getString();
  http.end();

  if (httpCode != 200) {
    Serial.printf("[心跳] HTTP 状态码异常: %d\n", httpCode);
    return false;
  }

  DynamicJsonDocument resp(512);
  DeserializationError err = deserializeJson(resp, payload);
  if (err) {
    Serial.printf("[心跳] 响应解析失败: %s\n", err.c_str());
    return false;
  }
  int code = resp["code"] | 0;
  if (code == 200) {
    Serial.println(F("[心跳] 成功"));
    return true;
  }
  Serial.printf("[心跳] 服务端 code=%d: %s\n", code, resp["message"] | "");
  return false;
}

}  // namespace ApiClient
