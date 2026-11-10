#ifndef SMART_HOME_NET_MANAGER_H
#define SMART_HOME_NET_MANAGER_H

#include <Arduino.h>

/* Wi-Fi 连接与自动重连。
 * 注意：命名空间不能叫 NetworkManager —— ESP32 Arduino core 3.x 内置了
 * class NetworkManager（libraries/Network），会同名冲突，故命名为 NetManager。 */
namespace NetManager {

// 初始化 WiFi（STA 模式 + 自动重连）并发起首次连接（非阻塞，不等待）
void begin();

// 检查连接：已连接返回 true；断开时每 15s 触发一次非阻塞重连，返回 false
bool ensureConnected();

// 当前是否已连接
bool isConnected();

// 本机 IP 字符串（未连接时返回空串）
String localIP();

}  // namespace NetManager

#endif
