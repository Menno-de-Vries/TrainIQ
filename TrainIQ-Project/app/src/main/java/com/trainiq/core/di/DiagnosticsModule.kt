package com.trainiq.core.di

import com.trainiq.BuildConfig
import com.trainiq.core.diagnostics.AndroidPerformanceSessionMonitor
import com.trainiq.core.diagnostics.BreadcrumbRingBuffer
import com.trainiq.core.diagnostics.CrashContextProvider
import com.trainiq.core.diagnostics.CrashReporter
import com.trainiq.core.diagnostics.FrameAnomalyPolicy
import com.trainiq.core.diagnostics.AndroidTelemetryNetworkPolicy
import com.trainiq.core.diagnostics.ExportingTelemetry
import com.trainiq.core.diagnostics.HttpTelemetryExporter
import com.trainiq.core.diagnostics.NoOpCrashReporter
import com.trainiq.core.diagnostics.NoOpTelemetry
import com.trainiq.core.diagnostics.NoopPerfettoTraceController
import com.trainiq.core.diagnostics.OkHttpTelemetrySender
import com.trainiq.core.diagnostics.PerfettoTraceController
import com.trainiq.core.diagnostics.PerformanceSessionMonitor
import com.trainiq.core.diagnostics.TelemetryConfig
import com.trainiq.core.diagnostics.TelemetryExporter
import com.trainiq.core.diagnostics.Telemetry
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import android.content.Context
import java.util.UUID
import javax.inject.Singleton
import okhttp3.OkHttpClient

@Module
@InstallIn(SingletonComponent::class)
object DiagnosticsProvidesModule {
    @Provides
    @Singleton
    fun provideBreadcrumbRingBuffer(): BreadcrumbRingBuffer = BreadcrumbRingBuffer()

    @Provides
    @Singleton
    fun provideCrashContextProvider(breadcrumbs: BreadcrumbRingBuffer): CrashContextProvider =
        CrashContextProvider(
            breadcrumbs = breadcrumbs,
            appVersion = BuildConfig.VERSION_NAME,
            buildType = BuildConfig.BUILD_TYPE,
            sessionId = UUID.randomUUID().toString(),
        )

    @Provides
    @Singleton
    fun provideTelemetryConfig(): TelemetryConfig = TelemetryConfig.fromBuildConfig()

    @Provides
    @Singleton
    fun provideTelemetryExporter(
        @ApplicationContext context: Context,
        config: TelemetryConfig,
        okHttpClient: OkHttpClient,
    ): TelemetryExporter =
        if (config.enabled && !config.endpointUrl.isNullOrBlank()) {
            HttpTelemetryExporter(
                config = config,
                sender = OkHttpTelemetrySender(okHttpClient),
                networkPolicy = AndroidTelemetryNetworkPolicy(context),
            )
        } else {
            com.trainiq.core.diagnostics.NoOpTelemetryExporter()
        }

    @Provides
    @Singleton
    fun provideTelemetry(config: TelemetryConfig, exporter: TelemetryExporter): Telemetry =
        if (config.enabled) ExportingTelemetry(exporter) else NoOpTelemetry

    @Provides
    @Singleton
    fun provideCrashReporter(config: TelemetryConfig): CrashReporter =
        com.trainiq.core.diagnostics.DelegatingCrashReporter(
            config = config,
            delegate = NoOpCrashReporter(),
        )

    @Provides
    @Singleton
    fun provideFrameAnomalyPolicy(): FrameAnomalyPolicy = FrameAnomalyPolicy()

    @Provides
    @Singleton
    fun providePerfettoTraceController(): PerfettoTraceController = NoopPerfettoTraceController()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class DiagnosticsBindsModule {
    @Binds
    @Singleton
    abstract fun bindPerformanceSessionMonitor(
        monitor: AndroidPerformanceSessionMonitor,
    ): PerformanceSessionMonitor
}
