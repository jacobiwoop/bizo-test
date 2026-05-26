package com.example.di

import android.content.Context
import com.example.data.InboxStateStore
import com.example.data.RealtimeManager
import com.example.data.SessionManager
import com.example.data.api.BizoService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideSessionManager(@ApplicationContext context: Context): SessionManager {
        return SessionManager(context)
    }

    @Provides
    @Singleton
    fun provideBizoService(sessionManager: SessionManager): BizoService {
        return BizoService.create(sessionManager)
    }

    @Provides
    @Singleton
    fun provideRealtimeManager(sessionManager: SessionManager): RealtimeManager {
        return RealtimeManager(sessionManager)
    }

    @Provides
    @Singleton
    fun provideInboxStateStore(bizoService: BizoService): InboxStateStore {
        return InboxStateStore(bizoService)
    }
}
