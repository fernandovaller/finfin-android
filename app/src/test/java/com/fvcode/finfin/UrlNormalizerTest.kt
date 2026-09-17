package com.fvcode.finfin

import com.fvcode.finfin.core.network.UrlNormalizer
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class UrlNormalizerTest {

    @Test
    fun normalizaIpComPorta() {
        assertEquals(
            "http://192.168.0.10:3001/api/",
            UrlNormalizer.normalizar("http://192.168.0.10:3001"),
        )
    }

    @Test
    fun adicionaSchemeQuandoAusente() {
        assertEquals(
            "http://192.168.0.10:3001/api/",
            UrlNormalizer.normalizar("192.168.0.10:3001"),
        )
    }

    @Test
    fun forcaSufixoApiComBarra() {
        assertEquals(
            "https://api.exemplo.com/api/",
            UrlNormalizer.normalizar("https://api.exemplo.com/api"),
        )
        assertEquals(
            "https://api.exemplo.com/api/",
            UrlNormalizer.normalizar("https://api.exemplo.com/qualquer/coisa"),
        )
    }

    @Test
    fun rejeitaLocalhost() {
        try {
            UrlNormalizer.normalizar("http://localhost:3001/api/")
            fail("deveria rejeitar localhost")
        } catch (e: IllegalArgumentException) {
            // esperado
        }
    }

    @Test
    fun emuladorPadraoOk() {
        assertEquals(
            "http://10.0.2.2:3001/api/",
            UrlNormalizer.normalizar("http://10.0.2.2:3001/api/"),
        )
    }
}
