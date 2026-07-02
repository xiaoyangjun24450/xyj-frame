# Compose UI Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the existing Android View UI with a full Jetpack Compose + Material 3 implementation for all 8 FocusPod MVP pages.

**Architecture:** Keep the existing `StudyFlow`, models, and mock services as the single source of app state. Replace `MainActivity` rendering with `setContent`, render `FocusPodApp(state, flow)`, and dispatch pages from `StudySessionState.page`. Compose screens are display-only except `PlanSelectScreen`, which calls `flow.selectPlan(plan)`.

**Tech Stack:** Kotlin 2.2.21, Android Gradle Plugin 8.13.1, Jetpack Compose, Material 3, Compose BOM 2026.06.00, Activity Compose 1.13.0.

---

## References

- Official Compose compiler setup: use `org.jetbrains.kotlin.plugin.compose` with Kotlin 2.0+ and the same Kotlin version as the project.
- Official Compose BOM setup: use `androidx.compose:compose-bom:2026.06.00` and omit versions on Compose libraries.
- Project spec: `docs/superpowers/specs/2026-07-02-compose-ui-redesign-design.md`
- Product docs: `docs/01-usage-flow.md`, `docs/02-page-state-machine.md`, `docs/03-code-structure.md`, `docs/04-roadmap.md`, `docs/UI设计原则.md`

## File Structure

Create:

- `app/src/main/java/com/xyj/focuspod/screen/FocusPodApp.kt`: Compose root, page dispatch, page-entry side effect.
- `app/src/main/java/com/xyj/focuspod/ui/theme/FocusPodTheme.kt`: Material 3 color scheme, typography, shapes.
- `app/src/main/java/com/xyj/focuspod/ui/component/AppScaffold.kt`: edge-to-edge page shell, safe-area padding, alert overlay.
- `app/src/main/java/com/xyj/focuspod/ui/component/StudyChrome.kt`: stage bar, voice caption, hint bars, status chips.
- `app/src/main/java/com/xyj/focuspod/ui/component/StudyCards.kt`: plan card, question card, AI guide card, device check list.
- `app/src/main/java/com/xyj/focuspod/ui/component/ResultPanels.kt`: grading camera panel, grade result panel, door status panel.
- `app/src/test/java/com/xyj/focuspod/ui/UiTextTest.kt`: focused unit tests for UI label helpers.

Modify:

- `gradle/libs.versions.toml`: add Compose, Activity Compose, JUnit, and Compose compiler plugin aliases.
- `build.gradle.kts`: register Compose compiler plugin alias.
- `app/build.gradle.kts`: enable Compose and add dependencies.
- `app/src/main/AndroidManifest.xml`: keep existing activity entry, no route changes.
- `app/src/main/java/com/xyj/focuspod/MainActivity.kt`: switch from View rendering to Compose rendering.
- `app/src/main/java/com/xyj/focuspod/screen/StartScreen.kt`: replace View function with `StartScreen`.
- `app/src/main/java/com/xyj/focuspod/screen/PlanSelectScreen.kt`: replace View function with `PlanSelectScreen`.
- `app/src/main/java/com/xyj/focuspod/screen/DeviceSelfCheckScreen.kt`: replace View function with `DeviceSelfCheckScreen`.
- `app/src/main/java/com/xyj/focuspod/screen/TutoringScreen.kt`: replace View function with `TutoringScreen`.
- `app/src/main/java/com/xyj/focuspod/screen/ExamScreen.kt`: replace View function with `ExamScreen`.
- `app/src/main/java/com/xyj/focuspod/screen/GradingScreen.kt`: replace View function with `GradingScreen`.
- `app/src/main/java/com/xyj/focuspod/screen/GradeResultScreen.kt`: replace View function with `GradeResultScreen`.
- `app/src/main/java/com/xyj/focuspod/screen/DoneScreen.kt`: replace View function with `DoneScreen`.

Delete after no references remain:

- `app/src/main/java/com/xyj/focuspod/ui/component/ViewExtensions.kt`
- `app/src/main/java/com/xyj/focuspod/ui/layout/AppPage.kt`
- `app/src/main/java/com/xyj/focuspod/ui/theme/AppColors.kt`

## Task 1: Enable Compose Dependencies

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `build.gradle.kts`
- Modify: `app/build.gradle.kts`

- [ ] **Step 1: Add version catalog entries**

Update `gradle/libs.versions.toml` so it contains these entries while keeping existing aliases:

```toml
[versions]
agp = "8.13.1"
kotlin = "2.2.21"
coreKtx = "1.17.0"
composeBom = "2026.06.00"
activityCompose = "1.13.0"
junit = "4.13.2"

[libraries]
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "coreKtx" }
androidx-activity-compose = { group = "androidx.activity", name = "activity-compose", version.ref = "activityCompose" }
androidx-compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }
androidx-compose-ui = { group = "androidx.compose.ui", name = "ui" }
androidx-compose-foundation = { group = "androidx.compose.foundation", name = "foundation" }
androidx-compose-material3 = { group = "androidx.compose.material3", name = "material3" }
androidx-compose-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
androidx-compose-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
junit = { group = "junit", name = "junit", version.ref = "junit" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
compose-compiler = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
```

