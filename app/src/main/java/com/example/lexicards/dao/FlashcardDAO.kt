package com.example.lexicards.dao

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import com.example.lexicards.banco.BancoDados
import com.example.lexicards.modelo.Flashcard
import com.example.lexicards.regras.RegrasNegocio
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Centraliza todas as operações SQL sobre a tabela de flashcards.
 */
class FlashcardDAO(private val context: Context) {

    private val banco = BancoDados(context)

    /**
     * Ordenação PERSONALIZADA (Requisito 5).
     *
     * Os cartões são exibidos na ordem em que precisam ser estudados:
     * primeiro as palavras novas, depois as que estão em aprendizado e por
     * último as já dominadas. Dentro de cada grupo, ordem alfabética da
     * palavra, ignorando maiúsculas/minúsculas.
     */
    private val ORDEM_PADRAO =
        "CASE " + BancoDados.COL_STATUS +
            " WHEN '" + RegrasNegocio.STATUS_NOVA + "' THEN 0" +
            " WHEN '" + RegrasNegocio.STATUS_APRENDENDO + "' THEN 1" +
            " ELSE 2 END ASC, " +
            BancoDados.COL_PALAVRA + " COLLATE NOCASE ASC"

    // ------------------------------------------------------------------
    // CREATE
    // ------------------------------------------------------------------

    fun adicionarFlashcard(flashcard: Flashcard): Long {
        val db = banco.writableDatabase
        val valores = ContentValues().apply {
            put(BancoDados.COL_PALAVRA, flashcard.palavra)
            put(BancoDados.COL_TRADUCAO, flashcard.traducao)
            put(BancoDados.COL_IDIOMA, flashcard.idioma)
            put(BancoDados.COL_FRASE, flashcard.fraseExemplo)
            put(BancoDados.COL_ACERTOS, flashcard.acertos)
            put(BancoDados.COL_ERROS, flashcard.erros)
            put(
                BancoDados.COL_STATUS,
                RegrasNegocio.calcularStatus(flashcard.acertos, flashcard.erros)
            )
            put(BancoDados.COL_DATA, dataDeHoje())
        }
        val id = db.insert(BancoDados.TABELA, null, valores)
        db.close()
        return id
    }

    // ------------------------------------------------------------------
    // UPDATE
    // ------------------------------------------------------------------

    fun atualizarFlashcard(flashcard: Flashcard): Int {
        val db = banco.writableDatabase
        val valores = ContentValues().apply {
            put(BancoDados.COL_PALAVRA, flashcard.palavra)
            put(BancoDados.COL_TRADUCAO, flashcard.traducao)
            put(BancoDados.COL_IDIOMA, flashcard.idioma)
            put(BancoDados.COL_FRASE, flashcard.fraseExemplo)
            put(
                BancoDados.COL_STATUS,
                RegrasNegocio.calcularStatus(flashcard.acertos, flashcard.erros)
            )
        }
        val rowsAffected = db.update(
            BancoDados.TABELA, valores,
            BancoDados.COL_ID + "=?", arrayOf(flashcard.id.toString())
        )
        db.close()
        return rowsAffected
    }

    /**
     * Registra o resultado de uma revisão feita na tela de estudo e reaplica a
     * regra de negócio do status de domínio.
     */
    fun registrarRevisao(id: Int, acertou: Boolean): Int {
        val flashcard = buscarPorId(id) ?: return 0

        val acertos = if (acertou) flashcard.acertos + 1 else flashcard.acertos
        val erros = if (acertou) flashcard.erros else flashcard.erros + 1

        val db = banco.writableDatabase
        val valores = ContentValues().apply {
            put(BancoDados.COL_ACERTOS, acertos)
            put(BancoDados.COL_ERROS, erros)
            put(BancoDados.COL_STATUS, RegrasNegocio.calcularStatus(acertos, erros))
        }
        val rowsAffected = db.update(
            BancoDados.TABELA, valores,
            BancoDados.COL_ID + "=?", arrayOf(id.toString())
        )
        db.close()
        return rowsAffected
    }

    // ------------------------------------------------------------------
    // DELETE
    // ------------------------------------------------------------------

    fun excluirFlashcard(id: Int): Int {
        val db = banco.writableDatabase
        val rowsDeleted = db.delete(
            BancoDados.TABELA,
            BancoDados.COL_ID + "=?", arrayOf(id.toString())
        )
        db.close()
        return rowsDeleted
    }

    // ------------------------------------------------------------------
    // READ - os três filtros exigidos no Requisito 5
    // ------------------------------------------------------------------

    /** FILTRO 1 - lista todos os flashcards cadastrados. */
    fun listarTodos(): List<Flashcard> = listar(null, null, null)

    /** FILTRO 2 - busca por um item específico (palavra ou tradução). */
    fun buscarPorPalavra(termo: String): List<Flashcard> = listar(termo, null, null)

    /** FILTRO 3 - filtra pelo status de domínio (Nova / Aprendendo / Dominada). */
    fun filtrarPorStatus(status: String): List<Flashcard> = listar(null, status, null)

    /** FILTRO EXTRA - filtra pelo idioma do cartão. */
    fun filtrarPorIdioma(idioma: String): List<Flashcard> = listar(null, null, idioma)

