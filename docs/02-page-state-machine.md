# 页面设计和状态机

适用范围：原生 Android Kotlin MVP。  
目标：页面只做展示和交互，业务流程由状态机统一推进。

## 页面列表

```text
StartScreen              启动、断点恢复
PlanSelectScreen         学习计划选择
DeviceCheckScreen        权限、USB、网络、IMU 检查
TutoringScreen           例题辅导
PracticeScreen           练习作答
CaptureScreen            拍照判卷
GradeResultScreen        判卷结果
ExamScreen               考试
WrongQueueScreen         错题队列
WrongExplainScreen       错题讲解、变式题
DoneScreen               完成并开门
ErrorScreen              异常、重试、应急提示
```

MVP 先用单 `MainActivity` + 多 Screen。`MainActivity` 持有当前状态，根据状态切换页面 View。页面复杂后再考虑 Jetpack Navigation 或拆 Activity。

## 页面职责

StartScreen：
- 显示应用启动状态。
- 检查本地是否有未完成学习进度。
- 有断点则询问继续或重新开始。

PlanSelectScreen：
- 显示计划名称、科目、预计时长、题数、达标分数。
- 点击开始后进入设备检查。

DeviceCheckScreen：
- 检查相机权限。
- 检查麦克风权限。
- 检查网络。
- 检查 IMU 传感器。
- 检查 USB 设备。
- 全部通过后进入 USB 等待或辅导。

TutoringScreen：
- 显示例题和当前知识点。
- 显示 AI 对话。
- 显示语音识别状态。
- AI 只给提示，不直接给最终答案。

PracticeScreen：
- 显示同类型练习题。
- 提示学生纸笔作答。
- 监听手机姿态，翻转稳定后进入拍照。

CaptureScreen：
- 打开后摄。
- 拍摄纸面答案。
- 上传图片、题目、评分点、上下文到后端。
- 等待 AI 判卷结果。

GradeResultScreen：
- 显示是否通过、分数、简要反馈。
- 通过则进入下一题或考试。
- 不通过则回到讲解或重做。

ExamScreen：
- 每次显示一道题。
- 翻转手机提交当前题。
- 后台判分。
- 考试中不展示详细解析。

WrongQueueScreen：
- 显示错题数量和当前补救进度。
- 按难度从低到高处理。
- 支持“太难了”放弃，但受比例限制。

WrongExplainScreen：
- AI 讲解错题。
- 生成同知识点变式题。
- 变式题通过后移出错题队列。

DoneScreen：
- 显示完成状态。
- 调用 `UsbDoorService.unlock()`。
- 开门失败时显示重试和应急提示。

ErrorScreen：
- 显示错误原因。
- 提供重试、返回上一步、联系客服或应急处理。

## 核心状态

```kotlin
enum class AppStudyState {
    SELECT_PLAN,
    DEVICE_CHECK,
    USB_WAITING,
    TUTORING,
    PRACTICE,
    CAPTURE_GRADING,
    GRADE_RESULT,
    EXAM,
    WRONG_QUEUE,
    WRONG_EXPLAINING,
    VARIANT_PRACTICE,
    UNLOCKING,
    DONE,
    ERROR
}
```

## 姿态状态

```kotlin
enum class DevicePose {
    UNKNOWN,
    VERTICAL,
    HORIZONTAL_BACK_CAMERA_DOWN,
    UNSTABLE
}
```

规则：

- 竖直稳定 3 秒：进入或保持辅导、答题模式。
- 水平且后摄朝下稳定 3 秒：触发拍照或提交。
- 考试阶段：翻转手机代表提交当前题。
- 必须有姿态校准，避免不同手机坐标轴差异。

## 主流程

```text
SELECT_PLAN
 -> DEVICE_CHECK
 -> USB_WAITING
 -> TUTORING
 -> PRACTICE
 -> CAPTURE_GRADING
 -> GRADE_RESULT
 -> EXAM
 -> DONE
```

考试不达标时：

```text
EXAM
 -> WRONG_QUEUE
 -> WRONG_EXPLAINING
 -> VARIANT_PRACTICE
 -> EXAM
 -> DONE
```

异常时：

```text
任意状态 -> ERROR -> 重试或回到上一个可恢复状态
```

## 状态推进原则

- 页面不直接决定完整业务流程。
- 页面只发出用户事件或系统事件。
- `StudyFlow` 根据当前状态和事件计算下一状态。
- `StudyFlowRules` 判断通过、重做、放弃比例、是否开门。
- `SessionStorage` 保存关键断点，APP 重启后可恢复。

## 常见事件

```text
PlanSelected
PermissionPassed
UsbReady
TutoringFinished
PracticeSubmitted
GradePassed
GradeFailed
ExamPassed
ExamFailed
WrongQuestionCleared
UnlockSucceeded
UnlockFailed
RecoverableError
FatalError
```

## UI 风格

- 安静、专注、像学习终端，不做营销页。
- 重点显示当前任务、题目、AI 对话、设备状态。
- 按钮文案明确，避免学生误操作。
- 错误提示要给出下一步动作。
- 考试页减少干扰，不展示多余解释。
