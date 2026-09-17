package com.fvcode.finfin.di

import com.fvcode.finfin.BuildConfig
import com.fvcode.finfin.core.network.AuthInterceptor
import com.fvcode.finfin.core.network.DynamicHostInterceptor
import com.fvcode.finfin.data.remote.FinfinApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttp(
        dynamicHost: DynamicHostInterceptor,
        auth: AuthInterceptor,
    ): OkHttpClient {
        val log = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
        return OkHttpClient.Builder()
            // PRIMEIRO: reescreve scheme/host/porta p/ URL efetiva (runtime).
            .addInterceptor(dynamicHost)
            .addInterceptor(auth)
            .addInterceptor(log)
            .build()
    }

    /**
     * baseUrl aqui é só o default de compilação (fallback). O host efetivo
     * de cada request vem do [DynamicHostInterceptor] (DataStore ou default).
     */
    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides
    @Singleton
    fun provideApi(retrofit: Retrofit): FinfinApi = retrofit.create(FinfinApi::class.java)
}
