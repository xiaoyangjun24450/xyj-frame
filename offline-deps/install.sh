#!/usr/bin/env bash
set -euo pipefail

# 本脚本不会自动下载 Android SDK，只负责在 SDK 已安装后写入 local.properties。

# 当前脚本所在目录，即 offline-deps。
SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
# 项目根目录。
PROJECT_DIR="$(cd -- "$SCRIPT_DIR/.." && pwd)"

# 离线下载好的 Gradle wrapper 分发包。
GRADLE_ZIP="$SCRIPT_DIR/gradle-8.14.3-all.zip"
# Gradle wrapper 的配置文件，distributionUrl 在这里定义。
WRAPPER_PROPERTIES="$PROJECT_DIR/gradle/wrapper/gradle-wrapper.properties"

# 没有 Gradle zip 时直接失败，避免 wrapper 回退到网络下载。
if [[ ! -f "$GRADLE_ZIP" ]]; then
  echo "Missing $GRADLE_ZIP"
  echo "Download gradle-8.14.3-all.zip and place it in $SCRIPT_DIR"
  exit 1
fi

# 确认这是一个带 Gradle wrapper 的项目。
if [[ ! -f "$WRAPPER_PROPERTIES" ]]; then
  echo "Missing $WRAPPER_PROPERTIES"
  exit 1
fi

# 使用 file:// URL 指向本机离线 zip。这里写绝对路径，避免 ~ 或相对路径解析不一致。
GRADLE_ZIP_URL="file\\://$GRADLE_ZIP"
sed -i "s#^distributionUrl=.*#distributionUrl=$GRADLE_ZIP_URL#" "$WRAPPER_PROPERTIES"
echo "Updated Gradle wrapper to use $GRADLE_ZIP"

# Android Gradle Plugin 通过 local.properties 找 Android SDK。
# 优先使用 ANDROID_HOME；没有设置时，按本机约定尝试 ~/Android。
if [[ -n "${ANDROID_HOME:-}" ]]; then
  printf 'sdk.dir=%s\n' "$ANDROID_HOME" > "$PROJECT_DIR/local.properties"
  echo "Wrote local.properties from ANDROID_HOME=$ANDROID_HOME"
elif [[ -d "$HOME/Android" ]]; then
  printf 'sdk.dir=%s/Android\n' "$HOME" > "$PROJECT_DIR/local.properties"
  echo "Wrote local.properties with sdk.dir=$HOME/Android"
else
  echo "Android SDK not configured. Set ANDROID_HOME or create local.properties manually."
fi

echo "Done."
