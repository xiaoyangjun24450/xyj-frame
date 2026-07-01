# XYJ 最小框架上手文档

这个分支已经移除原有应用业务，只保留可继续开发的 Android 基础工程。

## 1. 项目结构

- `settings.gradle.kts`: 声明根项目和 `app` 模块。
- `build.gradle.kts`: 根项目插件声明。
- `gradle/libs.versions.toml`: 依赖和插件版本集中管理。
- `app/build.gradle.kts`: App 模块构建配置。
- `app/src/main/AndroidManifest.xml`: 应用入口声明。
- `app/src/main/java/com/xyj/focuspod/MainActivity.kt`: 当前唯一页面。
- `build.sh`: 安装 Debug 包。
- `simulation.sh`: 查看应用日志。

## 2. 启动链路

1. 系统启动 `MainActivity`。
2. `MainActivity` 创建一个居中的 `TextView`。
3. 页面显示应用名 `XYJ`。

## 3. 开发建议

- 新页面从 `app/src/main/java/com/xyj/focuspod/` 下继续添加。
- 新资源放到 `app/src/main/res/` 对应目录。
- 新依赖先写入 `gradle/libs.versions.toml`，再在 `app/build.gradle.kts` 中引用。

## 4. 验证命令

```bash
./gradlew :app:assembleDebug
```
