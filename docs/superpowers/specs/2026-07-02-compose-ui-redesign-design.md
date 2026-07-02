# Compose UI 全量重构设计

## 背景

当前项目是 Android Kotlin 单模块工程，已有 8 个页面、`StudyFlow` 状态机、模型和 mock 服务。现有 UI 使用原生 View 手写布局，能跑通 MVP 流程，但视觉层级、安全区避让、跨设备适配和组件复用都不足。

本次重构目标是根据 `docs/UI设计原则.md` 和 `docs/` 下页面需求，把所有 UI 页面改成现代 Compose + Material 3 实现。用户已明确允许新增官方 AndroidX Compose / Material3 依赖，并选择“全量 Compose + Material 3 重构”方案。

## 目标

- 全量重写 8 个 UI 页面：启动页、学习计划选择页、设备自检页、辅导页、考试页、批卷页、批卷结果页、完成开门页。
- 使用 Jetpack Compose + Material 3 构建 UI。
- 保留现有 `StudyFlow`、model、mock service，继续跑通当前自动流程。
- 实现沉浸式 edge-to-edge，内容避让状态栏和底部手势栏。
- 页面 3 以后不设计触控按钮，只展示状态、语音字幕、姿态提示和异常提示层。
- 视觉风格保持安静、专注、学习终端感，适合小白用户、外行人和投资人演示。

## 非目标

- 不接入真实相机、IMU、USB、后端或大模型。
- 不改变学习流程规则和考试达标逻辑。
- 不新增复杂导航框架。
- 不做用户账号、设置页、历史记录页等文档外页面。

## 技术方案

`MainActivity` 改为 Compose 入口。Activity 继续持有 `AppContainer` 和 `StudyFlow`，观察 `StudySessionState`，用 `setContent` 渲染根组件。

根组件命名为 `FocusPodApp`：

- 接收 `state: StudySessionState` 和 `flow: StudyFlow`。
- 包裹 `FocusPodTheme`。
- 根据 `state.page` 分发到 8 个 Compose 页面。
- 页面切换后触发 `studyFlow.enterCurrentPage()`，保持当前状态机行为。

UI 目录：

```text
app/src/main/java/com/xyj/focuspod/ui/theme/
├── FocusPodTheme.kt
├── FocusPodColors.kt
└── FocusPodType.kt

app/src/main/java/com/xyj/focuspod/ui/component/
├── AppScaffold.kt
├── AlertOverlay.kt
├── StageTopBar.kt
├── VoiceCaptionBar.kt
├── PlanCard.kt
├── DeviceCheckList.kt
├── QuestionCard.kt
├── AiGuidePanel.kt
├── PoseHintBar.kt
├── GradingCameraPanel.kt
├── GradeResultPanel.kt
└── DoorStatusPanel.kt

app/src/main/java/com/xyj/focuspod/screen/
├── FocusPodApp.kt
├── StartScreen.kt
├── PlanSelectScreen.kt
├── DeviceSelfCheckScreen.kt
├── TutoringScreen.kt
├── ExamScreen.kt
├── GradingScreen.kt
├── GradeResultScreen.kt
└── DoneScreen.kt
```

旧 View 工具文件和旧页面函数在 Compose 页面接管后不再使用。若确认没有引用，可以删除旧 `ViewExtensions.kt`、`AppPage.kt` 和旧 View 页面实现，避免代码库保留两套 UI。

## 视觉系统

颜色：

- 主色使用低饱和深青绿，表达完成、通过、开门成功和学习进度。
- 背景使用轻微带色的中性浅色，不使用纯白铺满。
- 卡片使用 Material 3 tonal elevation，靠明度和边界区分层级。
- 警告色使用克制橙红，用于设备异常、判卷失败、开门失败。
- 深色相机页使用暗铬色背景，白色文字和轻微 scrim 提升可读性。

形状：

- 普通卡片使用 24dp 圆角。
- 底部吸底操作区和大面板使用 28dp 圆角。
- 状态徽标使用胶囊形状。

间距：

- 页面左右边距使用 20dp 或 24dp。
- 组件间距遵守 8dp 栅格。
- 滚动页面底部保留 96dp 以上 padding，避免内容被底部区域或系统手势遮挡。

字号：

- 页面标题 28sp 左右。
- 学习中题目正文 26sp 到 30sp。
- 状态和结果文字 32sp 到 44sp。
- 辅助说明 14sp 到 16sp。