- [ ] **Step 2: Register the Compose compiler plugin**

Update root `build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.compose.compiler) apply false
}
```

- [ ] **Step 3: Enable Compose in the app module**

Update `app/build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.xyj.focuspod"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.xyj.focuspod"
        minSdk = 24
        targetSdk = 36
        versionCode = versionProperties.getProperty("VERSION_CODE").toInt()
        versionName = versionProperties.getProperty("VERSION_NAME")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)

    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
}
```

- [ ] **Step 4: Verify dependency wiring**

Run:

```bash
./gradlew :app:assembleDebug
```

Expected: build reaches Kotlin compilation or succeeds. If Gradle cannot resolve Compose artifacts because of network sandboxing, rerun the same command with escalated network approval.

- [ ] **Step 5: Commit**

```bash
git add gradle/libs.versions.toml build.gradle.kts app/build.gradle.kts
git commit -m "build: enable compose material3"
```

## Task 2: Add UI Label Helpers With Unit Tests

**Files:**
- Create: `app/src/main/java/com/xyj/focuspod/ui/component/UiText.kt`
- Create: `app/src/test/java/com/xyj/focuspod/ui/UiTextTest.kt`

- [ ] **Step 1: Write failing tests**

Create `app/src/test/java/com/xyj/focuspod/ui/UiTextTest.kt`:

```kotlin
package com.xyj.focuspod.ui

import com.xyj.focuspod.model.DoorStatus
import com.xyj.focuspod.model.GradingStep
import com.xyj.focuspod.model.StudyStage
import com.xyj.focuspod.ui.component.doorStatusText
import com.xyj.focuspod.ui.component.gradingStepText
import com.xyj.focuspod.ui.component.stageText
import org.junit.Assert.assertEquals
import org.junit.Test

class UiTextTest {
    @Test
    fun stageTextShowsLearningStage() {
        assertEquals("例题讲解", stageText(StudyStage.EXAMPLE))
        assertEquals("考试", stageText(StudyStage.EXAM))
    }

    @Test
    fun gradingStepTextShowsCurrentProgress() {
        assertEquals("正在拍摄", gradingStepText(GradingStep.CAPTURING))
        assertEquals("正在上传", gradingStepText(GradingStep.UPLOADING))
        assertEquals("正在批改", gradingStepText(GradingStep.GRADING))
        assertEquals("批改完成\n请把手机翻回正面", gradingStepText(GradingStep.WAITING_FLIP_BACK))
    }

    @Test
    fun doorStatusTextShowsDoorProgress() {
        assertEquals("正在开门", doorStatusText(DoorStatus.OPENING))
        assertEquals("开门成功", doorStatusText(DoorStatus.OPENED))
        assertEquals("开门失败", doorStatusText(DoorStatus.FAILED))
    }
}
```

- [ ] **Step 2: Run tests and verify failure**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests com.xyj.focuspod.ui.UiTextTest
```

Expected: FAIL because `UiText.kt` and the three helper functions do not exist.

- [ ] **Step 3: Implement UI text helpers**

Create `app/src/main/java/com/xyj/focuspod/ui/component/UiText.kt`:

```kotlin
package com.xyj.focuspod.ui.component

import com.xyj.focuspod.model.DoorStatus
import com.xyj.focuspod.model.GradingStep
import com.xyj.focuspod.model.StudyStage

fun stageText(stage: StudyStage): String {
    return when (stage) {
        StudyStage.EXAMPLE -> "例题讲解"
        StudyStage.EXAM -> "考试"
    }
}

fun gradingStepText(step: GradingStep): String {
    return when (step) {
        GradingStep.CAPTURING -> "正在拍摄"
        GradingStep.UPLOADING -> "正在上传"
        GradingStep.GRADING -> "正在批改"
        GradingStep.WAITING_FLIP_BACK -> "批改完成\n请把手机翻回正面"
    }
}

fun doorStatusText(status: DoorStatus): String {
    return when (status) {
        DoorStatus.OPENING -> "正在开门"
        DoorStatus.OPENED -> "开门成功"
        DoorStatus.FAILED -> "开门失败"
    }
}
```

- [ ] **Step 4: Run tests and verify pass**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests com.xyj.focuspod.ui.UiTextTest
```

