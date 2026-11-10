#include "DisplayManager.h"
#include "ConfigResolver.h"
#include <Wire.h>
#include <LiquidCrystal_I2C.h>

static LiquidCrystal_I2C lcd(LCD_ADDR, LCD_COLS, LCD_ROWS);
static String lastLine0 = "";
static String lastLine1 = "";

namespace DisplayManager {

void begin() {
  // 先按配置引脚初始化 I2C；LCD_ADDR/COLS/ROWS 见 config。
  // 注：LiquidCrystal_I2C 库内部 lcd.init() 也会 Wire.begin()，
  //     默认 21/22，与本次确认的接线一致，故无冲突。
  Wire.begin(PIN_LCD_SDA, PIN_LCD_SCL);
  lcd.init();
  lcd.backlight();
  lcd.clear();
  lastLine0 = "";
  lastLine1 = "";
  showStatus("Booting...");
}

// 写某一行；内容未变化则不重写，避免刷新闪烁
static void writeLine(int row, const String& text) {
  String& last = (row == 0) ? lastLine0 : lastLine1;
  if (text == last) return;
  last = text;
  lcd.setCursor(0, row);
  lcd.print(text);
  // 用空格补齐，擦掉上一帧残留字符
  for (int i = text.length(); i < LCD_COLS; i++) lcd.print(' ');
}

void showEnv(float temperature, bool okT, float humidity, bool okH) {
  String line = "T:";
  line += okT ? String(temperature, 1) : String("--");
  if (okT) line += "C";
  line += " H:";
  line += okH ? String(humidity, 0) : String("--");
  if (okH) line += "%";
  writeLine(0, line);
}

void showStatus(const String& status) {
  writeLine(1, status);
}

}  // namespace DisplayManager
