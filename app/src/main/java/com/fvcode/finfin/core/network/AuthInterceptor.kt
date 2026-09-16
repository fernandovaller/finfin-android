package com.fvcode.finfin.core.network

import com.fvcode.finfin.core.datastore.FinfinPreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Injeta `Authorization: Bearer <token>` como `api.ts:85-87`.
 * Usa runBlocking pontual porque OkHttp Interceptor e sincrono.
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val prefs: FinfinPreferences,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = runBlocking { prefs.token.first() }
        val req = if (token.isNullOrBlank()) {
            chain.request()
        } else {
            chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        }
        return chain.proceed(req)
    }
}