Expected: PASS, all 3 tests pass.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/xyj/focuspod/ui/component/UiText.kt app/src/test/java/com/xyj/focuspod/ui/UiTextTest.kt
git commit -m "test: cover ui status labels"
```

## Task 3: Add Compose Theme

**Files:**
- Create: `app/src/main/java/com/xyj/focuspod/ui/theme/FocusPodTheme.kt`

- [ ] **Step 1: Create Material 3 theme**

Create `FocusPodTheme.kt`:

```kotlin
package com.xyj.focuspod.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val FocusBackground = Color(0xFFF3F7F6)
val FocusSurface = Color(0xFFFBFEFD)
val FocusSurfaceLift = Color(0xFFEAF2F1)
val FocusPrimary = Color(0xFF0B665F)
val FocusPrimarySoft = Color(0xFFD6EBE8)
val FocusText = Color(0xFF152423)
val FocusTextMuted = Color(0xFF657978)
val FocusWarning = Color(0xFFE46645)
val FocusSuccess = Color(0xFF168E69)
val FocusCamera = Color(0xFF122020)

private val FocusLightColors = lightColorScheme(
    primary = FocusPrimary,
    onPrimary = Color.White,
    primaryContainer = FocusPrimarySoft,
    onPrimaryContainer = FocusText,
    background = FocusBackground,
    onBackground = FocusText,
    surface = FocusSurface,
    onSurface = FocusText,
    surfaceVariant = FocusSurfaceLift,
    onSurfaceVariant = FocusTextMuted,
    error = FocusWarning,
    onError = Color.White
)

private val FocusTypography = Typography(
    headlineLarge = androidx.compose.ui.text.TextStyle(
        fontSize = 30.sp,
        lineHeight = 38.sp,
        fontWeight = FontWeight.Bold,
        color = FocusText
    ),
    headlineMedium = androidx.compose.ui.text.TextStyle(
        fontSize = 26.sp,
        lineHeight = 34.sp,
        fontWeight = FontWeight.Bold,
        color = FocusText
    ),
    titleLarge = androidx.compose.ui.text.TextStyle(
        fontSize = 22.sp,
        lineHeight = 30.sp,
        fontWeight = FontWeight.Bold,
        color = FocusText
    ),
    bodyLarge = androidx.compose.ui.text.TextStyle(
        fontSize = 18.sp,
        lineHeight = 28.sp,
        color = FocusText
    ),
    bodyMedium = androidx.compose.ui.text.TextStyle(
        fontSize = 16.sp,
        lineHeight = 24.sp,
        color = FocusTextMuted
    ),
    labelLarge = androidx.compose.ui.text.TextStyle(
        fontSize = 15.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.SemiBold
    )
)

private val FocusShapes = Shapes(
    small = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(28.dp)
)

@Composable
fun FocusPodTheme(content: @Composable () -> Unit) {
    val colors = FocusLightColors
    MaterialTheme(
        colorScheme = colors,
        typography = FocusTypography,
        shapes = FocusShapes,
        content = content
    )
}
```

- [ ] **Step 2: Run build**

Run:

```bash
./gradlew :app:assembleDebug
```

Expected: PASS or only fails in files that still reference old View UI during later tasks.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/xyj/focuspod/ui/theme/FocusPodTheme.kt
git commit -m "feat: add focuspod compose theme"
```

## Task 4: Add Shared Compose Components

**Files:**
- Create: `app/src/main/java/com/xyj/focuspod/ui/component/AppScaffold.kt`
- Create: `app/src/main/java/com/xyj/focuspod/ui/component/StudyChrome.kt`
- Create: `app/src/main/java/com/xyj/focuspod/ui/component/StudyCards.kt`
- Create: `app/src/main/java/com/xyj/focuspod/ui/component/ResultPanels.kt`

- [ ] **Step 1: Create app shell and alert overlay**

Create `AppScaffold.kt` with:

```kotlin
package com.xyj.focuspod.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.xyj.focuspod.ui.theme.FocusWarning

@Composable
fun AppScaffold(
    alertMessage: String?,
    scrollable: Boolean = true,
    bottomBar: (@Composable BoxScope.() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (scrollable) {
            LazyColumn(
                state = rememberLazyListState(),
                contentPadding = PaddingValues(start = 24.dp, top = 24.dp, end = 24.dp, bottom = 120.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .widthIn(max = 720.dp)
                    .align(Alignment.TopCenter)
            ) {
                item { Box { content() } }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 20.dp)
                    .widthIn(max = 720.dp)
                    .align(Alignment.Center)
            ) {
                content()
            }
        }

        bottomBar?.let {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .widthIn(max = 720.dp)
            ) {
                it()
            }
        }

        if (!alertMessage.isNullOrBlank()) {
            AlertOverlay(message = alertMessage)
        }
    }
}

@Composable
fun AlertOverlay(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.42f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.titleLarge,
                color = FocusWarning,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(24.dp)
            )
        }
    }
}
```

- [ ] **Step 2: Create chrome components**

Create `StudyChrome.kt` using the exact file content in Appendix A.

- [ ] **Step 3: Create card components**

Create `StudyCards.kt` using the exact file content in Appendix B.

- [ ] **Step 4: Create result components**

Create `ResultPanels.kt` using the exact file content in Appendix C.

- [ ] **Step 5: Run build**

Run:

```bash
./gradlew :app:assembleDebug
```

