# XYJ Android Framework

支持多个独立 App 的 Windows Android 工作区，包含 Kotlin 应用入口、基础资源与构建配置。

## 安装环境

让 Agent 按 [环境安装手册](docs/AGENT_SETUP.md) 执行，也可直接复制下面的提示词。

需要 JDK 17、Android SDK 36、Build Tools 35.0.0、platform-tools 和 Gradle 8.14.3。工具统一放入项目 `.tools/`，不需要 Android Studio；工具和缓存不提交 Git。

## 给 Agent 的安装提示词

复制以下内容发送给 Agent：

```text
请为当前项目安装并验证 Windows 开发环境。先阅读 README.md 和 docs/AGENT_SETUP.md，然后实际执行安装，不要只给我操作建议。

将 JDK、Android SDK、platform-tools（包含 adb）和本地 Gradle 放在项目 .tools 目录，设置隐藏属性并确认 Git 忽略；使用项目内工具，不依赖其他盘的已有安装。不修改系统长期环境变量或执行策略。已有完整安装先检查再复用，不覆盖未知文件。

下载使用官方来源并核验校验值，检查每步退出码。自动下载失败时尝试官方渠道；若必须由浏览器下载，明确告诉我下载地址和保存位置，不使用残缺文件。

运行 apps/xiaoyangjun-demo/windows_build.bat -BuildOnly，确认构建成功并检查 APK。然后检测手机；有已授权设备时安装验证，无设备或未授权则如实报告。不要擅自卸载应用、清除数据、替换签名密钥或修改系统驱动。

遇到问题修正通用安装步骤，但文档不要写入本机绝对路径、设备序列号、个人信息或本次调试流水账。最后报告安装内容、构建结果、设备验证结果和未完成项。
```

## 日常使用

在项目根目录打开 PowerShell：

```powershell
# 编译并安装到手机
.\apps\xiaoyangjun-demo\windows_build.bat
# 仅编译
.\apps\xiaoyangjun-demo\windows_build.bat -BuildOnly
# 查看应用日志（先在手机打开应用，Ctrl+C 停止）
.\apps\xiaoyangjun-demo\windows_simulation.bat
```

多台设备时追加 `-Serial 设备序列号`。两个入口仅为本次进程放行 PowerShell 脚本，不修改系统长期执行策略。

产物位于 `apps/xiaoyangjun-demo/build/outputs/apk/debug/xiaoyangjun-demo-debug.apk`。编译成功不代表安装成功；安装失败应查看前面的 adb 原始错误。日志入口不会启动模拟器。

## 项目结构

- `apps/xiaoyangjun-demo/src/main/`：应用代码、清单与资源。
- `apps/xiaoyangjun-demo/build.gradle.kts`：应用构建配置。
- `gradle/`：依赖版本与 Gradle Wrapper 文件。
- `scripts/`：Windows 构建、环境与日志实现。
- `docs/`：Agent 环境安装指南。
- `apps/xiaoyangjun-demo/version.properties`：应用版本。
- `.tools/`：本地工具与下载包，已忽略并可设为隐藏。
- `.gradle-home/`：项目 Gradle 缓存，已忽略。

## 常见问题

- 缺少 Java 或 SDK：按安装手册检查目录层级。
- Gradle 自动下载失败：支持浏览器下载 ZIP，再校验并使用本地包，详见安装手册。
- adb 列表为空：检查 USB 链路、调试授权与 Windows 驱动，不等同于编译环境失败。
- 签名不一致：复用原调试密钥；卸载旧应用会清除数据，不应自动卸载。


## 多 App 开发

每个 App 放在 `apps/<名称>/`，拥有独立的源码、`build.gradle.kts`、`version.properties` 和两个 `.bat` 入口。工具、Gradle 配置和版本目录在仓库根目录共用。现有应用为“小羊君demo”（目录 `xiaoyangjun-demo`），Gradle 模块为 `:xiaoyangjun-demo`，demo 使用独立安装包标识，见下方应用列表。

新增 App：

1. 创建 `apps/<名称>/`，名称使用小写字母、数字和连字符，且以字母开头。包含 `build.gradle.kts` 的直属目录会自动加入构建。
2. 添加 Android application 构建配置、源码和资源，设置独立的 `namespace`、`applicationId` 和版本文件。不同 App 必须使用不同的 applicationId，才能同时安装。
3. 复制现有 App 的两个 `.bat` 入口，将其中 `-App xiaoyangjun-demo` 改为新目录名称。不要复制 `build/` 输出。
4. 运行新目录内的 `windows_build.bat -BuildOnly`。产物为 `apps/<名称>/build/outputs/apk/debug/<名称>-debug.apk`。

可在任意工作目录调用 App 的入口，也可进入 App 文件夹直接运行。日志脚本从该 App 的构建结果读取包名，首次使用前需编译。当前入口针对标准 debug 构建，未支持 product flavors 或拆分 APK。

每个 App 独立编译和安装；不传 App 名称时，共用脚本不会默认操作其他应用。新增 App 的业务配置建议放在各自命名空间下，避免共用配置键互相覆盖。

## 从 GitHub 迁入 App

仓库内提供技能：`.agents/skills/import-github-android-app/SKILL.md`。它会克隆完整独立 Git 仓库到 apps 下，保留来源历史、远程和 Gradle 工程，再适配共用 Windows 工具环境。

给 Agent 的示例请求：

```text
请读取并使用 .agents/skills/import-github-android-app/SKILL.md，
把 https://github.com/组织/仓库 迁入当前项目开发，按现有 Windows 编译方式接入。
保留原有功能，完成构建验证并更新文档。
```

如果客户端已发现该技能，也可用 `$import-github-android-app` 调用。仓库文件存在不代表当前会话已经自动加载，按路径读取的方式可直接使用。


独立项目复用 `.tools` 和 `.gradle-home`，保留自身依赖、模块及配置。入口运行在子仓库根目录；`windows-host.json` 指定构建任务与 APK 元数据位置。宿主自有 App 仍按前面的多模块方式开发。
