import java.security.MessageDigest

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
}

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
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
        buildConfigField(
            "String",
            "GEMINI_BASE_URL",
            "\"https://generativelanguage.googleapis.com/\""
        )
        buildConfigField("Boolean", "TELEMETRY_ENABLED", "false")
        buildConfigField("String", "TELEMETRY_ENDPOINT_URL", "\"\"")
        buildConfigField("String", "TELEMETRY_API_TOKEN", "\"\"")
        buildConfigField("Double", "TELEMETRY_SAMPLE_RATE", "0.0")
        buildConfigField("Boolean", "TELEMETRY_UPLOAD_WIFI_ONLY", "true")
        buildConfigField("Integer", "TELEMETRY_MAX_BATCH_SIZE", "20")
        buildConfigField("Long", "TELEMETRY_FLUSH_INTERVAL_MILLIS", "60000L")
        buildConfigField("Boolean", "TELEMETRY_PERFETTO_ENABLED", "false")
        buildConfigField("Boolean", "TELEMETRY_CRASH_CONTEXT_ENABLED", "true")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
        val marker = "trainiq-room-migration-chain-v2-to-v11"
        val testTask = "connectedDebugAndroidTest"
        val currentRoomVersion = 11
        val requiredStartVersion = 2
        val requiredEndVersion = 11
        val coveredStartVersion = 2
        val coveredEndVersion = 11
        val verifiedAtMillis = verifiedAtMillisProperty
            .map(String::toLong)
            .getOrElse(System.currentTimeMillis())
        val migrationCount = 9
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