Expected: PASS after imports are correct.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/xyj/focuspod/ui/component
git commit -m "feat: add compose shared ui components"
```

## Task 5: Add Compose App Root and Activity Bridge

**Files:**
- Create: `app/src/main/java/com/xyj/focuspod/screen/FocusPodApp.kt`
- Modify: `app/src/main/java/com/xyj/focuspod/MainActivity.kt`

- [ ] **Step 1: Create root dispatcher**

Create `FocusPodApp.kt`:

```kotlin
package com.xyj.focuspod.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.xyj.focuspod.flow.StudyFlow
import com.xyj.focuspod.model.StudyPage
import com.xyj.focuspod.model.StudySessionState
import com.xyj.focuspod.ui.theme.FocusPodTheme

@Composable
fun FocusPodApp(state: StudySessionState, flow: StudyFlow) {
    FocusPodTheme {
        LaunchedEffect(state.page) {
            flow.enterCurrentPage()
        }

        when (state.page) {
            StudyPage.START -> StartScreen(state)
            StudyPage.PLAN_SELECT -> PlanSelectScreen(state, flow)
            StudyPage.DEVICE_SELF_CHECK -> DeviceSelfCheckScreen(state)
            StudyPage.TUTORING -> TutoringScreen(state)
            StudyPage.EXAM -> ExamScreen(state)
            StudyPage.GRADING -> GradingScreen(state)
            StudyPage.GRADE_RESULT -> GradeResultScreen(state)
            StudyPage.DONE -> DoneScreen(state)
        }
    }
}
```

- [ ] **Step 2: Replace Activity rendering**

Modify `MainActivity.kt`:

```kotlin
package com.xyj.focuspod

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import com.xyj.focuspod.app.AppContainer
import com.xyj.focuspod.model.StudySessionState
import com.xyj.focuspod.screen.FocusPodApp

class MainActivity : ComponentActivity() {
    private val appContainer = AppContainer()
    private val studyFlow = appContainer.studyFlow
    private val currentState = mutableStateOf(StudySessionState())
    private val observer: (StudySessionState) -> Unit = { state ->
        currentState.value = state
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        studyFlow.observe(observer)
        setContent {
            FocusPodApp(
                state = currentState.value,
                flow = studyFlow
            )
        }
        studyFlow.start()
    }

    override fun onDestroy() {
        studyFlow.removeObserver(observer)
        super.onDestroy()
    }
}
```

- [ ] **Step 3: Run build and record expected failures**

Run:

```bash
./gradlew :app:assembleDebug
```

Expected: FAIL until each screen file exposes the PascalCase Compose function used by `FocusPodApp`.

- [ ] **Step 4: Commit after screen tasks pass**

Do not commit this task until Task 6 and Task 7 make the root compile.

## Task 6: Rewrite Touch Pages in Compose

**Files:**
- Modify: `app/src/main/java/com/xyj/focuspod/screen/StartScreen.kt`
- Modify: `app/src/main/java/com/xyj/focuspod/screen/PlanSelectScreen.kt`

- [ ] **Step 1: Replace `StartScreen.kt`**

Implement:

```kotlin
package com.xyj.focuspod.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.xyj.focuspod.model.StudySessionState
import com.xyj.focuspod.ui.component.AppScaffold
import com.xyj.focuspod.ui.theme.FocusPrimary
import com.xyj.focuspod.ui.theme.FocusPrimarySoft

@Composable
fun StartScreen(state: StudySessionState) {
    AppScaffold(alertMessage = state.alertMessage, scrollable = false) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Canvas(modifier = Modifier.size(156.dp)) {
                drawRoundRect(color = FocusPrimarySoft, size = size)
                drawRoundRect(
                    color = FocusPrimary,
                    topLeft = Offset(size.width * 0.28f, size.height * 0.18f),
                    size = Size(size.width * 0.44f, size.height * 0.64f),
                    style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                )
                drawCircle(
                    color = FocusPrimary,
                    radius = 6.dp.toPx(),
                    center = Offset(size.width / 2f, size.height * 0.70f)
                )
            }
            Spacer(modifier = Modifier.height(28.dp))
            Text(text = "XYJ 学习仓", style = MaterialTheme.typography.headlineLarge)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "正在启动，请稍候",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(48.dp))
            Text(
                text = "手机放入学习仓后，将通过语音和翻转动作完成学习闭环",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}
