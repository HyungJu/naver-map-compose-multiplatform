import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.jvm.tasks.Jar
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.util.Base64

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinCocoapods)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.androidLibrary)
    `maven-publish`
    signing
}

fun Project.gradlePropertyOrEnv(propertyName: String, envName: String) =
    providers.gradleProperty(propertyName).orElse(providers.environmentVariable(envName))

val isSnapshotVersion = version.toString().endsWith("-SNAPSHOT")
val mavenCentralUsername = gradlePropertyOrEnv("mavenCentralUsername", "MAVEN_CENTRAL_USERNAME")
val mavenCentralPassword = gradlePropertyOrEnv("mavenCentralPassword", "MAVEN_CENTRAL_PASSWORD")
val signingKeyId = gradlePropertyOrEnv("signingInMemoryKeyId", "MAVEN_CENTRAL_GPG_KEY_ID")
val signingKey = gradlePropertyOrEnv("signingInMemoryKey", "MAVEN_CENTRAL_GPG_PRIVATE_KEY")
val signingPassword = gradlePropertyOrEnv("signingInMemoryKeyPassword", "MAVEN_CENTRAL_GPG_PASSPHRASE")
val mavenCentralStagingHost = providers.gradleProperty("mavenCentralStagingHost")
val mavenCentralSnapshotsUrl = providers.gradleProperty("mavenCentralSnapshotsUrl")
val mavenCentralNamespace = providers.gradleProperty("mavenCentralNamespace")
val mavenCentralPublishingType = providers.gradleProperty("mavenCentralPublishingType").orElse("automatic")

val javadocJar by tasks.registering(Jar::class) {
    archiveClassifier.set("javadoc")
    from(rootProject.layout.projectDirectory.file("README.md"))
    from(rootProject.layout.projectDirectory.file("LICENSE"))
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
        if (name != "kotlinMultiplatform") {
            artifact(javadocJar)
        }

        pom {
            name.set(providers.gradleProperty("POM_NAME"))
            description.set(providers.gradleProperty("POM_DESCRIPTION"))
            url.set(providers.gradleProperty("POM_URL"))

            licenses {
                license {
                    name.set(providers.gradleProperty("POM_LICENSE_NAME"))
                    url.set(providers.gradleProperty("POM_LICENSE_URL"))
                }
            }

            developers {
                developer {
                    id.set(providers.gradleProperty("POM_DEVELOPER_ID"))
                    name.set(providers.gradleProperty("POM_DEVELOPER_NAME"))
                    url.set(providers.gradleProperty("POM_DEVELOPER_URL"))
                }
            }

            scm {
                url.set(providers.gradleProperty("POM_SCM_URL"))
                connection.set(providers.gradleProperty("POM_SCM_CONNECTION"))
                developerConnection.set(providers.gradleProperty("POM_SCM_DEV_CONNECTION"))
            }
        }
    }

    repositories {
        maven {
            name = "mavenCentral"
            url = uri(
                if (isSnapshotVersion) {
                    mavenCentralSnapshotsUrl.get()
                } else {
                    "${mavenCentralStagingHost.get().removeSuffix("/")}/service/local/staging/deploy/maven2/"
                }
            )

            credentials {
                username = mavenCentralUsername.orNull ?: ""
                password = mavenCentralPassword.orNull ?: ""
            }
        }
    }
}

signing {
    val isRemotePublish = gradle.startParameter.taskNames.any { taskName ->
        taskName.contains("publish", ignoreCase = true) &&
            !taskName.contains("MavenLocal", ignoreCase = true)
    }

    isRequired = isRemotePublish

    val key = signingKey.orNull
    val password = signingPassword.orNull
    if (key != null && password != null) {
        useInMemoryPgpKeys(signingKeyId.orNull, key, password)
    }

    sign(publishing.publications)
}

val publishAllPublicationsToMavenCentral = tasks.named("publishAllPublicationsToMavenCentralRepository")

val releaseMavenCentralRepository by tasks.registering {
    group = "publishing"
    description = "Closes and releases the uploaded Sonatype Central staging repository."
    dependsOn(publishAllPublicationsToMavenCentral)
    onlyIf { !isSnapshotVersion }

    doLast {
        val username = mavenCentralUsername.orNull
            ?: error("Missing Maven Central username. Set mavenCentralUsername or MAVEN_CENTRAL_USERNAME.")
        val password = mavenCentralPassword.orNull
            ?: error("Missing Maven Central password. Set mavenCentralPassword or MAVEN_CENTRAL_PASSWORD.")

        val authToken = Base64.getEncoder()
            .encodeToString("$username:$password".toByteArray(StandardCharsets.UTF_8))
        val endpoint = URI.create(
            "${mavenCentralStagingHost.get().removeSuffix("/")}/manual/upload/defaultRepository/" +
                "${mavenCentralNamespace.get()}?publishing_type=${mavenCentralPublishingType.get()}"
        )
        val request = HttpRequest.newBuilder(endpoint)
            .header("Authorization", "Bearer $authToken")
            .header("Accept", "application/json")
            .POST(HttpRequest.BodyPublishers.noBody())
            .build()
        val response = HttpClient.newHttpClient()
            .send(request, HttpResponse.BodyHandlers.ofString())

        if (response.statusCode() !in 200..299) {
            error(
                "Failed to release Maven Central deployment (${response.statusCode()}): ${response.body()}"
            )
        }

        logger.lifecycle("Maven Central deployment released successfully: ${response.body()}")
    }
}

tasks.register("publishToMavenCentral") {
    group = "publishing"
    description = "Publishes all naver-map-compose artifacts to Maven Central and finalizes non-SNAPSHOT releases."
    dependsOn(if (isSnapshotVersion) publishAllPublicationsToMavenCentral else releaseMavenCentralRepository)
}
