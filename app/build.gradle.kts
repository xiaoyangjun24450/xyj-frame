import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

val versionProperties = Properties().apply {
    rootProject.file("version.properties").inputStream().use { load(it) }
}

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use { load(it) }
    }
}

fun configValue(name: String, defaultValue: String = ""): String {
    return providers.gradleProperty(name).orNull
        ?: localProperties.getProperty(name)
        ?: defaultValue
}

fun String.asBuildConfigString(): String {
    return "\"" + replace("\\", "\\\\").replace("\"", "\\\"") + "\""
}

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

        val doubaoAppId = configValue("doubao.appId")
        buildConfigField("String", "DOUBAO_APP_ID", doubaoAppId.asBuildConfigString())
        buildConfigField("String", "DOUBAO_APP_KEY", configValue("doubao.appKey", doubaoAppId).asBuildConfigString())
        buildConfigField("String", "DOUBAO_TOKEN", configValue("doubao.token").asBuildConfigString())
        buildConfigField("String", "DOUBAO_UID", configValue("doubao.uid", "student-001").asBuildConfigString())
        buildConfigField("String", "DOUBAO_RESOURCE_ID", configValue("doubao.resourceId", "volc.speech.dialog").asBuildConfigString())
        buildConfigField("String", "DOUBAO_DIALOG_ADDRESS", configValue("doubao.dialogAddress", "wss://openspeech.bytedance.com").asBuildConfigString())
        buildConfigField("String", "DOUBAO_DIALOG_URI", configValue("doubao.dialogUri", "/api/v3/realtime/dialogue").asBuildConfigString())
        buildConfigField("String", "DOUBAO_BOT_NAME", configValue("doubao.botName", "豆包").asBuildConfigString())
        buildConfigField("String", "DOUBAO_AEC_MODEL_PATH", configValue("doubao.aecModelPath").asBuildConfigString())
        buildConfigField("String", "DOUBAO_DEBUG_PATH", configValue("doubao.debugPath").asBuildConfigString())
        buildConfigField("String", "DOUBAO_RECORDER_PATH", configValue("doubao.recorderPath").asBuildConfigString())
        buildConfigField("String", "DOUBAO_PLAYER_PATH", configValue("doubao.playerPath").asBuildConfigString())
        buildConfigField("String", "DOUBAO_LOG_LEVEL", configValue("doubao.logLevel", "WARN").asBuildConfigString())
        buildConfigField("String", "DOUBAO_MULTIMODAL_API_KEY", configValue("doubao.multimodal.apiKey").asBuildConfigString())
        buildConfigField("String", "DOUBAO_MULTIMODAL_MODEL", configValue("doubao.multimodal.model").asBuildConfigString())
        buildConfigField(
            "String",
            "DOUBAO_MULTIMODAL_ENDPOINT",
            configValue("doubao.multimodal.endpoint", "https://ark.cn-beijing.volces.com/api/v3/responses").asBuildConfigString()
        )
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
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
    implementation(libs.bytedance.speechengine.tob)
    implementation(libs.okhttp)
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
}
