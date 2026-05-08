package com.trainiq.core.di

import android.content.Context
import androidx.room.Room
import androidx.work.WorkManager
import com.trainiq.BuildConfig
import com.trainiq.core.database.TrainIqDao
import com.trainiq.core.database.TrainIqDatabase
import com.trainiq.core.database.TrainIqMigrations
import com.trainiq.core.security.AndroidKeystoreGeminiKeyStore
import com.trainiq.core.security.GeminiEncryptedKeyStore
import com.trainiq.data.migration.JsonRoomImportPlanner
import com.trainiq.data.migration.JsonRoomImportSink
import com.trainiq.data.migration.RoomJsonImportSink
import com.trainiq.data.migration.AssetRoomMigrationChainVerificationMarkerSource
import com.trainiq.data.migration.RoomMigrationChainVerificationMarkerSource
import com.trainiq.data.migration.RoomRuntimeReadinessGate
import com.trainiq.data.remote.GeminiApi
import com.trainiq.data.repository.RoomCoachRepository
import com.trainiq.data.repository.RoomHomeRepository
import com.trainiq.data.repository.RoomNutritionRepository
import com.trainiq.data.repository.RoomProgressRepository
import com.trainiq.data.repository.RoomWorkoutRepository
import com.trainiq.domain.repository.CoachRepository
import com.trainiq.domain.repository.HomeRepository
import com.trainiq.domain.repository.NutritionRepository
import com.trainiq.domain.repository.ProgressRepository
import com.trainiq.domain.repository.WorkoutRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .callTimeout(30, TimeUnit.SECONDS)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .writeTimeout(25, TimeUnit.SECONDS)
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.NONE
                },
            )
            .build()

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.GEMINI_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides
    @Singleton
    fun provideGeminiApi(retrofit: Retrofit): GeminiApi = retrofit.create(GeminiApi::class.java)

    @Provides
    @Singleton
    fun provideGeminiEncryptedKeyStore(store: AndroidKeystoreGeminiKeyStore): GeminiEncryptedKeyStore = store

    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager = WorkManager.getInstance(context)

    @Provides
    @Singleton
    fun provideTrainIqDatabase(@ApplicationContext context: Context): TrainIqDatabase =
        Room.databaseBuilder(
            context,
            TrainIqDatabase::class.java,
            "trainiq.db",
        )
            .addMigrations(*TrainIqMigrations.All)
            .build()

    @Provides
    fun provideTrainIqDao(database: TrainIqDatabase): TrainIqDao = database.dao()

    @Provides
    fun provideRoomRuntimeReadinessGate(dao: TrainIqDao): RoomRuntimeReadinessGate =
        RoomRuntimeReadinessGate(dao)

    @Provides
    @Singleton
    fun provideRoomMigrationChainVerificationMarkerSource(
        source: AssetRoomMigrationChainVerificationMarkerSource,
    ): RoomMigrationChainVerificationMarkerSource = source

    @Provides
    fun provideJsonRoomImportPlanner(): JsonRoomImportPlanner = JsonRoomImportPlanner()

    @Provides
    fun provideJsonRoomImportSink(database: TrainIqDatabase): JsonRoomImportSink = RoomJsonImportSink(database)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds abstract fun bindHomeRepository(repository: RoomHomeRepository): HomeRepository
    @Binds abstract fun bindWorkoutRepository(repository: RoomWorkoutRepository): WorkoutRepository
    @Binds abstract fun bindNutritionRepository(repository: RoomNutritionRepository): NutritionRepository
    @Binds abstract fun bindProgressRepository(repository: RoomProgressRepository): ProgressRepository
    @Binds abstract fun bindCoachRepository(repository: RoomCoachRepository): CoachRepository
}
