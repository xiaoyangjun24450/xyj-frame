pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "XYJ"
// Each immediate apps/ directory with a build file is an independent application.
file("apps").listFiles()?.filter { it.isDirectory && !it.resolve(".git").exists() && it.resolve("build.gradle.kts").isFile }
    ?.sortedBy { it.name }?.forEach { app ->
        require(app.name.matches(Regex("[a-z][a-z0-9-]*"))) { "Invalid app directory: ${app.name}" }
        include(":${app.name}")
        project(":${app.name}").projectDir = app
    }
