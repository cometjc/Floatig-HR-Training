plugins {
    id("com.android.application")
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")

    id("io.sentry.android.gradle") version "6.5.0"
}

android {
    namespace = "com.cometjc.floatighrtraining"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.cometjc.floatighrtraining"
        minSdk = 33
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.10.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.datastore:datastore-preferences:1.2.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.room:room-ktx:2.7.1")
    implementation(project(":polarBleSdk"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-rx3:1.10.2")
    ksp("androidx.room:room-compiler:2.7.1")
    testImplementation("junit:junit:4.13.2")
    testImplementation("androidx.room:room-testing:2.7.1")
    testImplementation("androidx.test:core:1.6.1")
    testImplementation("androidx.test.ext:junit:1.2.1")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
    testImplementation("org.robolectric:robolectric:4.14.1")
    debugImplementation("androidx.compose.ui:ui-tooling")
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}


sentry {
    org.set("jethroyu")
    projectName.set("floatig-hr-training")

    val allowMappingUpload = providers.environmentVariable("SENTRY_UPLOAD_PROGUARD_MAPPING")
        .map { it.equals("true", ignoreCase = true) }
        .orElse(false)
    val allowSourceContext = providers.environmentVariable("SENTRY_INCLUDE_SOURCE_CONTEXT")
        .map { it.equals("true", ignoreCase = true) }
        .orElse(false)

    autoUploadProguardMapping.set(allowMappingUpload.get())
    includeSourceContext.set(allowSourceContext.get())
}
