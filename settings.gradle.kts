// settings.gradle.kts

pluginManagement {
    repositories {
        // only plugins from com.android*, com.google* and androidx*
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        // core plugin repos
        mavenCentral()
        gradlePluginPortal()
        // Compose Compiler snapshots
        maven("https://androidx.dev/storage/compose-compiler/repository")
    }
}

dependencyResolutionManagement {
    // Prevent modules from declaring their own repositories
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    repositories {
        google()
        mavenCentral()
        // add this for Compose Compiler artifacts not yet on Maven Central
        maven("https://androidx.dev/storage/compose-compiler/repository")
    }
}

rootProject.name = "My Application"
include(":app")
