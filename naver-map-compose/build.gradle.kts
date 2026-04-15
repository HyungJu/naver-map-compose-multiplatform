import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.gradle.api.publish.maven.MavenPublication

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinCocoapods)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.androidLibrary)
    `maven-publish`
}

group = "io.github.jude.navermap"
version = "0.1.0-SNAPSHOT"

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
        summary = "Compose Multiplatform wrapper for NAVER Map on Android and iOS"
        homepage = "https://github.com/jude/naver-map-compose-multiplatform"
        version = project.version.toString()
        ios.deploymentTarget = "16.0"

        framework {
            baseName = "NaverMapCompose"
            isStatic = true
        }

        pod("NMapsMap")
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.runtime)
            implementation(compose.ui)
        }

        androidMain.dependencies {
            implementation(libs.naver.map.android)
        }
    }
}

android {
    namespace = "io.github.jude.navermap.compose"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

publishing {
    publications.withType<MavenPublication>().configureEach {
        pom {
            name.set("naver-map-compose")
            description.set("Compose Multiplatform wrapper for NAVER Map")
            url.set("https://github.com/jude/naver-map-compose-multiplatform")

            licenses {
                license {
                    name.set("Apache License 2.0")
                    url.set("https://www.apache.org/licenses/LICENSE-2.0")
                }
            }

            developers {
                developer {
                    id.set("jude")
                    name.set("Jude")
                }
            }

            scm {
                url.set("https://github.com/jude/naver-map-compose-multiplatform")
            }
        }
    }
}
