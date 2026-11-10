#ifndef API_CLIENT_H
#define API_CLIENT_H

#include <Arduino.h>
#include <ArduinoJson.h>

/* 一键求助结果分类（用于本地提示与避免误报成功）。 */
enum HelpResult {
  HELP_OK = 0,            // 服务端受理成功
  HELP_NETWORK_ERROR = 1, // 网络/HTTP 失败，请求未送达服务端
  HELP_SERVER_REJECT = 2  // 已送达服务端，但业务 code != 200
};

/* HTTP 请求与 JSON 序列化（走网关 8080）。 */
namespace ApiClient {

// API01：上报环境数据。gasRaw / timestamp 暂省略（可空，后端按空处理）。
// water 为水滴感应原始 ADC 值（0~4095）；未接入时传 -1 由调用方省略。
void reportEnvironment(bool okT, float temperature, bool okH, float humidity, bool pirTriggered, int water);

// API02：拉取某设备的待执行指令。成功返回 true，并把整个响应解析进 out（调用方读 out["data"]）。
bool fetchPending(const String& deviceCode, DynamicJsonDocument& out);

// API03：上报指令执行结果（SUCCESS / FAILED）。返回 true=服务端确认（code 200）。
// 注：后端对相同状态幂等（重复上报同一结果仍返回 200），可安全重试。
bool reportCommandResult(long commandId, const String& deviceCode,
                         const String& status, const String& resultMessage);

// API04：一键求助。返回结果分类，供本地提示与避免误报成功。
HelpResult requestHelp();

// API05：设备心跳（每 30s）。返回 true=服务端确认在线。无请求体。
bool reportHeartbeat();

}  // namespace ApiClient

#endif
