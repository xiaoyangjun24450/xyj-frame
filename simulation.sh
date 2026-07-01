#!/usr/bin/env bash
# 实时查看本应用进程日志（需先在手机上启动应用）
PACKAGE="com.pku.acematetennis"
ADB="${ADB:-${ANDROID_HOME:-$HOME/Android/Sdk}/platform-tools/adb}"

PID="$("$ADB" shell pidof -s "$PACKAGE" 2>/dev/null | tr -d '\r')"
if [[ -z "$PID" ]]; then
  echo "未找到进程: $PACKAGE。请先在真机上打开该应用。" >&2
  exit 1
fi

# *:I = 全局最低级别为 Info（不显示 Verbose / Debug）
exec "$ADB" logcat --pid="$PID" '*:I'
