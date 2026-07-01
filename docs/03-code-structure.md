# 代码结构

适用范围：原生 Android 手机仓 MVP。  
目标：先跑通页面显示和状态跳转，再逐步接入后端、豆包大模型、IMU、相机和 USB 手机仓。

## 当前基础

```text
app/src/main/
├── AndroidManifest.xml
├── java/com/xyj/focuspod/MainActivity.kt
└── res/
    ├── values/
    └── values-night/
```

当前项目是单模块 Android Kotlin 工程，先保持单模块，不急着拆库。

## 目标目录

```text
app/src/main/java/com/xyj/focuspod/
├── MainActivity.kt
├── app/
│   ├── AppContainer.kt
│   └── AppConfig.kt
├── model/
│   ├── StudyModels.kt
│   ├── DeviceModels.kt
│   └── AiModels.kt
├── flow/
│   ├── StudyFlow.kt
│   └── StudyFlowRules.kt
├── screen/
│   ├── StartScreen.kt
│   ├── PlanSelectScreen.kt
│   ├── DeviceSelfCheckScreen.kt
│   ├── TutoringScreen.kt
│   ├── ExamScreen.kt
│   ├── GradingScreen.kt
│   ├── GradeResultScreen.kt
│   └── DoneScreen.kt
├── ui/
│   ├── component/
│   ├── layout/
│   └── theme/
├── service/
│   ├── ai/
│   ├── api/
│   ├── camera/
│   ├── device/
│   ├── interaction/
│   ├── sensor/
│   ├── storage/
│   └── usb/
├── repository/
├── mock/
└── util/
```

## 页面层

`screen/` 只负责显示页面和接收状态，不直接写 AI、相机、IMU、USB 细节。

```text
StartScreen.kt              启动页，3 秒后自动跳转
PlanSelectScreen.kt         学习计划选择页，唯一主要触控页
DeviceSelfCheckScreen.kt    设备自检页，合并 USB 等待
TutoringScreen.kt           辅导页，例题和错题整理后的新例题共用
ExamScreen.kt               考试页，只显示题目
GradingScreen.kt            批卷页，显示拍摄画面和批改状态
GradeResultScreen.kt        批卷结果页，10 秒后自动跳转
DoneScreen.kt               完成开门页
```

页面 3 以后不设计触控按钮，页面只能展示状态、语音字幕、姿态提示和异常提示层。

## UI 组件

```text
ui/component/SplashMarkView.kt
ui/component/PlanCard.kt
ui/component/PrimaryButton.kt
ui/component/StageBadge.kt
ui/component/RemainingCountBadge.kt
ui/component/StatusTag.kt
ui/component/DeviceCheckList.kt
ui/component/QuestionPanel.kt
ui/component/AiGuidePanel.kt
ui/component/VoiceCaptionBar.kt
ui/component/PoseHintPanel.kt
ui/component/CameraGradingPanel.kt
ui/component/GradeResultPanel.kt
ui/component/DoorStatusPanel.kt
ui/component/AppAlertOverlay.kt
ui/layout/AppPage.kt
ui/layout/LockedPhoneLayout.kt
ui/layout/TopStageBar.kt
ui/theme/AppColors.kt
ui/theme/AppTextStyles.kt
```

`PrimaryButton` 只用于学习计划选择页。`AppAlertOverlay` 是当前页面提示层，不是独立异常页。

## 状态和流程

```kotlin
enum class StudyPage {
    START,
    PLAN_SELECT,
    DEVICE_SELF_CHECK,
    TUTORING,
    EXAM,
    GRADING,
    GRADE_RESULT,
    DONE
}

enum class StudyStage {
    EXAMPLE,
    EXAM
}
```

说明：

- 错题不单独做页面阶段。
- 考试不达标后，把错题整理成新的例题，回到 `EXAMPLE`。
- `StudyFlow` 只关心当前页面、当前阶段、当前题、批卷结果和考试结果。
- 第一阶段所有“检测条件”先用 5 秒模拟实现。

## 核心数据

```kotlin
data class StudySessionState(
    val page: StudyPage,
    val stage: StudyStage,
    val selectedPlan: StudyPlan?,
    val currentQuestion: Question?,
    val remainingCount: Int,
    val lastGradeResult: GradeResult?,
    val alertMessage: String?
)
```

```kotlin
data class StudyPlan(
    val id: String,
    val title: String,
    val subject: String,
    val estimatedMinutes: Int,
    val passScore: Int,
    val exampleQuestions: List<Question>,
    val examQuestions: List<Question>
)
```

## 服务接口

先定义接口，再逐步替换实现。

```text
api/StudyPlanApi.kt              获取学习计划，第一阶段本地写死一条
ai/TutoringAiService.kt          辅导页 AI 语音交互，后续接豆包大模型
ai/GradingAiService.kt           图片批卷，后续接豆包大模型
sensor/PoseDetector.kt           IMU 翻转检测，第一阶段 5 秒后触发
camera/CameraCaptureService.kt   后置摄像头和拍照，第一阶段 5 秒后返回假图片
usb/DoorControlService.kt        USB 开门，第一阶段 5 秒后返回成功
interaction/VoiceService.kt      语音播报和语音识别
storage/SessionStorage.kt        保存上次学习位置和本次学习记录
```

## 模拟实现

```text
mock/MockStudyPlanApi.kt
mock/FakePoseDetector.kt
mock/FakeCameraCaptureService.kt
mock/FakeTutoringAiService.kt
mock/FakeGradingAiService.kt
mock/FakeDoorControlService.kt
```

模拟规则：

- 学习计划：本地写死一条计划。
- 页面 1：3 秒后跳转。
- 设备自检：5 秒后通过。
- 翻转检测：5 秒后触发。
- 批卷：5 秒后返回固定结果。
- 批卷完成后翻回正面：5 秒后触发。
- 开门：5 秒后返回成功。

## 后端接口预留

```text
GET  /plans              获取学习计划
POST /sessions/save      保存学习过程
POST /ai/tutor           辅导对话
POST /ai/grade           图片批卷
```

APP 端不保存大模型 API Key。豆包大模型 Key 放在后端，由后端调用火山引擎。

## 依赖原则

- 第一阶段尽量不加复杂依赖。
- 接豆包语音交互时再补网络、音频、流式返回相关依赖。
- 接相机时优先考虑 CameraX。
- 接 IMU 时使用 Android `SensorManager`。
- 接 USB 时使用 Android USB Host 和固定手机仓硬件方案。