    /**
     * Monta a consulta combinando os filtros ativos. Qualquer parâmetro nulo ou
     * em branco é simplesmente ignorado na cláusula WHERE.
     */
    fun listar(termo: String?, status: String?, idioma: String?): List<Flashcard> {
        val flashcards = mutableListOf<Flashcard>()

        val condicoes = mutableListOf<String>()
        val argumentos = mutableListOf<String>()

        if (!termo.isNullOrBlank()) {
            condicoes.add(
                "(" + BancoDados.COL_PALAVRA + " LIKE ? OR " +
                    BancoDados.COL_TRADUCAO + " LIKE ?)"
            )
            argumentos.add("%" + termo.trim() + "%")
            argumentos.add("%" + termo.trim() + "%")
        }
        if (!status.isNullOrBlank()) {
            condicoes.add(BancoDados.COL_STATUS + "=?")
            argumentos.add(status)
        }
        if (!idioma.isNullOrBlank()) {
            condicoes.add(BancoDados.COL_IDIOMA + "=?")
            argumentos.add(idioma)
        }

        val selection = if (condicoes.isEmpty()) null else condicoes.joinToString(" AND ")
        val selectionArgs = if (argumentos.isEmpty()) null else argumentos.toTypedArray()

        val db = banco.readableDatabase
        val cursor = db.query(
            BancoDados.TABELA, null,
            selection, selectionArgs,
            null, null, ORDEM_PADRAO
        )

        if (cursor.moveToFirst()) {
            do {
                flashcards.add(cursorParaFlashcard(cursor))
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return flashcards
    }

    /** Busca um único cartão pelo id, usado na tela de edição. */
    fun buscarPorId(id: Int): Flashcard? {
        val db = banco.readableDatabase
        val cursor = db.query(
            BancoDados.TABELA, null,
            BancoDados.COL_ID + "=?", arrayOf(id.toString()),
            null, null, null
        )

        var flashcard: Flashcard? = null
        if (cursor.moveToFirst()) {
            flashcard = cursorParaFlashcard(cursor)
        }
        cursor.close()
        db.close()
        return flashcard
    }

    /**
     * Verifica se a palavra já existe no mesmo idioma, evitando cartões
     * duplicados. O parâmetro ignorarId permite editar um cartão sem que ele
     * seja comparado consigo mesmo.
     */
    fun existePalavra(palavra: String, idioma: String, ignorarId: Int = 0): Boolean {
        val db = banco.readableDatabase
        val cursor = db.query(
            BancoDados.TABELA, arrayOf(BancoDados.COL_ID),
            BancoDados.COL_PALAVRA + "=? COLLATE NOCASE AND " +
                BancoDados.COL_IDIOMA + "=? COLLATE NOCASE AND " +
                BancoDados.COL_ID + "<>?",
            arrayOf(palavra.trim(), idioma.trim(), ignorarId.toString()),
            null, null, null
        )
        val existe = cursor.count > 0
        cursor.close()
        db.close()
        return existe
    }

    /** Lista os idiomas distintos já cadastrados, usada para montar o filtro. */
    fun listarIdiomas(): List<String> {
        val idiomas = mutableListOf<String>()
        val db = banco.readableDatabase
        val cursor = db.query(
            BancoDados.TABELA, arrayOf(BancoDados.COL_IDIOMA),
            null, null,
            BancoDados.COL_IDIOMA, null,
            BancoDados.COL_IDIOMA + " COLLATE NOCASE ASC"
        )
        if (cursor.moveToFirst()) {
            do {
                idiomas.add(cursor.getString(0))
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return idiomas
    }

    /** Conta quantos cartões existem em um determinado status. */
    fun contarPorStatus(status: String): Int {
        val db = banco.readableDatabase
        val cursor = db.rawQuery(
            "SELECT COUNT(*) FROM " + BancoDados.TABELA +
                " WHERE " + BancoDados.COL_STATUS + "=?",
            arrayOf(status)
        )
        var total = 0
        if (cursor.moveToFirst()) {
            total = cursor.getInt(0)
        }
        cursor.close()
        db.close()
        return total
    }

    // ------------------------------------------------------------------
    // Auxiliares
    // ------------------------------------------------------------------

    private fun cursorParaFlashcard(cursor: Cursor): Flashcard {
        return Flashcard(
            id = cursor.getInt(cursor.getColumnIndexOrThrow(BancoDados.COL_ID)),
            palavra = cursor.getString(cursor.getColumnIndexOrThrow(BancoDados.COL_PALAVRA)),
            traducao = cursor.getString(cursor.getColumnIndexOrThrow(BancoDados.COL_TRADUCAO)),
            idioma = cursor.getString(cursor.getColumnIndexOrThrow(BancoDados.COL_IDIOMA)),
            fraseExemplo = cursor.getString(cursor.getColumnIndexOrThrow(BancoDados.COL_FRASE)),
            acertos = cursor.getInt(cursor.getColumnIndexOrThrow(BancoDados.COL_ACERTOS)),
            erros = cursor.getInt(cursor.getColumnIndexOrThrow(BancoDados.COL_ERROS)),
            status = cursor.getString(cursor.getColumnIndexOrThrow(BancoDados.COL_STATUS)),
            dataCriacao = cursor.getString(cursor.getColumnIndexOrThrow(BancoDados.COL_DATA))
        )
    }

    private fun dataDeHoje(): String {
        return SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
    }
}