```

- [ ] **Step 2: Replace `PlanSelectScreen.kt`**

Replace `PlanSelectScreen.kt` using the exact file content in Appendix D.

- [ ] **Step 3: Run build**

Run:

```bash
./gradlew :app:assembleDebug
```

Expected: still FAIL if non-touch page Compose functions are missing; no errors should remain in `StartScreen.kt` or `PlanSelectScreen.kt`.

## Task 7: Rewrite Locked Flow Pages in Compose

**Files:**
- Modify: `app/src/main/java/com/xyj/focuspod/screen/DeviceSelfCheckScreen.kt`
- Modify: `app/src/main/java/com/xyj/focuspod/screen/TutoringScreen.kt`
- Modify: `app/src/main/java/com/xyj/focuspod/screen/ExamScreen.kt`
- Modify: `app/src/main/java/com/xyj/focuspod/screen/GradingScreen.kt`
- Modify: `app/src/main/java/com/xyj/focuspod/screen/GradeResultScreen.kt`
- Modify: `app/src/main/java/com/xyj/focuspod/screen/DoneScreen.kt`

- [ ] **Step 1: Rewrite `DeviceSelfCheckScreen.kt`**

Replace `DeviceSelfCheckScreen.kt` using the exact file content in Appendix E.

- [ ] **Step 2: Rewrite `TutoringScreen.kt`**

Replace `TutoringScreen.kt` using the exact file content in Appendix F.

- [ ] **Step 3: Rewrite `ExamScreen.kt`**

Replace `ExamScreen.kt` using the exact file content in Appendix G.

- [ ] **Step 4: Rewrite `GradingScreen.kt`**

Replace `GradingScreen.kt` using the exact file content in Appendix H.

- [ ] **Step 5: Rewrite `GradeResultScreen.kt`**

Replace `GradeResultScreen.kt` using the exact file content in Appendix I.

- [ ] **Step 6: Rewrite `DoneScreen.kt`**

Replace `DoneScreen.kt` using the exact file content in Appendix J.

- [ ] **Step 7: Run build**

Run:

```bash
./gradlew :app:assembleDebug
```

Expected: PASS after all screen imports and component calls are correct.

- [ ] **Step 8: Commit Tasks 5-7 together**

```bash
git add app/src/main/java/com/xyj/focuspod/MainActivity.kt app/src/main/java/com/xyj/focuspod/screen app/src/main/java/com/xyj/focuspod/ui
git commit -m "feat: rewrite app ui in compose"
```

## Task 8: Remove Old View UI Code

**Files:**
- Delete: `app/src/main/java/com/xyj/focuspod/ui/component/ViewExtensions.kt`
- Delete: `app/src/main/java/com/xyj/focuspod/ui/layout/AppPage.kt`
- Delete: `app/src/main/java/com/xyj/focuspod/ui/theme/AppColors.kt`

- [ ] **Step 1: Confirm there are no old View references**

Run:

```bash
rg -n "appPage|topStageBar|voiceCaptionBar|AppColors|applyText|setRoundedBackground|android.widget|android.view" app/src/main/java/com/xyj/focuspod
```

Expected: no matches in app UI code. `MainActivity.kt` should import Compose APIs, not `android.app.Activity` or old screen functions.

- [ ] **Step 2: Delete old View helpers**

Delete the three listed old UI helper files with `apply_patch`.

- [ ] **Step 3: Run build**

Run:

```bash
./gradlew :app:assembleDebug
```

Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/xyj/focuspod/ui/component/ViewExtensions.kt app/src/main/java/com/xyj/focuspod/ui/layout/AppPage.kt app/src/main/java/com/xyj/focuspod/ui/theme/AppColors.kt
git commit -m "refactor: remove legacy view ui helpers"
```

## Task 9: Final Verification

**Files:**
- No intended file edits.

- [ ] **Step 1: Run unit tests**

Run:

```bash
./gradlew :app:testDebugUnitTest
```

Expected: PASS.

- [ ] **Step 2: Run debug build**

Run:

```bash
./gradlew :app:assembleDebug
```

Expected: PASS.

- [ ] **Step 3: Inspect remaining diffs**

Run:

```bash
git status --short
```

Expected: only intentional code changes and the existing untracked `docs/UI设计原则.md` if it remains untracked.

- [ ] **Step 4: Manual acceptance checklist**

Check against the built UI or code review:

- 8 pages are Compose functions.
- `PlanSelectScreen` is the only page with a primary click action.
- Pages 3-8 contain no buttons.
- `AppScaffold` applies status bar and navigation bar padding.
- Scrollable pages have bottom content padding.
- Result, grading, and door status labels use tested helper functions.

- [ ] **Step 5: Commit any remaining intentional UI changes**

```bash
git add app/src/main/java app/build.gradle.kts build.gradle.kts gradle/libs.versions.toml app/src/test/java
git commit -m "chore: verify compose ui redesign"
```

Skip this commit if all implementation changes were already committed in earlier tasks.

## Self-Review Results

- Spec coverage: covered Compose dependency setup, Activity bridge, theme, shared components, all 8 pages, old View cleanup, safe areas, no-touch pages, alert overlay, and verification.
- Placeholder scan: no unspecified implementation slots remain; component tasks define exact files and responsibilities.
- Type consistency: `FocusPodApp(state: StudySessionState, flow: StudyFlow)`, `stageText(StudyStage)`, `gradingStepText(GradingStep)`, and `doorStatusText(DoorStatus)` are used consistently across tasks.

