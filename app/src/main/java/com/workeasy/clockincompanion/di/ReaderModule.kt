package com.workeasy.clockincompanion.di

import com.workeasy.clockincompanion.BuildConfig
import com.workeasy.clockincompanion.data.reader.SerialFingerprintReader
import com.workeasy.clockincompanion.data.reader.SimulatedFingerprintReader
import com.workeasy.clockincompanion.domain.reader.DebugFingerprintControls
import com.workeasy.clockincompanion.domain.reader.FingerprintReader
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ReaderModule {

    @Provides
    @Singleton
    fun provideFingerprintReader(
        simulated: SimulatedFingerprintReader,
        serial: SerialFingerprintReader,
    ): FingerprintReader =
        if (BuildConfig.USE_SIMULATED_READER) simulated else serial

    @Provides
    @Singleton
    fun provideDebugFingerprintControls(
        simulated: SimulatedFingerprintReader,
        serial: SerialFingerprintReader,
    ): DebugFingerprintControls =
        if (BuildConfig.USE_SIMULATED_READER) simulated else serial
}
