package com.fvcode.finfin.navigation

/** Espelho das rotas de `frontend/src/main.tsx:39-63`. */
object Rotas {
    const val LOGIN = "login"
    const val RECUPERAR = "recuperar-senha"
    const val REDEFINIR = "redefinir-senha?token={token}"
    const val HOME = "home"
    const val LANCAMENTOS = "lancamentos"
    const val OFX = "lancamentos/ofx"
    const val RELATORIOS = "relatorios"
    const val CATEGORIAS = "categorias"
    const val CONTAS = "contas"
    const val FORMAS = "formas-pagamento"
    const val AUDITORIA = "auditoria"
    const val CONFIG = "configuracoes"
    const val PERFIL = "perfil"

    fun redefinir(token: String) = "redefinir-senha?token=$token"
}
