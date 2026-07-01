# Acemate Android Framework

这是一个保留最基本开发框架的 Android 项目。

## 当前保留内容

- Gradle Wrapper 与基础 Gradle 配置
- 单模块 Android App：`app`
- 最小启动入口：`MainActivity`
- 基础资源：应用名、颜色、主题
- 构建脚本：`build.sh`
- 日志脚本：`simulation.sh`

## 常用命令

```bash
./gradlew :app:assembleDebug
./gradlew :app:installDebug
./build.sh
```

## 目录结构

```text
app/src/main/
├── AndroidManifest.xml
├── java/com/pku/acematetennis/MainActivity.kt
└── res/
    ├── values/
    └── values-night/
```

后续开发可以从 `MainActivity.kt` 开始添加页面和业务逻辑。
