package com.sagestock.di

import android.content.Context
import androidx.room.Room
import com.sagestock.data.MockStockRepository
import com.sagestock.data.PaperRepositoryImpl
import com.sagestock.data.WatchlistRepositoryImpl
import com.sagestock.data.local.PaperTradeDao
import com.sagestock.data.local.SageStockDatabase
import com.sagestock.data.local.WatchlistDao
import com.sagestock.domain.PaperRepository
import com.sagestock.domain.StockRepository
import com.sagestock.domain.WatchlistRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {
    @Binds
    @Singleton
    abstract fun bindStockRepository(impl: MockStockRepository): StockRepository

    @Binds
    @Singleton
    abstract fun bindWatchlistRepository(impl: WatchlistRepositoryImpl): WatchlistRepository

    @Binds
    @Singleton
    abstract fun bindPaperRepository(impl: PaperRepositoryImpl): PaperRepository

    companion object {
        @Provides
        @IoDispatcher
        fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

        @Provides
        @Singleton
        fun provideDatabase(@ApplicationContext context: Context): SageStockDatabase =
            Room.databaseBuilder(context, SageStockDatabase::class.java, "sagestock.db")
                .fallbackToDestructiveMigration()
                .build()

        @Provides
        fun provideWatchlistDao(database: SageStockDatabase): WatchlistDao = database.watchlistDao()

        @Provides
        fun providePaperTradeDao(database: SageStockDatabase): PaperTradeDao = database.paperTradeDao()
    }
}
