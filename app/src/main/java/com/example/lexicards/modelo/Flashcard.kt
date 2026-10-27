package br.ufpr.lexicards.modelo

data class Flashcard(
    val id: Int = 0,
    val palavra: String,
    val traducao: String,
    val idioma: String,
    val fraseExemplo: String,
    val acertos: Int = 0,
    val erros: Int = 0,
    val status: String = "Nova",
    val dataCriacao: String = ""
) {

    val revisoes: Int
        get() = acertos + erros

    val aproveitamento: Int
        get() = if (revisoes == 0) 0 else (acertos * 100) / revisoes

    override fun toString(): String {
        val resumo = if (revisoes == 0) {
            "nunca revisada"
        } else {
            "$acertos acerto(s) / $erros erro(s) - $aproveitamento%"
        }
        return "$palavra  ->  $traducao\n$idioma  |  $status  |  $resumo"
    }
}