## Appendix A: `StudyChrome.kt`

```kotlin
package com.xyj.focuspod.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xyj.focuspod.ui.theme.FocusPrimary
import com.xyj.focuspod.ui.theme.FocusPrimarySoft
import com.xyj.focuspod.ui.theme.FocusSurfaceLift
import com.xyj.focuspod.ui.theme.FocusTextMuted

@Composable
fun StageTopBar(
    stageText: String,
    remainingText: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        StatusChip(text = stageText, color = FocusPrimary, textColor = Color.White)
        Text(
            text = remainingText,
            style = MaterialTheme.typography.labelLarge,
            color = FocusTextMuted
        )
    }
}

@Composable
fun StatusChip(
    text: String,
    color: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = color,
        shape = RoundedCornerShape(999.dp),
        modifier = modifier
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = textColor,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
        )
    }
}

@Composable
fun VoiceCaptionBar(
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = FocusSurfaceLift,
        shape = MaterialTheme.shapes.large,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusChip(text = "语音", color = FocusPrimarySoft, textColor = FocusPrimary)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = text.ifBlank { "等待语音提示" },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun PoseHintBar(
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = MaterialTheme.shapes.large,
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp)
        )
    }
}
```

## Appendix B: `StudyCards.kt`

```kotlin
package com.xyj.focuspod.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xyj.focuspod.model.DeviceCheckItem
import com.xyj.focuspod.model.DeviceCheckStatus
import com.xyj.focuspod.model.Question
import com.xyj.focuspod.model.StudyPlan
import com.xyj.focuspod.ui.theme.FocusPrimary
import com.xyj.focuspod.ui.theme.FocusPrimarySoft
import com.xyj.focuspod.ui.theme.FocusSuccess
import com.xyj.focuspod.ui.theme.FocusWarning

@Composable
fun PlanCard(
    plan: StudyPlan,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (selected) FocusPrimary else MaterialTheme.colorScheme.surfaceVariant
    val containerColor = if (selected) FocusPrimarySoft else MaterialTheme.colorScheme.surface

    OutlinedCard(
        colors = CardDefaults.outlinedCardColors(containerColor = containerColor),
        border = BorderStroke(if (selected) 2.dp else 1.dp, borderColor),
        shape = MaterialTheme.shapes.large,
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = plan.title, style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = plan.subject,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (selected) {
                    StatusChip(text = "已选择", color = FocusPrimary, textColor = Color.White)
                }
            }
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = "约 ${plan.estimatedMinutes} 分钟 · ${plan.exampleQuestions.size + plan.examQuestions.size} 题 · ${plan.passScore} 分达标",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun QuestionCard(
    question: Question?,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.extraLarge,
        tonalElevation = 3.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = question?.title ?: "等待题目",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = question?.prompt.orEmpty(),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun AiGuidePanel(
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.large,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "AI 引导",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = text, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
fun DeviceCheckList(
    checks: List<DeviceCheckItem>,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.extraLarge,
        tonalElevation = 3.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            checks.forEach { item ->
                val statusText = when (item.status) {
                    DeviceCheckStatus.CHECKING -> "检查中"
                    DeviceCheckStatus.PASSED -> "已通过"
                    DeviceCheckStatus.FAILED -> "未通过"
                }
                val statusColor = when (item.status) {
                    DeviceCheckStatus.CHECKING -> MaterialTheme.colorScheme.onSurfaceVariant
                    DeviceCheckStatus.PASSED -> FocusSuccess
                    DeviceCheckStatus.FAILED -> FocusWarning
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = item.name, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelLarge,
                        color = statusColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
```

## Appendix C: `ResultPanels.kt`

```kotlin
package com.xyj.focuspod.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.xyj.focuspod.model.DoorStatus
import com.xyj.focuspod.model.GradeResult
import com.xyj.focuspod.model.GradingStep
import com.xyj.focuspod.ui.theme.FocusCamera
import com.xyj.focuspod.ui.theme.FocusPrimary
import com.xyj.focuspod.ui.theme.FocusSuccess
import com.xyj.focuspod.ui.theme.FocusWarning

@Composable
fun GradingCameraPanel(
    step: GradingStep,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(FocusCamera),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                text = gradingStepText(step),
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = "请保持手机稳定",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.76f)
            )
        }
    }
}

@Composable
fun GradeResultPanel(
    result: GradeResult?,
    countdownSeconds: Int,
    modifier: Modifier = Modifier
) {
    val passed = result?.passed == true
    val resultColor = if (passed) FocusSuccess else FocusWarning
    val title = if (passed) "本题通过" else "本题未通过"

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.extraLarge,
        tonalElevation = 4.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(28.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge,
                color = resultColor,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "${result?.score ?: 0} 分",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = result?.feedback.orEmpty(),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            StatusChip(
                text = "${countdownSeconds} 秒后进入下一步",
                color = FocusPrimary,
                textColor = Color.White
            )
        }
    }
}

@Composable
fun DoorStatusPanel(
    totalExamScore: Int,
    completedQuestionCount: Int,
    mistakeCount: Int,
    doorStatus: DoorStatus,
    modifier: Modifier = Modifier
) {
    val statusColor = when (doorStatus) {
        DoorStatus.OPENING -> FocusPrimary
        DoorStatus.OPENED -> FocusSuccess
        DoorStatus.FAILED -> FocusWarning
    }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.extraLarge,
        tonalElevation = 4.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(28.dp)
        ) {
            Text(
                text = "学习完成",
                style = MaterialTheme.typography.headlineLarge,
                color = FocusSuccess,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = "考试得分 ${totalExamScore} 分 · 完成 ${completedQuestionCount} 题 · 错题已处理 ${mistakeCount} 题",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(28.dp))
            Text(
                text = doorStatusText(doorStatus),
                style = MaterialTheme.typography.headlineMedium,
                color = statusColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
```

