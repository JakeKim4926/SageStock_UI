package com.sagestock.di

import com.sagestock.BuildConfig
import com.sagestock.data.remote.AuthApi
import com.sagestock.data.remote.AuthInterceptor
import com.sagestock.data.remote.SageStockApi
import com.sagestock.data.remote.TokenAuthenticator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

/** 인증 호출 전용 OkHttp/Retrofit(인터셉터·Authenticator 없음 → refresh 순환 방지). */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AuthHttpClient

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AuthRetrofit

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    // --- 인증 전용(plain) 클라이언트 ---

    @Provides
    @Singleton
    @AuthHttpClient
    fun provideAuthOkHttpClient(): OkHttpClient = baseClientBuilder().build()

    @Provides
    @Singleton
    @AuthRetrofit
    fun provideAuthRetrofit(@AuthHttpClient client: OkHttpClient, json: Json): Retrofit =
        retrofitBuilder(client, json).build()

    @Provides
    @Singleton
    fun provideAuthApi(@AuthRetrofit retrofit: Retrofit): AuthApi = retrofit.create(AuthApi::class.java)

    // --- 메인(인증 부착) 클라이언트 ---

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        authenticator: TokenAuthenticator,
    ): OkHttpClient = baseClientBuilder()
        .addInterceptor(authInterceptor)
        .authenticator(authenticator)
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient, json: Json): Retrofit =
        retrofitBuilder(client, json).build()

    @Provides
    @Singleton
    fun provideSageStockApi(retrofit: Retrofit): SageStockApi = retrofit.create(SageStockApi::class.java)

    private fun baseClientBuilder(): OkHttpClient.Builder = OkHttpClient.Builder()
        .connectTimeout(CONNECT_TIMEOUT_SEC, TimeUnit.SECONDS)
        .readTimeout(READ_TIMEOUT_SEC, TimeUnit.SECONDS)

    private fun retrofitBuilder(client: OkHttpClient, json: Json): Retrofit.Builder = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .client(client)
        .addConverterFactory(json.asConverterFactory(CONTENT_TYPE.toMediaType()))

    private const val CONNECT_TIMEOUT_SEC = 15L
    // render 무료 플랜은 유휴 시 콜드 스타트(~30~50초)로 첫 응답이 지연됨 → read 타임아웃을 넉넉히.
    private const val READ_TIMEOUT_SEC = 60L
    private const val CONTENT_TYPE = "application/json"
}
