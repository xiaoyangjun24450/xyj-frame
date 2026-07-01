# 代码结构

适用范围：原生 Android Kotlin MVP。  
当前项目：Gradle 单模块 `app`，入口为 `MainActivity.kt`。

## 当前基础

```text
app/src/main/
├── AndroidManifest.xml
├── java/com/xyj/focuspod/MainActivity.kt
└── res/
    ├── values/
    └── values-night/
```

现有依赖保持很轻：Android Gradle Plugin、Kotlin Android、`androidx.core:core-ktx`。MVP 先少加依赖，优先把闭环跑通。

## 目标目录

```text
app/src/main/
├── AndroidManifest.xml
├── java/com/xyj/focuspod/
│   ├── MainActivity.kt
│   ├── app/
│   │   ├── AppContainer.kt
│   │   └── AppConfig.kt
│   ├── model/
│   │   ├── AiModels.kt
│   │   ├── DeviceModels.kt
│   │   └── StudyModels.kt
│   ├── flow/
│   │   ├── StudyFlow.kt
│   │   └── StudyFlowRules.kt
│   ├── screen/
│   │   ├── StartScreen.kt
│   │   ├── PlanSelectScreen.kt
│   │   ├── DeviceCheckScreen.kt
│   │   ├── study/
│   │   │   ├── TutoringScreen.kt
│   │   │   ├── PracticeScreen.kt
│   │   │   ├── CaptureScreen.kt
│   │   │   └── GradeResultScreen.kt
│   │   ├── exam/ExamScreen.kt
│   │   ├── wrong/WrongQueueScreen.kt
│   │   ├── wrong/WrongExplainScreen.kt
│   │   ├── DoneScreen.kt
│   │   └── ErrorScreen.kt
│   ├── ui/
│   │   ├── component/
│   │   ├── layout/
│   │   └── theme/
│   ├── service/
│   │   ├── api/
│   │   ├── device/
│   │   ├── study/
│   │   └── storage/
│   ├── repository/
│   ├── mock/
│   └── util/
└── res/
    ├── drawable/
    ├── mipmap-*/
    ├── values/
    └── values-night/
```

## 分层职责

`app/`：
- 应用级配置。
- 手工依赖装配。
- 管理 mock 和真实实现的切换。

`model/`：
- 学习计划、题目、判卷结果、设备状态。
- 只放数据结构和简单枚举。

`flow/`：
- 学习流程状态机。
- 通过、重做、放弃比例、开门条件。
- 不直接操作 UI、USB、HTTP。

`screen/`：
- 页面展示和交互。
- 接收状态，触发事件。
- 不直接写复杂业务规则。

`ui/`：
- 可复用按钮、状态标签、题目面板、AI 对话面板。
- 布局和主题辅助。
- 不直接调用后端和硬件。

`service/api/`：
- HTTP 请求封装。
- 学习计划、AI、进度保存接口。
- 统一错误结构。

`service/device/`：
- 权限检查。
- USB 舱门。
- 姿态传感器。
- 语音识别。
- 相机拍照。

`service/study/`：
- 辅导、判卷、考试、错题等业务服务。
- 调用 api、device、repository，不直接依赖 Screen。

`service/storage/`：
- 本地断点恢复。
- 设备选择和临时状态。
- MVP 可先用 `SharedPreferences`。

`repository/`：
- 组合 mock、本地缓存、远端接口。
- 给上层提供稳定数据入口。

`mock/`：
- 学习计划假数据。
- 假 USB。
- 假 AI 判卷结果。
- 后端未完成前保证主流程可运行。

## 第一批文件

```text
app/AppContainer.kt
app/AppConfig.kt
model/StudyModels.kt
model/AiModels.kt
model/DeviceModels.kt
flow/StudyFlow.kt
flow/StudyFlowRules.kt
service/device/PermissionService.kt
service/device/UsbDoorService.kt
service/device/PoseService.kt
service/storage/SessionStorage.kt
repository/StudyRepository.kt
mock/MockStudyData.kt
screen/StartScreen.kt
screen/PlanSelectScreen.kt
screen/DeviceCheckScreen.kt
screen/study/TutoringScreen.kt
```

## 核心接口示例

```kotlin
interface UsbDoorService {
    fun scan(): List<UsbDeviceInfo>
    suspend fun connect(deviceId: String? = null)
    suspend fun waitForReady(): UsbDeviceInfo
    suspend fun getStatus(): DoorState
    suspend fun lock(): DoorControlResult
    suspend fun unlock(): DoorControlResult
}
```

```kotlin
data class StudySessionState(
    val currentState: AppStudyState,
    val selectedPlan: StudyPlan?,
    val currentQuestion: Question?,
    val wrongQuestions: List<Question>,
    val lastError: AppError?
)
```

## 后端接口

```text
POST /ai/chat                 辅导对话
POST /ai/grade                图片判卷
POST /ai/generate-variant     生成变式题
POST /ai/explain-wrong        错题讲解
GET  /plans                   获取学习计划
POST /sessions/save           保存学习进度
```

APP 端不保存大模型 API Key。AI Key、模型调用、JSON 校验、调用日志都放后端。

## 约定

- Kotlin 文件和类型用大驼峰：`StudyFlow.kt`、`StudyPlan`。
- 包名用小写：`service.device`、`screen.study`。
- 资源名用小写下划线：`screen_plan_select.xml`、`ic_usb.xml`。
- 不把所有业务写在 `MainActivity.kt`。
- 不在 Screen 里直接写 USB、AI、HTTP 细节。
- 不在 APP 端保存大模型 API Key。
- 先单模块，复杂度上来后再拆模块。
