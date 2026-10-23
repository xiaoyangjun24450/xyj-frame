# Windows 环境安装：Agent 执行手册

## 目标和边界

在项目根目录完成 Windows 原生安装与编译，不依赖机器上的其他 Android 安装。所有工具放入已被 Git 忽略的 `.tools/`，Gradle 缓存放入 `.gradle-home/`。不修改系统环境变量、长期执行策略，不提交工具或本机路径。本文适用于 Windows x64；其他架构先核对厂商支持，不直接套用。

用户要求安装环境后，Agent 应实际执行、检查退出码并记录结果，不只把步骤交回用户。工具下载和编译需要联网。手机安装与日志分别验证，没有设备就明确记录，不能声称已验证。

## 1. 准备和下载

工作目录必须是仓库根目录。先检查现有 `.tools`，完整且版本合适的安装可复用；不要覆盖未知目录。以下为固定版本安装示例。执行每个阶段后检查错误，下载失败不要解压残缺文件。

```powershell
New-Item -ItemType Directory -Force .tools/downloads | Out-Null
attrib +h .tools
curl.exe -L --fail --retry 3 -o .tools/downloads/zulu-jdk.zip https://cdn.azul.com/zulu/bin/zulu17.68.203-ca-crac-jdk17.0.20.1-win_x64.zip
if ($LASTEXITCODE) { throw 'JDK download failed' }
if ((Get-FileHash .tools/downloads/zulu-jdk.zip).Hash -ne '99F4A11A067F58A5B7DB03DF9B19F308B29FB8666FC3A3ACF16BA691A2D6F1DA') { throw 'JDK checksum mismatch' }
curl.exe -L --fail --retry 3 -o .tools/downloads/commandline.zip https://dl.google.com/android/repository/commandlinetools-win-13114758_latest.zip
if ($LASTEXITCODE) { throw 'Android tools download failed' }
if ((Get-FileHash .tools/downloads/commandline.zip -Algorithm SHA1).Hash -ne '54A582F3BF73E04253602F2D1C80BD5868AAC115') { throw 'Android tools checksum mismatch' }
```

