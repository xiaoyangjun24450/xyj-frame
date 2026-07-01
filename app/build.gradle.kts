import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

val versionProperties = Properties().apply {
    rootProject.file("version.properties").inputStream().use { load(it) }
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
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
}
