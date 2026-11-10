#include "SensorManager.h"
#include "ConfigResolver.h"
#include <DHT.h>

// XHT11 按 DHT11 协议读取；若模块实为 DHT22 或其它型号，改这里的类型参数即可
static DHT dht(PIN_XHT11, DHT11);

namespace SensorManager {

void begin() {
  dht.begin();
  pinMode(PIN_PIR, INPUT);
  pinMode(PIN_MQ2, INPUT_PULLUP);  // MQ-2 DOUT 多为开漏比较器输出，加内部上拉
  pinMode(PIN_WATER, INPUT);       // GPIO34 输入专用、无内部上拉（analogRead 用）
}

bool readTemperature(float &out) {
  float t = dht.readTemperature();
  if (isnan(t)) return false;
  out = t;
  return true;
}

bool readHumidity(float &out) {
  float h = dht.readHumidity();
  if (isnan(h)) return false;
  out = h;
  return true;
}

bool readPir() {
  return digitalRead(PIN_PIR) == HIGH;
}

bool readGasThreshold() {
  // 常见模块：超过阈值时 DOUT 拉 LOW
  return digitalRead(PIN_MQ2) == LOW;
}

int readWater() {
  return analogRead(PIN_WATER);  // 0~4095 原始 ADC 值
}

}  // namespace SensorManager
