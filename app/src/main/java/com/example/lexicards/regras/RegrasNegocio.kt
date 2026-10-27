package br.ufpr.lexicards.regras

import java.text.Normalizer

object RegrasNegocio {

    const val STATUS_NOVA = "Nova"
    const val STATUS_APRENDENDO = "Aprendendo"
    const val STATUS_DOMINADA = "Dominada"

    const val MIN_PALAVRAS_FRASE = 4
    const val MIN_ACERTOS_DOMINIO = 3
    const val TAXA_MINIMA_DOMINIO = 0.70

    fun calcularStatus(acertos: Int, erros: Int): String {
        val revisoes = acertos + erros
        if (revisoes == 0) return STATUS_NOVA

        val taxa = acertos.toDouble() / revisoes
        return if (acertos >= MIN_ACERTOS_DOMINIO && taxa >= TAXA_MINIMA_DOMINIO) {
            STATUS_DOMINADA
        } else {
            STATUS_APRENDENDO
        }
    }

}
