import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import java.util.Properties

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinCocoapods)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.androidApplication)
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use(::load)
    }
}

val naverMapClientId = providers
    .gradleProperty("naver.map.client.id")
    .orElse(localProperties.getProperty("naver.map.client.id", ""))

val generatedSampleConfigDir = layout.buildDirectory.dir("generated/source/sampleConfig/commonMain/kotlin")

val generateSampleNaverMapClientId by tasks.registering {
    inputs.property("naverMapClientId", naverMapClientId)
    outputs.dir(generatedSampleConfigDir)

    doLast {
        val clientId = naverMapClientId.orNull?.trim().orEmpty()
        if (clientId.isBlank()) {
            error("Missing naver.map.client.id. Add it to local.properties or pass -Pnaver.map.client.id.")
        }

        val outputFile = generatedSampleConfigDir.get()
            .file("io/github/hyungju/navermap/sample/SampleNaverMapClientId.kt")
            .asFile
        outputFile.parentFile.mkdirs()
        outputFile.writeText(
            """
            package io.github.hyungju.navermap.sample

            internal const val SampleNaverMapClientId = "${clientId.escapeForKotlinString()}"
            """.trimIndent() + "\n",
        )
    }
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    cocoapods {
        summary = "Compose Multiplatform sample app for naver-map-compose"
        homepage = "https://github.com/HyungJu/naver-map-compose-multiplatform"
        version = project.version.toString()
        ios.deploymentTarget = "16.0"
        podfile = project.file("../iosApp/Podfile")
        pod("NMapsMap")

        framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    sourceSets {
        named("commonMain") {
            kotlin.srcDir(generatedSampleConfigDir)

            dependencies {
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.runtime)
                implementation(compose.ui)
                implementation(libs.jetbrains.navigation.compose)
                implementation(projects.naverMapCompose)
            }
        }

        named("androidMain") {
            dependencies {
                implementation(libs.androidx.activity.compose)
            }
        }
    }
}

android {
    namespace = "io.github.hyungju.navermap.sample"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "io.github.hyungju.navermap.sample"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = project.version.toString()
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    debugImplementation(libs.compose.ui.tooling)
}

tasks.withType<KotlinCompilationTask<*>>().configureEach {
    dependsOn(generateSampleNaverMapClientId)
}

fun String.escapeForKotlinString(): String = buildString(length) {
    for (char in this@escapeForKotlinString) {
        when (char) {
            '\\' -> append("\\\\")
            '"' -> append("\\\"")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> append(char)
        }
    }
}