## Appendix D: `PlanSelectScreen.kt`

```kotlin
package com.xyj.focuspod.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xyj.focuspod.flow.StudyFlow
import com.xyj.focuspod.model.StudyPlan
import com.xyj.focuspod.model.StudySessionState
import com.xyj.focuspod.ui.component.AppScaffold
import com.xyj.focuspod.ui.component.PlanCard
import com.xyj.focuspod.ui.component.StatusChip
import com.xyj.focuspod.ui.theme.FocusPrimary
import com.xyj.focuspod.ui.theme.FocusSuccess
import com.xyj.focuspod.ui.theme.FocusWarning

@Composable
fun PlanSelectScreen(
    state: StudySessionState,
    flow: StudyFlow
) {
    var selectedPlan by remember(state.availablePlans) {
        mutableStateOf<StudyPlan?>(state.availablePlans.firstOrNull())
    }

    AppScaffold(
        alertMessage = state.alertMessage,
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.extraLarge,
                tonalElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    enabled = selectedPlan != null,
                    onClick = { selectedPlan?.let(flow::selectPlan) },
                    colors = ButtonDefaults.buttonColors(containerColor = FocusPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                        .height(56.dp)
                ) {
                    Text(text = "开始本计划", style = MaterialTheme.typography.titleLarge)
                }
            }
        }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(text = "选择学习计划", style = MaterialTheme.typography.headlineLarge)
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = state.studentName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                StatusChip(
                    text = if (state.networkOnline) "网络在线" else "网络离线",
                    color = if (state.networkOnline) FocusSuccess else FocusWarning,
                    textColor = MaterialTheme.colorScheme.onPrimary
                )
            }
            Spacer(modifier = Modifier.height(28.dp))

            if (state.availablePlans.isEmpty()) {
                Text(
                    text = "暂无学习计划，请联系老师",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                state.availablePlans.forEach { plan ->
                    PlanCard(
                        plan = plan,
                        selected = selectedPlan == plan,
                        onClick = { selectedPlan = plan }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}
```

## Appendix E: `DeviceSelfCheckScreen.kt`

```kotlin
package com.xyj.focuspod.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.xyj.focuspod.model.StudySessionState
import com.xyj.focuspod.ui.component.AppScaffold
import com.xyj.focuspod.ui.component.DeviceCheckList
import com.xyj.focuspod.ui.component.PoseHintBar
import com.xyj.focuspod.ui.component.VoiceCaptionBar

@Composable
fun DeviceSelfCheckScreen(state: StudySessionState) {
    AppScaffold(alertMessage = state.alertMessage) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "设备自检中，请勿触摸手机",
                style = MaterialTheme.typography.headlineLarge
            )
            Spacer(modifier = Modifier.height(22.dp))
            DeviceCheckList(checks = state.deviceChecks)
            Spacer(modifier = Modifier.height(18.dp))
            PoseHintBar(text = "请确认手机仓 USB 已连接，系统会自动进入学习流程。")
            Spacer(modifier = Modifier.height(14.dp))
            VoiceCaptionBar(text = state.voiceCaption)
        }
    }
}
```

## Appendix F: `TutoringScreen.kt`

```kotlin
package com.xyj.focuspod.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.xyj.focuspod.model.QuestionSource
import com.xyj.focuspod.model.StudySessionState
import com.xyj.focuspod.ui.component.AiGuidePanel
import com.xyj.focuspod.ui.component.AppScaffold
import com.xyj.focuspod.ui.component.PoseHintBar
import com.xyj.focuspod.ui.component.QuestionCard
import com.xyj.focuspod.ui.component.StageTopBar
import com.xyj.focuspod.ui.component.VoiceCaptionBar

@Composable
fun TutoringScreen(state: StudySessionState) {
    val question = state.currentQuestion
    val guideText = if (question?.source == QuestionSource.MISTAKE_REVIEW) {
        "这是一道错题复盘例题。先回忆题目条件，再找出关系式，最后检查单位。"
    } else {
        "先读题找已知条件，再把问题拆成两步。系统只做引导，不直接给最终答案。"
    }

    AppScaffold(alertMessage = state.alertMessage) {
        Column(modifier = Modifier.fillMaxWidth()) {
            StageTopBar(stageText = "例题讲解", remainingText = "剩余 ${state.remainingCount} 题")
            Spacer(modifier = Modifier.height(18.dp))
            QuestionCard(question = question)
            Spacer(modifier = Modifier.height(16.dp))
            AiGuidePanel(text = guideText)
            Spacer(modifier = Modifier.height(16.dp))
            PoseHintBar(text = "请保持坐姿端正。完成后请翻转手机开始批卷。")
            Spacer(modifier = Modifier.height(14.dp))
            VoiceCaptionBar(text = state.voiceCaption)
        }
    }
}
```

