package com.example.myplants.data.di

import com.example.myplants.data.repository.BleManagerRepositoryImpl
import com.example.myplants.domain.repository.BleManagerRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class BleBindModule {

    @Binds
    @Singleton
    abstract fun bindBleRepository(
        impl: BleManagerRepositoryImpl
    ): BleManagerRepository
}