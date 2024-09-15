package com.example.myplants.data.di

import android.app.Application
import androidx.room.Room
import com.example.myplants.data.data_source.PlantDatabase
import com.example.myplants.data.repository.PlantRepositoryImpl
import com.example.myplants.domain.repository.PlantRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun providePlantDatabase(app: Application): PlantDatabase {
        return Room.databaseBuilder(
            app,
            PlantDatabase::class.java,
            PlantDatabase.DATABASE_NAME
        ).build()
    }

    @Provides
    @Singleton
    fun provideNoteRepository(db: PlantDatabase): PlantRepository {
        return PlantRepositoryImpl(db.plantDao)
    }


}