## Appendix G: `ExamScreen.kt`

```kotlin
package com.xyj.focuspod.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.xyj.focuspod.model.StudySessionState
import com.xyj.focuspod.ui.component.AppScaffold
import com.xyj.focuspod.ui.component.PoseHintBar
import com.xyj.focuspod.ui.component.QuestionCard
import com.xyj.focuspod.ui.component.StageTopBar
import com.xyj.focuspod.ui.component.VoiceCaptionBar

@Composable
fun ExamScreen(state: StudySessionState) {
    AppScaffold(alertMessage = state.alertMessage) {
        Column(modifier = Modifier.fillMaxWidth()) {
            StageTopBar(stageText = "考试", remainingText = "剩余 ${state.remainingCount} 题")
            Spacer(modifier = Modifier.height(20.dp))
            QuestionCard(question = state.currentQuestion)
            Spacer(modifier = Modifier.height(18.dp))
            PoseHintBar(text = "写完后请翻转手机提交本题。")
            Spacer(modifier = Modifier.height(14.dp))
            VoiceCaptionBar(text = state.voiceCaption)
        }
    }
}
```

## Appendix H: `GradingScreen.kt`

```kotlin
package com.xyj.focuspod.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.weight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.xyj.focuspod.model.StudySessionState
import com.xyj.focuspod.ui.component.AppScaffold
import com.xyj.focuspod.ui.component.GradingCameraPanel
import com.xyj.focuspod.ui.component.StageTopBar
import com.xyj.focuspod.ui.component.VoiceCaptionBar
import com.xyj.focuspod.ui.component.stageText

@Composable
fun GradingScreen(state: StudySessionState) {
    AppScaffold(alertMessage = state.alertMessage, scrollable = false) {
        Column(modifier = Modifier.fillMaxSize()) {
            StageTopBar(stageText = stageText(state.stage), remainingText = "剩余 ${state.remainingCount} 题")
            Spacer(modifier = Modifier.height(16.dp))
            GradingCameraPanel(
                step = state.gradingStep,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            VoiceCaptionBar(text = state.voiceCaption, modifier = Modifier.fillMaxWidth())
        }
    }
}
```

## Appendix I: `GradeResultScreen.kt`

```kotlin
package com.xyj.focuspod.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.xyj.focuspod.model.StudySessionState
import com.xyj.focuspod.ui.component.AppScaffold
import com.xyj.focuspod.ui.component.GradeResultPanel
import com.xyj.focuspod.ui.component.StageTopBar
import com.xyj.focuspod.ui.component.VoiceCaptionBar
import com.xyj.focuspod.ui.component.stageText

@Composable
fun GradeResultScreen(state: StudySessionState) {
    AppScaffold(alertMessage = state.alertMessage) {
        Column(modifier = Modifier.fillMaxWidth()) {
            StageTopBar(stageText = stageText(state.stage), remainingText = "剩余 ${state.remainingCount} 题")
            Spacer(modifier = Modifier.height(22.dp))
            GradeResultPanel(
                result = state.lastGradeResult,
                countdownSeconds = state.resultCountdownSeconds
            )
            Spacer(modifier = Modifier.height(14.dp))
            VoiceCaptionBar(text = state.voiceCaption)
        }
    }
}
```

## Appendix J: `DoneScreen.kt`

```kotlin
package com.xyj.focuspod.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.xyj.focuspod.model.StudySessionState
import com.xyj.focuspod.ui.component.AppScaffold
import com.xyj.focuspod.ui.component.DoorStatusPanel
import com.xyj.focuspod.ui.component.VoiceCaptionBar

@Composable
fun DoneScreen(state: StudySessionState) {
    AppScaffold(alertMessage = state.alertMessage) {
        Column(modifier = Modifier.fillMaxWidth()) {
            DoorStatusPanel(
                totalExamScore = state.totalExamScore,
                completedQuestionCount = state.completedQuestionCount,
                mistakeCount = state.mistakeCount,
                doorStatus = state.doorStatus
            )
            Spacer(modifier = Modifier.height(14.dp))
            VoiceCaptionBar(text = state.voiceCaption)
        }
    }
}
```
