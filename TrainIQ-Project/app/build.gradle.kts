import java.io.File
import java.security.MessageDigest
import org.gradle.api.DefaultTask
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    id("kotlin-parcelize")
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
}

fun trainIqGitOutput(vararg arguments: String): String = runCatching {
    providers.exec {
        workingDir(rootDir.parentFile)
        commandLine("git", *arguments)
    }.standardOutput.asText.get().trim()
}.getOrDefault("unknown")

fun String.asBuildConfigString(): String =
    "\"${replace("\\", "\\\\").replace("\"", "\\\"")}\""

val trainIqGitBranch = trainIqGitOutput("branch", "--show-current").ifBlank { "detached" }
val trainIqGitShortSha = trainIqGitOutput("rev-parse", "--short=7", "HEAD")
val trainIqGitStatus = trainIqGitOutput("status", "--porcelain")
val trainIqGitDirty = trainIqGitStatus.isNotEmpty() && trainIqGitStatus != "unknown"

android {
    namespace = "com.trainiq"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.trainiq"
        minSdk = 26
        targetSdk = 36
        versionCode = 3
        versionName = "1.0.1-B"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
        buildConfigField(
            "String",
            "GEMINI_BASE_URL",
            "\"https://generativelanguage.googleapis.com/\""
        )
        buildConfigField("String", "OPENAI_BASE_URL", "\"https://api.openai.com/\"")
        buildConfigField("String", "GIT_BRANCH", trainIqGitBranch.asBuildConfigString())
        buildConfigField("String", "GIT_SHORT_SHA", trainIqGitShortSha.asBuildConfigString())
        buildConfigField("Boolean", "GIT_DIRTY", trainIqGitDirty.toString())
        buildConfigField("Boolean", "TELEMETRY_ENABLED", "false")
        buildConfigField("String", "TELEMETRY_ENDPOINT_URL", "\"\"")
        buildConfigField("Double", "TELEMETRY_SAMPLE_RATE", "0.0")
        buildConfigField("Boolean", "TELEMETRY_UPLOAD_WIFI_ONLY", "true")
        buildConfigField("Integer", "TELEMETRY_MAX_BATCH_SIZE", "20")
        buildConfigField("Long", "TELEMETRY_FLUSH_INTERVAL_MILLIS", "60000L")
        buildConfigField("Boolean", "TELEMETRY_PERFETTO_ENABLED", "false")
        buildConfigField("Boolean", "TELEMETRY_CRASH_CONTEXT_ENABLED", "true")
        buildConfigField(
            "Boolean",
            "SAMSUNG_HEALTH_DATA_SDK_AAR_PRESENT",
            File(projectDir, "libs")
                .walkTopDown()
                .any { file ->
                    file.isFile &&
                        file.extension.equals("aar", ignoreCase = true) &&
                        file.name.contains("samsung-health-data-api", ignoreCase = true)
                }
                .toString(),
        )
        buildConfigField(
            "Boolean",
            "SAMSUNG_HEALTH_NON_API_AAR_PRESENT",
            File(projectDir, "libs")
                .walkTopDown()
                .any { file ->
                    file.isFile &&
                        file.extension.equals("aar", ignoreCase = true) &&
                        file.name.contains("samsung-health", ignoreCase = true) &&
                        !file.name.contains("samsung-health-data-api", ignoreCase = true)
                }
                .toString(),
        )
    }

    signingConfigs {
        create("release") {
            val trainIqStoreFile = trainIqSigningValue("TRAINIQ_KEYSTORE_FILE", "trainiq.keystoreFile")
            val trainIqStorePassword = trainIqSigningValue("TRAINIQ_KEYSTORE_PASSWORD", "trainiq.keystorePassword")
            val trainIqKeyAlias = trainIqSigningValue("TRAINIQ_KEY_ALIAS", "trainiq.keyAlias")
            val trainIqKeyPassword = trainIqSigningValue("TRAINIQ_KEY_PASSWORD", "trainiq.keyPassword")

            if (
                trainIqStoreFile.isPresent &&
                trainIqStorePassword.isPresent &&
                trainIqKeyAlias.isPresent &&
                trainIqKeyPassword.isPresent
            ) {
                storeFile = file(trainIqStoreFile.get())
                storePassword = trainIqStorePassword.get()
                keyAlias = trainIqKeyAlias.get()
                keyPassword = trainIqKeyPassword.get()
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            signingConfig = signingConfigs.getByName("release").takeIf { it.storeFile != null }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        create("profileable") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("debug")
            isDebuggable = false
            isProfileable = true
            matchingFallbacks += listOf("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    sourceSets {
        getByName("androidTest").assets.srcDir("src/test/resources")
        getByName("androidTest").assets.srcDir("$projectDir/schemas")
        getByName("debug").assets.srcDir("$buildDir/generated/roomMigrationVerification/debug/assets")
        getByName("release").assets.srcDir("$buildDir/generated/roomMigrationVerification/release/assets")
        getByName("profileable").assets.srcDir("$buildDir/generated/roomMigrationVerification/profileable/assets")
    }
}

fun trainIqSigningValue(envName: String, propertyName: String) =
    providers.environmentVariable(envName).orElse(providers.gradleProperty(propertyName))

abstract class CheckReleaseSigningReadinessTask : DefaultTask() {
    @get:Input
    abstract val signingInputs: MapProperty<String, String>

    @get:Input
    abstract val projectDirectoryPath: Property<String>

    @TaskAction
    fun checkSigningInputs() {
        val inputs = signingInputs.get()
        val present = inputs.filterValues(String::isNotBlank).keys
        if (present.isEmpty()) {
            error("TrainIQ release signing is required for release artifacts.")
        }
        val missing = inputs.filterValues(String::isBlank).keys
        check(missing.isEmpty()) {
            "Incomplete TrainIQ release signing configuration. Missing: ${missing.joinToString()}"
        }
        val configuredPath = inputs.getValue("TRAINIQ_KEYSTORE_FILE/trainiq.keystoreFile")
        val configuredFile = File(configuredPath)
        val storeFile = if (configuredFile.isAbsolute) {
            configuredFile
        } else {
            File(projectDirectoryPath.get(), configuredPath)
        }
        check(storeFile.isFile) {
            "Configured TrainIQ keystore file does not exist: ${storeFile.absolutePath}"
        }
        logger.lifecycle("TrainIQ release signing configuration is complete.")
    }
}

tasks.register<CheckReleaseSigningReadinessTask>("checkReleaseSigningReadiness") {
    group = "verification"
    description = "Verifies TrainIQ release signing configuration is complete before release artifacts are packaged."
    projectDirectoryPath.set(layout.projectDirectory.asFile.absolutePath)
    signingInputs.putAll(
        mapOf(
            "TRAINIQ_KEYSTORE_FILE/trainiq.keystoreFile" to trainIqSigningValue("TRAINIQ_KEYSTORE_FILE", "trainiq.keystoreFile").orNull.orEmpty(),
            "TRAINIQ_KEYSTORE_PASSWORD/trainiq.keystorePassword" to trainIqSigningValue("TRAINIQ_KEYSTORE_PASSWORD", "trainiq.keystorePassword").orNull.orEmpty(),
            "TRAINIQ_KEY_ALIAS/trainiq.keyAlias" to trainIqSigningValue("TRAINIQ_KEY_ALIAS", "trainiq.keyAlias").orNull.orEmpty(),
            "TRAINIQ_KEY_PASSWORD/trainiq.keyPassword" to trainIqSigningValue("TRAINIQ_KEY_PASSWORD", "trainiq.keyPassword").orNull.orEmpty(),
        ),
    )
}

tasks.matching { it.name in setOf("assembleRelease", "bundleRelease", "packageRelease") }.configureEach {
    dependsOn("checkReleaseSigningReadiness")
}

fun samsungHealthDataSdkApiAarFiles(): List<File> {
    val libsDir = File(projectDir, "libs")
    if (!libsDir.isDirectory) return emptyList()
    return libsDir.walkTopDown()
        .filter { file ->
            file.isFile &&
                file.extension.equals("aar", ignoreCase = true) &&
                file.name.contains("samsung-health-data-api", ignoreCase = true)
        }
        .toList()
}

tasks.register("checkSamsungHealthDataSdkReadiness") {
    group = "verification"
    description = "Verifies the Samsung Health Data SDK API AAR is present before physical Samsung Health step parity builds."

    doLast {
        val libsDir = File(projectDir, "libs")
        val apiAars = samsungHealthDataSdkApiAarFiles()
        check(apiAars.isNotEmpty()) {
            "Samsung Health Data SDK API AAR missing. Add samsung-health-data-api*.aar to ${libsDir.absolutePath} before physical Samsung Health All steps parity builds."
        }
        logger.lifecycle("Samsung Health Data SDK API AAR ready: ${apiAars.joinToString { it.name }}")
    }
}

tasks.register("assembleSamsungHealthParityDebug") {
    group = "verification"
    description = "Assembles a debug build that is eligible for physical Samsung Health All steps parity testing."
    dependsOn("checkSamsungHealthDataSdkReadiness", "assembleDebug")
}

tasks.register("installSamsungHealthParityDebug") {
    group = "verification"
    description = "Installs a debug build only when the Samsung Health Data SDK API AAR is present for All steps parity testing."
    dependsOn("checkSamsungHealthDataSdkReadiness", "installDebug")
}

if (providers.gradleProperty("trainiq.requireSamsungHealthDataSdk").map(String::toBoolean).orElse(false).get()) {
    tasks.matching { task ->
        task.name in setOf(
            "assembleDebug",
            "installDebug",
            "assembleProfileable",
            "installProfileable",
        )
    }.configureEach {
        dependsOn("checkSamsungHealthDataSdkReadiness")
    }
}

fun registerRoomMigrationChainVerificationMarkerTask(
    buildVariant: String,
    taskName: String,
) = tasks.register(taskName) {
    group = "verification"
    description = "Generates a $buildVariant Room migration-chain verification marker after connected migration tests pass."
    dependsOn("connectedDebugAndroidTest")

    val outputFile = layout.buildDirectory.file(
        "generated/roomMigrationVerification/$buildVariant/assets/room_migration_chain_verification_marker.json",
    )
    val verifiedAtMillisProperty = providers.gradleProperty("roomMigrationVerificationTimestampMillis")
    outputs.file(outputFile)
    inputs.property(
        "roomMigrationVerificationTimestampMillis",
        verifiedAtMillisProperty.orElse(""),
    )

    doLast {
        val marker = "trainiq-room-migration-chain-v2-to-v16"
        val testTask = "connectedDebugAndroidTest"
        val currentRoomVersion = 16
        val requiredStartVersion = 2
        val requiredEndVersion = 16
        val coveredStartVersion = 2
        val coveredEndVersion = 16
        val verifiedAtMillis = verifiedAtMillisProperty
            .map(String::toLong)
            .getOrElse(System.currentTimeMillis())
        val migrationCount = 12
        val payloadForHash = listOf(
            marker,
            buildVariant,
            testTask,
            currentRoomVersion.toString(),
            requiredStartVersion.toString(),
            requiredEndVersion.toString(),
            coveredStartVersion.toString(),
            coveredEndVersion.toString(),
            verifiedAtMillis.toString(),
            migrationCount.toString(),
        ).joinToString(separator = "|")
        val hash = MessageDigest.getInstance("SHA-256")
            .digest(payloadForHash.toByteArray(Charsets.UTF_8))
            .joinToString(separator = "") { byte -> "%02x".format(byte) }
        val payloadWithoutHash = """
            {
              "marker": "$marker",
              "buildVariant": "$buildVariant",
              "testTask": "$testTask",
              "currentRoomVersion": $currentRoomVersion,
              "requiredStartVersion": $requiredStartVersion,
              "requiredEndVersion": $requiredEndVersion,
              "coveredStartVersion": $coveredStartVersion,
              "coveredEndVersion": $coveredEndVersion,
              "verifiedAtMillis": $verifiedAtMillis,
              "migrationCount": $migrationCount
            }
        """.trimIndent()
        outputFile.get().asFile.apply {
            parentFile.mkdirs()
            writeText(
                payloadWithoutHash
                    .replace("\n}", ",\n  \"payloadSha256\": \"$hash\"\n}")
                    .plus("\n"),
            )
        }
    }
}

val generateDebugRoomMigrationChainVerificationMarker = registerRoomMigrationChainVerificationMarkerTask(
    buildVariant = "debug",
    taskName = "generateDebugRoomMigrationChainVerificationMarker",
)

val generateReleaseRoomMigrationChainVerificationMarker = registerRoomMigrationChainVerificationMarkerTask(
    buildVariant = "release",
    taskName = "generateReleaseRoomMigrationChainVerificationMarker",
)

val generateProfileableRoomMigrationChainVerificationMarker = registerRoomMigrationChainVerificationMarkerTask(
    buildVariant = "profileable",
    taskName = "generateProfileableRoomMigrationChainVerificationMarker",
)

tasks.register("generateCiRoomMigrationChainVerificationMarkers") {
    group = "verification"
    description = "Generates all CI Room migration-chain verification markers after connected migration tests pass."
    dependsOn(
        generateDebugRoomMigrationChainVerificationMarker,
        generateReleaseRoomMigrationChainVerificationMarker,
        generateProfileableRoomMigrationChainVerificationMarker,
    )
}

kotlin {
    jvmToolchain(17)
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.incremental", "true")
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.aar"))))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.metrics.performance)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.compose.runtime)
    implementation(libs.compose.foundation)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material3.window.size)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.jmaterial)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.gson)
    implementation(libs.reorderable)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.health.connect.client)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.coil.compose)
    implementation(libs.mlkit.barcode)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.app.cash.turbine)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.room.testing)

    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
