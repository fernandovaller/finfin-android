package com.fvcode.finfin.core.network

import com.fvcode.finfin.BuildConfig
import com.fvcode.finfin.core.datastore.FinfinPreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Troca scheme/host/porta de cada request pela URL efetiva em runtime
 * (DataStore `finfin_base_url` ou `BuildConfig.BASE_URL` como fallback).
 *
 * Permite apontar o APK — inclusive o baixado do GitHub Release — para
 * qualquer backend sem novo build. O path (`/api/...`) é preservado,
 * então `FinfinApi` não muda.
 *
 * Deve ser o PRIMEIRO interceptor (antes do Auth).
 */
@Singleton
class DynamicHostInterceptor @Inject constructor(
    private val prefs: FinfinPreferences,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val efetiva = try {
            val salva = runBlocking { prefs.baseUrl.first() }
            UrlNormalizer.efetiva(salva, BuildConfig.BASE_URL)
        } catch (_: Exception) {
            BuildConfig.BASE_URL
        }
        val alvo = efetiva.toHttpUrlOrNull() ?: return chain.proceed(original)
        val novaUrl = original.url.newBuilder()
            .scheme(alvo.scheme)
            .host(alvo.host)
            .port(alvo.port)
            .build()
        return chain.proceed(original.newBuilder().url(novaUrl).build())
    }
}
