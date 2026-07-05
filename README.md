# XYJ Android Framework

这是一个保留最基本开发框架的 Android 项目。

## 当前保留内容

- Gradle Wrapper 与基础 Gradle 配置
- 单模块 Android App：`app`
- 最小启动入口：`MainActivity`
- 基础资源：应用名、颜色、主题
- 构建脚本：`build.sh`
- 日志脚本：`simulation.sh`

## 环境准备

项目需要 Java 17、Android SDK 和 Gradle Wrapper。Android SDK 可以通过官方 Command line tools 安装：

1. 从 Android 官方下载 Command line tools：
   https://developer.android.com/studio#command-tools

   Linux 包示例：
   https://dl.google.com/android/repository/commandlinetools-linux-14742923_latest.zip

2. 解压后目录必须整理成：

   ```text
   $HOME/Android/cmdline-tools/latest/bin/sdkmanager
   ```

   也就是 `sdkmanager` 必须位于 `<SDK根目录>/cmdline-tools/latest/bin/`。

3. 安装本项目需要的 SDK 组件：

   ```bash
   $HOME/Android/cmdline-tools/latest/bin/sdkmanager --sdk_root=$HOME/Android \
     "platform-tools" "platforms;android-36" "build-tools;36.0.0"
   ```

   构建时 Android Gradle Plugin 可能还会按需安装兼容的 build-tools 版本。

4. Gradle 通过 `local.properties` 里的 `sdk.dir` 找 SDK，写入命令：

   ```bash
   printf 'sdk.dir=%s/Android\n' "$HOME" > local.properties
   ```

如果使用离线 Gradle 分发包，把 `gradle-8.14.3-all.zip` 放到 `offline-deps/`，然后执行：

```bash
./offline-deps/install.sh
```

## 豆包语音辅导配置

辅导页已接入火山引擎豆包 Dialog 语音 SDK。鉴权参数不要提交到仓库，建议写在本机 `local.properties`：

```properties
doubao.appId=你的 AppID
doubao.token=你的 Access Token
doubao.appKey=PlgvMymc7f3tQnJ6
```

可选配置：

```properties
doubao.uid=student-001
doubao.appKey=默认使用 doubao.appId，通常不用填
doubao.resourceId=volc.speech.dialog
doubao.dialogAddress=wss://openspeech.bytedance.com
doubao.dialogUri=/api/v3/realtime/dialogue
doubao.botName=豆包
doubao.aecModelPath=/absolute/path/to/aec.model
doubao.debugPath=/absolute/path/to/existing/log/dir
doubao.recorderPath=/absolute/path/to/existing/recorder/dir
doubao.playerPath=/absolute/path/to/existing/player/dir
doubao.logLevel=WARN
```

如果控制台只显示 `APP ID`、`Access Token`、`Secret Key`，这里先只填 `APP ID` 和 `Access Token`。当前 SDK 报错中的 `X-Api-App-Key` 期望值是 `APP ID`，所以 `doubao.appKey` 默认也会使用 `doubao.appId`；不要把 `Secret Key` 填到 `doubao.appKey`，否则会出现 `invalid X-Api-App-Key`。

不配置 `doubao.appId`、`doubao.token` 时，辅导页会保留题目和本地引导文案，但语音会话会显示配置缺失提示。开启 AEC 时需要额外提供文档里的 `aec.model` 文件路径。

## 豆包多模态批卷配置

批卷页使用后置摄像头预览，翻转进入批卷页后等待 5 秒自动拍照。配置豆包多模态参数后，App 会按 Responses API 把答案图片、题干、标准答案和批卷提示词发送给模型；未配置时批卷会失败并提示配置缺失。

接口触发条件、请求结构和返回示例见 [大模型接口设计](docs/05-llm-api-design.md)。

建议写入本机 `local.properties`：

```properties
doubao.multimodal.apiKey=你的火山方舟 API Key
doubao.multimodal.model=你的多模态模型或 Endpoint ID
doubao.multimodal.endpoint=https://ark.cn-beijing.volces.com/api/v3/responses
```

图片按文档使用 `input_image.image_url`，传入 `data:image/jpeg;base64,...` 形式；输出使用 `text.format.type=json_schema`，字段为 `score`、`feedback`、`reason`、`suggestion`。

## 豆包辅导会话使用方式

```
启动会话
-> SESSION_STARTED
-> 播放开场白 SayHello
-> 后面就靠学生语音和 AI 对话继续
```
- App 在 `StartSession.dialog.system_role` 写入本题上下文和辅导规则。
- `system_role` 包含阶段、题目来源、标题、题干、知识点、辅导要求和“不要直接给答案”等规则。
- 固定辅导提示词从 `app/src/main/assets/prompts/` 下的 txt 文件读取。
- `tutoring_context_prompt.txt` 渲染 `system_role`，`tutoring_speaking_style.txt` 渲染 `speaking_style`。
- 新会话启动成功后，App 通过 `tutoring_opening_prompt.txt` 播放开场白，不再朗读完整题干。
- 后面靠学生语音和 AI 对话继续。
- 同一题停留期间不会反复设置上下文；切到下一题会重建会话并设置下一题上下文。

## WSL 真机调试

WSL 里可以负责编译，真机连接和安装建议复用 Windows 侧的 `adb.exe`。这样不需要把 USB 设备挂进 WSL。[下载地址](https://dl.google.com/android/repository/platform-tools-latest-windows.zip)

1. Windows 下载并解压 Android SDK Platform-Tools，例如：

   ```text
   F:\Android\platform-tools\adb.exe
   ```

2. Windows PowerShell 确认设备已连接：

   ```powershell
   F:\Android\platform-tools\adb.exe devices -l
   ```

   需要看到设备状态为 `device`。

3. WSL 编译 Debug APK：

   ```bash
   bash wsl_build.sh
   ```

4. WSL 直接调用 Windows 的 `adb.exe` 安装 APK：

   ```bash
   /mnt/f/Android/platform-tools/adb.exe install -r -t app/build/outputs/apk/debug/app-debug.apk
   ```

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
├── java/com/xyj/focuspod/MainActivity.kt
└── res/
    ├── values/
    └── values-night/
```

后续开发可以从 `MainActivity.kt` 开始添加页面和业务逻辑。


## TODO

- 进入错题讲解后根据错题的类型，举一反三出同类型的新试题
- USB接口