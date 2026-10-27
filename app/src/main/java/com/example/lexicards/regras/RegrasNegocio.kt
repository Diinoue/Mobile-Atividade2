package com.example.lexicards.regras

import java.text.Normalizer

/**
 * Centraliza as REGRAS DE NEGÓCIO do aplicativo (Requisito 5).
 *
 * Regra 1 - Frase de exemplo obrigatória e válida:
 *           só é possível criar/editar um flashcard se o usuário escrever uma
 *           frase completa que realmente utilize a palavra cadastrada.
 *
 * Regra 2 - Status de domínio automático:
 *           cada cartão recebe o status "Nova", "Aprendendo" ou "Dominada"
 *           calculado a partir dos acertos e erros das revisões.
 *
 * Esta camada é chamada pelos Controllers ANTES de qualquer acesso ao DAO.
 */
object RegrasNegocio {

    // ---------- Status de domínio ----------
    const val STATUS_NOVA = "Nova"
    const val STATUS_APRENDENDO = "Aprendendo"
    const val STATUS_DOMINADA = "Dominada"

    // ---------- Parâmetros das regras ----------
    /** Número mínimo de palavras para a frase ser considerada "completa". */
    const val MIN_PALAVRAS_FRASE = 4

    /** Acertos mínimos para um cartão poder ser considerado dominado. */
    const val MIN_ACERTOS_DOMINIO = 3

    /** Aproveitamento mínimo (70%) para um cartão ser considerado dominado. */
    const val TAXA_MINIMA_DOMINIO = 0.70

    // ---------- Limites de tamanho (Requisito 7 - validação) ----------
    const val MAX_PALAVRA = 60
    const val MAX_TRADUCAO = 100
    const val MAX_IDIOMA = 30
    const val MAX_FRASE = 300

    /** Resultado de uma validação: válido ou inválido com a mensagem de erro. */
    data class Resultado(val valido: Boolean, val mensagem: String = "")

    private val VALIDO = Resultado(true)

    /**
     * REGRA DE NEGÓCIO 1.
     *
     * A frase de exemplo é obrigatória e precisa ser uma frase completa que
     * utilize a palavra do flashcard. A ideia é impedir o cadastro de palavras
     * "soltas": o usuário só registra o vocabulário depois de conseguir
     * empregar a palavra em um contexto real.
     *
     * São verificados quatro pontos:
     *  1. a frase não pode estar em branco;
     *  2. precisa ter no mínimo [MIN_PALAVRAS_FRASE] palavras;
     *  3. precisa terminar com pontuação final (. ! ou ?);
     *  4. precisa conter a palavra cadastrada.
     */
    fun validarFraseExemplo(palavra: String, frase: String): Resultado {
        val palavraLimpa = palavra.trim()
        val fraseLimpa = frase.trim()

        if (fraseLimpa.isEmpty()) {
            return Resultado(false, "Escreva uma frase de exemplo usando a palavra.")
        }

        val palavrasDaFrase = separarEmPalavras(fraseLimpa)
        if (palavrasDaFrase.size < MIN_PALAVRAS_FRASE) {
            return Resultado(
                false,
                "A frase de exemplo precisa ser uma frase completa " +
                    "(no mínimo $MIN_PALAVRAS_FRASE palavras)."
            )
        }

        if (!fraseLimpa.endsWith(".") && !fraseLimpa.endsWith("!") && !fraseLimpa.endsWith("?")) {
            return Resultado(false, "Termine a frase de exemplo com ponto final, ! ou ?")
        }

        if (!fraseContemPalavra(palavraLimpa, fraseLimpa)) {
            return Resultado(
                false,
                "A frase de exemplo precisa usar a palavra \"$palavraLimpa\"."
            )
        }

        return VALIDO
    }

    /**
     * REGRA DE NEGÓCIO 2.
     *
     * Calcula o status de domínio do cartão a partir do histórico de revisões:
     *  - "Nova"       -> ainda não foi revisada nenhuma vez;
     *  - "Dominada"   -> ao menos [MIN_ACERTOS_DOMINIO] acertos E
     *                    aproveitamento maior ou igual a 70%;
     *  - "Aprendendo" -> demais casos.
     */
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

    // ------------------------------------------------------------------
    // Funções auxiliares
    // ------------------------------------------------------------------

    /**
     * Deixa o texto em minúsculas e sem acentos, para que a comparação entre a
     * palavra e a frase funcione com "Café" / "cafe" / "CAFE".
     */
    fun normalizar(texto: String): String {
        val semAcento = Normalizer.normalize(texto, Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
        return semAcento.lowercase().trim()
    }

    /** Quebra a frase em palavras, descartando pontuação e espaços. */
    private fun separarEmPalavras(frase: String): List<String> {
        return frase.split(Regex("[^\\p{L}\\p{N}'-]+")).filter { it.isNotBlank() }
    }

    /**
     * Verifica se a frase realmente emprega a palavra cadastrada.
     *
     * - Expressões com mais de uma palavra (ex.: "get up") são procuradas
     *   como trecho dentro da frase.
     * - Palavras com 4 letras ou mais aceitam flexões simples, bastando que
     *   algum termo da frase comece com ela (ex.: "study" aceita "studying").
     */
    private fun fraseContemPalavra(palavra: String, frase: String): Boolean {
        val alvo = normalizar(palavra)
        if (alvo.isEmpty()) return false

        val fraseNormalizada = normalizar(frase)

        // Expressão composta: procura o trecho inteiro.
        if (alvo.contains(" ")) {
            return fraseNormalizada.contains(alvo)
        }

        val termos = separarEmPalavras(fraseNormalizada)
        return termos.any { termo ->
            termo == alvo || (alvo.length >= 4 && termo.startsWith(alvo))
        }
    }
}
