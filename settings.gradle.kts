pluginManagement {
    repositories {
        google()
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

rootProject.name = "FloatingHrTraining"
include(":app")
include(":polarBleSdk")
project(":polarBleSdk").projectDir = file("ble-provider/polar-ble-sdk/sources/Android/android-communications/library")
