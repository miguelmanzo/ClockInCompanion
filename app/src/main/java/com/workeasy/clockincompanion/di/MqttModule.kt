package com.workeasy.clockincompanion.di

import com.workeasy.clockincompanion.data.mqtt.MqttClockEventPublisher
import com.workeasy.clockincompanion.domain.publisher.ClockEventPublisher
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class MqttModule {

    @Binds
    @Singleton
    abstract fun bindClockEventPublisher(
        impl: MqttClockEventPublisher,
    ): ClockEventPublisher
}
