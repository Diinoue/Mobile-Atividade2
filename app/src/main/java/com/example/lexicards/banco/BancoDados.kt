package br.ufpr.lexicards.banco

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import br.ufpr.lexicards.regras.RegrasNegocio
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BancoDados(context: Context) :
    SQLiteOpenHelper(context, NOME_BANCO, null, VERSAO_BANCO) {

    companion object {
        const val NOME_BANCO = "lexicards.db"
        const val VERSAO_BANCO = 1
        const val TABELA = "flashcards"

        const val COL_ID = "id"
        const val COL_PALAVRA = "palavra"
        const val COL_TRADUCAO = "traducao"
        const val COL_IDIOMA = "idioma"
        const val COL_FRASE = "frase_exemplo"
        const val COL_ACERTOS = "acertos"
        const val COL_ERROS = "erros"
        const val COL_STATUS = "status"
        const val COL_DATA = "data_criacao"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val criarTabelaFlashcards = "CREATE TABLE $TABELA (" +
            "$COL_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "$COL_PALAVRA TEXT NOT NULL, " +
            "$COL_TRADUCAO TEXT NOT NULL, " +
            "$COL_IDIOMA TEXT NOT NULL, " +
            "$COL_FRASE TEXT NOT NULL, " +
            "$COL_ACERTOS INTEGER NOT NULL DEFAULT 0, " +
            "$COL_ERROS INTEGER NOT NULL DEFAULT 0, " +
            "$COL_STATUS TEXT NOT NULL, " +
            "$COL_DATA TEXT NOT NULL)"
        db.execSQL(criarTabelaFlashcards)

        inserirCartoesIniciais(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, versaoAntiga: Int, versaoNova: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABELA")
        onCreate(db)
    }

    private fun inserirCartoesIniciais(db: SQLiteDatabase) {
        val hoje = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())

        val exemplos = listOf(
            arrayOf("coffee", "café", "Inglês",
                "I drink coffee every morning.", 4, 0),
            arrayOf("window", "janela", "Inglês",
                "I can see the window from here.", 1, 2),
            arrayOf("to improve", "melhorar", "Inglês",
                "I want to improve my pronunciation.", 0, 0),
            arrayOf("challenge", "desafio", "Inglês",
                "This challenge is really difficult.", 3, 1),
            arrayOf("ventana", "janela", "Espanhol",
                "Abrí la ventana por la mañana.", 0, 0),
            arrayOf("madrugar", "acordar cedo", "Espanhol",
                "Tengo que madrugar todos los días.", 2, 3),
            arrayOf("bonheur", "felicidade", "Francês",
                "Le bonheur est dans les petites choses.", 0, 0)
        )

        for (exemplo in exemplos) {
            val acertos = exemplo[4] as Int
            val erros = exemplo[5] as Int

            val valores = ContentValues().apply {
                put(COL_PALAVRA, exemplo[0] as String)
                put(COL_TRADUCAO, exemplo[1] as String)
                put(COL_IDIOMA, exemplo[2] as String)
                put(COL_FRASE, exemplo[3] as String)
                put(COL_ACERTOS, acertos)
                put(COL_ERROS, erros)
                put(COL_STATUS, RegrasNegocio.calcularStatus(acertos, erros))
                put(COL_DATA, hoje)
            }
            db.insert(TABELA, null, valores)
        }
    }
}
