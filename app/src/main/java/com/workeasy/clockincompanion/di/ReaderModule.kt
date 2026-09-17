package com.workeasy.clockincompanion.di

import com.workeasy.clockincompanion.data.reader.SwitchableFingerprintReader
import com.workeasy.clockincompanion.domain.reader.DebugFingerprintControls
import com.workeasy.clockincompanion.domain.reader.FingerprintReader
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ReaderModule {

    @Binds
    @Singleton
    abstract fun bindFingerprintReader(impl: SwitchableFingerprintReader): FingerprintReader

    @Binds
    @Singleton
    abstract fun bindDebugFingerprintControls(
        impl: SwitchableFingerprintReader,
    ): DebugFingerprintControls
}
