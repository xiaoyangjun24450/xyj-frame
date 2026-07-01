#!/usr/bin/env bash
# WSL 下实时查看本应用进程日志（需先在手机上启动应用）
PACKAGE="com.xyj.focuspod"
ADB="${ADB:-/mnt/f/Android/platform-tools/adb.exe}"

PID="$("$ADB" shell pidof -s "$PACKAGE" 2>/dev/null | tr -d '\r')"
if [[ -z "$PID" ]]; then
  echo "未找到进程: $PACKAGE。请先在真机上打开该应用。" >&2
  exit 1
fi

# WSL 调 Windows .exe 时 Ctrl+C 不一定能直接传给 adb.exe。
# 这里让 logcat 在后台运行，并在脚本收到退出信号时主动结束它。
LOGCAT_PID=""
cleanup() {
  if [[ -n "$LOGCAT_PID" ]]; then
    kill "$LOGCAT_PID" 2>/dev/null || true
  fi
}
trap cleanup INT TERM EXIT

# *:I = 全局最低级别为 Info（不显示 Verbose / Debug）
"$ADB" logcat --pid="$PID" '*:I' &
LOGCAT_PID="$!"
wait "$LOGCAT_PID"
