package com.sagestock.di

import android.content.Context
import androidx.room.Room
import com.sagestock.BuildConfig
import com.sagestock.data.MockStockRepository
import com.sagestock.data.PaperRepositoryImpl
import com.sagestock.data.RetrofitAuthRepository
import com.sagestock.data.RetrofitStockRepository
import com.sagestock.data.WatchlistRepositoryImpl
import com.sagestock.data.local.PaperTradeDao
import com.sagestock.data.local.SageStockDatabase
import com.sagestock.data.local.WatchlistDao
import com.sagestock.domain.AuthRepository
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
import javax.inject.Provider
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
    abstract fun bindWatchlistRepository(impl: WatchlistRepositoryImpl): WatchlistRepository

    @Binds
    @Singleton
    abstract fun bindPaperRepository(impl: PaperRepositoryImpl): PaperRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: RetrofitAuthRepository): AuthRepository

    companion object {
        // Phase 7: BuildConfig.USE_MOCK으로 Mock↔실서버 교체(UI 무수정). Provider로 미사용 구현은 생성 안 함.
        @Provides
        @Singleton
        fun provideStockRepository(
            mock: Provider<MockStockRepository>,
            retrofit: Provider<RetrofitStockRepository>,
        ): StockRepository = if (BuildConfig.USE_MOCK) mock.get() else retrofit.get()

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