JDK 校验值来源：[Azul 官方包记录](https://api.azul.com/metadata/v1/zulu/packages/1eb58864-f1b0-4eb4-89f7-fee042c08b0e)。Android 工具 19.0 校验值来源：[Google 官方仓库清单](https://dl.google.com/android/repository/repository2-1.xml)。SHA-1 在这里用于比对 Google 清单的文件完整性，下载使用官方 HTTPS 地址。

## 2. 解压

首次安装运行下列命令。重复安装先检查目标，不直接覆盖已有 JDK 或把目录套两层。

```powershell
Expand-Archive .tools/downloads/zulu-jdk.zip .tools/jdk-stage
$jdkDir = Get-ChildItem .tools/jdk-stage -Directory
if (@($jdkDir).Count -ne 1) { throw 'Unexpected JDK archive layout' }
Move-Item -LiteralPath $jdkDir.FullName -Destination .tools/jdk
Expand-Archive .tools/downloads/commandline.zip .tools/cmdline-stage
New-Item -ItemType Directory -Force .tools/android-sdk/cmdline-tools | Out-Null
Move-Item .tools/cmdline-stage/cmdline-tools .tools/android-sdk/cmdline-tools/latest
& .tools/jdk/bin/java.exe -version
& .tools/jdk/bin/javac.exe -version
```

预期 Java 为 17，且存在 `.tools/android-sdk/cmdline-tools/latest/bin/sdkmanager.bat`。无需另装 Android Studio。JDK 包包含 CRaC 功能，本项目只使用其普通 Java 17 编译和运行功能。

## 3. 安装 SDK 和 adb

```powershell
$env:JAVA_HOME = "$PWD\.tools\jdk"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
$sdk = "$PWD\.tools\android-sdk"
& "$sdk\cmdline-tools\latest\bin\sdkmanager.bat" "--sdk_root=$sdk" 'platform-tools' 'platforms;android-36' 'build-tools;35.0.0'
if ($LASTEXITCODE) { throw 'SDK installation failed' }
```

首次安装会展示 SDK 许可并要求接受。交互终端可回答 `y`；在已授权完整安装的 Agent 无交互执行环境中，可将 `1..20 | ForEach-Object { 'y' }` 管道传给上面的 sdkmanager 命令，并保存输出日志。不要把“运行过命令”当作安装成功：检查退出码及下列文件。

```powershell
Test-Path .tools/android-sdk/platforms/android-36/android.jar
Test-Path .tools/android-sdk/build-tools/35.0.0/aapt2.exe
& .tools/android-sdk/platform-tools/adb.exe version
```

两个检查必须为 True，adb 输出的安装位置必须指向本项目。所有人都安装项目内 platform-tools，不以其他盘已有 adb 为理由跳过。

## 4. 构建验证

```powershell
.\apps\xiaoyangjun-demo\windows_build.bat -BuildOnly
```

必须退出码为 0，出现 `BUILD SUCCESSFUL`，且生成 `apps/xiaoyangjun-demo/build/outputs/apk/debug/xiaoyangjun-demo-debug.apk`。Gradle 8.14.3 会自动下载。脚本自动使用本地 JDK 和 SDK，并更新 `local.properties` 的 SDK 路径；不会设置系统长期环境变量。

使用 `.bat` 入口避免 PowerShell 默认禁止 `.ps1` 的问题。入口仅为该次进程设置执行策略。直接运行 `scripts/gradlew.bat` 不会自动加载项目环境。

## 5. 设备验证

```powershell
& .tools/android-sdk/platform-tools/adb.exe devices -l
.\apps\xiaoyangjun-demo\windows_build.bat
# 在手机上打开应用后
.\apps\xiaoyangjun-demo\windows_simulation.bat
```

多台设备时，两个入口都可加 `-Serial YOUR_SERIAL`。日志脚本显示当前应用进程的 Info 及以上日志，Ctrl+C 退出；应用重启后要重跑。它不是启动模拟器的脚本。

设备列表为空：报告无设备；`unauthorized`：需要用户在手机授权；驱动问题需按手机厂商处理。不能把环境安装成功等同于手机验收通过。

adb 查找顺序：显式 `$env:ADB`、项目 `.tools/android-sdk/platform-tools/adb.exe`、`ANDROID_HOME`、PATH。本流程验收时清除当前进程的 ADB 覆盖，确认使用项目副本。

## 故障与复跑

- JDK missing：检查 `.tools/jdk/bin/javac.exe`，避免 ZIP 解压后多一层版本目录。
- 下载慢：先观察下载进度和网络；可以切换其他官方 Java 17 发行版，但须更新下载地址、校验来源、解压命令及实测记录。不能跳过校验或从未知镜像下载可执行文件。
- SDK 下载失败：保留 sdkmanager 日志，重新执行组件安装命令，检查退出码。
- Gradle 或 Maven 下载失败：保存真实报错，检查网络，不把网络问题归因于 JAVA_HOME。
- `.tools` 以点开头并不会在 Windows 自动隐藏；必须设置隐藏属性。资源管理器开启“隐藏的项目”可查看。


### 浏览器能下载、Gradle 自动下载失败

浏览器与 Java 的网络/代理设置可能不同，不能只凭浏览器成功就认为 Java 也能直连。推荐使用浏览器下载完整的官方包：

https://services.gradle.org/distributions/gradle-8.14.3-bin.zip

将文件保存为项目根目录下 `.tools/downloads/gradle-8.14.3-bin.zip`，覆盖此前下载失败留下的残缺包。隐藏目录可在浏览器“另存为”的路径栏直接输入完整路径；必须是 `bin.zip`，不是 `all.zip`，也不能是 `.crdownload`。

停止仍在下载的构建进程（原终端 Ctrl+C），保存完毕后执行：

```powershell
.\apps\xiaoyangjun-demo\windows_build.bat -BuildOnly
```

Windows 构建脚本会先验证本地 ZIP 的 SHA-256，再解压到 `.tools/gradle` 并使用本地 Gradle，跳过 Wrapper 下载；不修改仓库的 Wrapper URL。校验失败会明确停止，避免把残缺包当作安装成功。包存在时每次都会校验，请保留该 ZIP；没有 ZIP 则恢复 Wrapper 自动下载。

官方 SHA-256：`bd71102213493060956ec229d946beee57158dbd89d0e62b91bca0fa2c5f3531`，来源：https://services.gradle.org/distributions/gradle-8.14.3-bin.zip.sha256 。

这只解决 Gradle 本体下载；首次构建的 Android/Kotlin 插件和 Maven 依赖仍需联网，后续若报下载失败应单独排查。
