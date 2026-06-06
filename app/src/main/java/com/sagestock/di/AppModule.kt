package com.sagestock.di

import com.sagestock.data.MockStockRepository
import com.sagestock.domain.StockRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {
    @Binds
    @Singleton
    abstract fun bindStockRepository(impl: MockStockRepository): StockRepository
}
