/*
 * 智能家居 ESP32 固件 —— 配置模板
 *
 * 使用方法：
 *   1. 复制本文件为同目录下的 config.h
 *   2. 在 config.h 中填入你的真实值（Wi-Fi 密码、局域网 IP、设备编码）
 *   3. config.h 已被 .gitignore 忽略，真实密码不会进入版本库
 *
 * 若暂未创建 config.h，程序会自动回退加载本模板（占位值），
 * 此时 Wi-Fi 必然连不上，仅用于“先编译通过”。
 *
 * 必须由你填写 / 确认的项：
 *   WIFI_SSID      Wi-Fi 名称（务必 2.4GHz，ESP32 不支持 5GHz）
 *   WIFI_PASSWORD  Wi-Fi 密码
 *   SERVER_HOST    电脑局域网 IPv4 地址（不能是 127.0.0.1！）
 *   SERVER_PORT    网关端口（默认 8080）
 *   DEVICE_CODE    设备编码，必须与数据库 device 表已有记录一致（如 ESP32_001）
 */

#ifndef SMART_HOME_CONFIG_H
#define SMART_HOME_CONFIG_H

/* ---------------- 网络 ---------------- */
#define WIFI_SSID     "你的WiFi名"          // 2.4GHz
#define WIFI_PASSWORD "你的WiFi密码"

/* 网关地址：7-01 仅测试用不到，7-02 环境上报时使用 */
#define SERVER_HOST   "192.168.1.100"       // 改成电脑局域网 IPv4
#define SERVER_PORT   8080

/* ---------------- 设备编码 ---------------- */
#define DEVICE_CODE   "ESP32_001"           // 与数据库 device 表记录一致

/* ---------------- 引脚定义 ----------------
 * 阶段七 4.2 原接线方案（未经你确认，不修改任何引脚）。
 * 已知并已由你确认接受的风险点：
 *   GPIO5（窗户舵机）、GPIO12（LED）、GPIO4（按键二）为启动绑带脚；
 *   GPIO23 无 ADC（MQ-2 只能读数字阈值，不能读模拟原始值）；
 *   GPIO34（水滴）输入专用、无内部上拉。
 */
#define PIN_FAN_A      19   // 130 电机风扇输入 A（需驱动模块，禁止直连 GPIO）
#define PIN_FAN_B      18   // 130 电机风扇输入 B
#define PIN_MQ2        23   // MQ-2 数字输出 DOUT
#define PIN_SERVO_WIN  5    // 窗户舵机
#define PIN_SERVO_DOOR 13   // 门舵机
#define PIN_LED        12   // 黄色 LED
#define PIN_PIR        14   // PIR 人体活动（数字输出）
#define PIN_VOICE_A    16   // 智能语音模块（后续扩展，7-01 未用）
#define PIN_VOICE_B    27   // 智能语音模块（后续扩展，7-01 未用）
#define PIN_XHT11      17   // 温湿度数据脚（XHT11/DHT11）
#define PIN_BUZZER     25   // 蜂鸣器
#define PIN_SK6812     26   // SK6812 RGB（后续扩展，7-01 未用）
#define PIN_WATER      34   // 水滴传感器（后续扩展，7-01 未用）
#define PIN_BTN_HELP   32   // 按键一：一键求助
#define PIN_BTN2       4    // 按键二：后续扩展

/* ---------------- 可选功能开关 ---------------- */
/* MQ-2 数字阈值测试开关：拿不到 MQ-2 数据时改为 0，gas 命令会提示跳过 */
#define ENABLE_MQ2     1

/* ---------------- 执行器设备编码（7-03，须与 device 表一致） ---------------- */
#define DEVICE_FAN     "FAN_001"
#define DEVICE_LED     "LED_001"
#define DEVICE_WINDOW  "WINDOW_001"
#define DEVICE_DOOR    "DOOR_001"

/* ---------------- 执行器参数（7-03） ---------------- */
#define FAN_PWM_FREQ       5000
#define FAN_DUTY           180
#define WINDOW_OPEN_ANGLE  176
#define WINDOW_CLOSE_ANGLE 0
#define DOOR_OPEN_ANGLE    180
#define DOOR_CLOSE_ANGLE   0

/* ---------------- 一键求助与 LCD（7-04） ---------------- */
#define PIN_LCD_SDA    21   // LCD1602 I2C SDA（ESP32 默认 I2C 引脚）
#define PIN_LCD_SCL    22   // LCD1602 I2C SCL（ESP32 默认 I2C 引脚）
#define LCD_ADDR       0x27 // I2C 地址（Keyes 1602 常见 0x27；若无显示可试 0x3F）
#define LCD_COLS       16
#define LCD_ROWS       2

#define HELP_BUTTON_ACTIVE_LOW 1   // 1=按下接 GND（内部上拉）；0=按下接 3.3V（内部下拉）
#define BUTTON_DEBOUNCE_MS  50     // 按键消抖时间 ms

#define BUZZER_TONE_HZ     3000    // 无源蜂鸣器音调频率 Hz
#define BUZZER_DUTY        128     // 占空比 0~255（128=50%）

#endif
