/*
 * 统一配置加载入口。
 *
 * 优先加载用户的真实配置 config.h（含真实 Wi-Fi 密码，已被 .gitignore 忽略）；
 * 若 config.h 不存在（例如刚 clone 仓库），则回退加载 config.example.h（占位值）。
 *
 * 所有 .ino / .cpp 统一 #include "ConfigResolver.h" 即可拿到全部宏定义。
 */
#ifndef CONFIG_RESOLVER_H
#define CONFIG_RESOLVER_H

#if __has_include("config.h")
  #include "config.h"
#else
  #include "config.example.h"
#endif

#endif