## 安全区和自适应

所有页面使用 edge-to-edge：

- 背景延伸到状态栏和导航栏。
- 标题、阶段栏、返回区域等核心内容使用 `statusBarsPadding()`。
- 底部字幕条、吸底按钮、状态提示使用 `navigationBarsPadding()`。
- 列表和滚动内容使用足够 `contentPadding`，最后一项能完整越过底部区域。

跨设备适配：

- 页面内容使用 `BoxWithConstraints` 或合理的宽度上限，在平板和折叠屏上保持内容居中，不无限拉宽。
- 学习中核心题目卡片随高度变化自适应，不依赖固定屏幕比例。
- 批卷页相机预览区域充满主体区域，状态层居中显示。

## 页面设计

### 启动页

- 居中显示品牌式学习仓图形、`XYJ 学习仓`、`正在启动，请稍候`。
- 底部显示“手机放入学习仓后，将通过语音和翻转动作完成学习闭环”。
- 无按钮，等待状态机自动跳转。

### 学习计划选择页

- 顶部显示“选择学习计划”、学生姓名、网络状态。
- 学习计划以 Material 3 卡片展示：标题、科目、预计时长、题目数量、达标分数。
- 选中计划显示更明确的边框、背景和勾选标识。
- 底部使用吸底操作区展示“开始本计划”，未选择时置灰。
- 没有计划时显示“暂无学习计划，请联系老师”。

### 设备自检页

- 顶部显示“设备自检中，请勿触摸手机”。
- 中间列表展示相机、麦克风、网络、姿态传感器、USB 手机仓。
- 每项显示检查中、已通过、未通过三种状态。
- 底部显示 USB 提示和语音字幕。
- 无按钮。

### 辅导页

- 顶部阶段栏显示“例题讲解”和剩余题数。
- 主区域显示题目卡片，题干字号较大，隔着手机仓也能阅读。
- 下方显示 AI 引导面板，用对话形式展示当前引导。
- 底部显示姿态提示、久坐喝水提醒、语音字幕。
- 角落或底部提示“完成后请翻转手机开始批卷”。
- 无按钮。

### 考试页

- 顶部阶段栏显示“考试”和剩余题数。
- 主区域只展示题目和必要配图区域。
- 不显示 AI 讲解、答案和详细解析。
- 底部显示“写完后请翻转手机提交本题”。
- 无按钮。

### 批卷页

- 主体为深色后摄预览区域。
- 顶部保留阶段栏和剩余题数。
- 中央覆盖状态文字：正在拍摄、正在上传、正在批改、批改完成请翻回正面。
- 底部显示“请保持手机稳定”和语音字幕。
- 无按钮。

### 批卷结果页

- 顶部保留阶段栏和剩余题数。
- 中央大字号展示本题通过或未通过。
- 显示分数、简短反馈和倒计时。
- 通过使用绿色，未通过使用橙红色。
- 无按钮，10 秒后自动进入下一步。

### 完成开门页

- 中央展示“学习完成”或“考试达标”。
- 下方展示考试得分、完成题数、错题处理数量。
- 显示开门状态：正在开门、开门成功、开门失败。
- 开门失败使用当前页面提示层，不跳转异常页。
- 无按钮。

## 异常提示层

所有异常沿用 `state.alertMessage`：

- 在当前页面上方覆盖半透明 scrim。
- 中央使用高圆角提示卡片。
- 使用警告色标题和清晰短句。
- 不新增异常页面。

## 数据流

- `StudyFlow` 是页面状态唯一来源。
- Compose 页面只读取 `StudySessionState`。
- 只有学习计划选择页能调用 `flow.selectPlan(plan)`。
- 页面 3 以后不调用流程方法推进，仍由 `StudyFlow` 的模拟检测和定时器推进。

## 验证

实现后运行：

```bash
./gradlew :app:assembleDebug
```

验收标准：

- 项目能成功编译。
- 从启动页到完成开门页流程仍能自动跑通。
- 计划选择页是唯一主要触控页面。
- 页面 3 以后没有触控按钮。
- 页面内容避让状态栏和底部手势栏。
- 批卷页、结果页、完成页状态文字清晰可读。

## 代码风格

- 注释使用中文，只解释关键布局和状态逻辑。
- Kotlin 写法保持主流、现代、直观。
- 不使用过度抽象和复杂高级语法。
- 不写与本次 UI 重构无关的功能代码。
