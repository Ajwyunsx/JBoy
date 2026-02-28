package com.jboy.emulator.di

import com.jboy.emulator.core.EmulatorCore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CoreModule {
    @Provides
    @Singleton
    fun provideEmulatorCore(): EmulatorCore = EmulatorCore.getInstance()
}
