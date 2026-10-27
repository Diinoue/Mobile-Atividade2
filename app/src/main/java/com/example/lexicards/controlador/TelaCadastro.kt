package com.example.lexicards.controlador

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.lexicards.R
import com.example.lexicards.dao.FlashcardDAO
import com.example.lexicards.modelo.Flashcard
import com.example.lexicards.regras.RegrasNegocio

class TelaCadastro : AppCompatActivity() {

    private lateinit var flashcardDAO: FlashcardDAO

    private lateinit var etPalavra: EditText
    private lateinit var etTraducao: EditText
    private lateinit var etIdioma: EditText
    private lateinit var etFrase: EditText
    private lateinit var btnDelete: Button
    private lateinit var tvInfo: TextView

    private var flashcardID: Int = 0

    private var acertos: Int = 0
    private var erros: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.tela_cadastro)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        etPalavra = findViewById(R.id.etPalavra)
        etTraducao = findViewById(R.id.etTraducao)
        etIdioma = findViewById(R.id.etIdioma)
        etFrase = findViewById(R.id.etFrase)
        btnDelete = findViewById(R.id.btnDelete)
        tvInfo = findViewById(R.id.tvInfo)

        flashcardDAO = FlashcardDAO(this)

        flashcardID = intent.getIntExtra("flashcardID", 0)
        if (flashcardID > 0) {
            carregarFlashcard()
        }
    }

    private fun carregarFlashcard() {
        val flashcard = flashcardDAO.buscarPorId(flashcardID)
        if (flashcard == null) {
            Toast.makeText(this, R.string.flashcard_nao_encontrado, Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        etPalavra.setText(flashcard.palavra)
        etTraducao.setText(flashcard.traducao)
        etIdioma.setText(flashcard.idioma)
        etFrase.setText(flashcard.fraseExemplo)

        acertos = flashcard.acertos
        erros = flashcard.erros

        btnDelete.visibility = View.VISIBLE
        tvInfo.visibility = View.VISIBLE
        tvInfo.text = getString(
            R.string.info_cartao,
            flashcard.status,
            flashcard.acertos,
            flashcard.erros,
            flashcard.dataCriacao
        )
    }

    fun salvarFlashcard(view: View) {
        val palavra = etPalavra.text.toString().trim()
        val traducao = etTraducao.text.toString().trim()
        val idioma = etIdioma.text.toString().trim()
        val frase = etFrase.text.toString().trim()

        if (!validarCampos(palavra, traducao, idioma, frase)) {
            return
        }

        val resultado = RegrasNegocio.validarFraseExemplo(palavra, frase)
        if (!resultado.valido) {
            mostrarErro(etFrase, resultado.mensagem)
            return
        }

        if (flashcardDAO.existePalavra(palavra, idioma, flashcardID)) {
            mostrarErro(etPalavra, getString(R.string.erro_duplicado, palavra, idioma))
            return
        }

        if (flashcardID == 0) {
            val novoFlashcard = Flashcard(
                palavra = palavra,
                traducao = traducao,
                idioma = idioma,
                fraseExemplo = frase
            )
            flashcardDAO.adicionarFlashcard(novoFlashcard)
            Toast.makeText(this, R.string.flashcard_adicionado, Toast.LENGTH_SHORT).show()
        } else {
            // Atualização de um flashcard existente
            val flashcardAtualizado = Flashcard(
                id = flashcardID,
                palavra = palavra,
                traducao = traducao,
                idioma = idioma,
                fraseExemplo = frase,
                acertos = acertos,
                erros = erros
            )
            flashcardDAO.atualizarFlashcard(flashcardAtualizado)
            Toast.makeText(this, R.string.flashcard_atualizado, Toast.LENGTH_SHORT).show()
        }

        finish()
    }

    private fun validarCampos(
        palavra: String,
        traducao: String,
        idioma: String,
        frase: String
    ): Boolean {
        if (palavra.isEmpty()) {
            mostrarErro(etPalavra, getString(R.string.erro_palavra_vazia))
            return false
        }
        if (palavra.length > RegrasNegocio.MAX_PALAVRA) {
            mostrarErro(etPalavra, getString(R.string.erro_tamanho, RegrasNegocio.MAX_PALAVRA))
            return false
        }
        if (traducao.isEmpty()) {
            mostrarErro(etTraducao, getString(R.string.erro_traducao_vazia))
            return false
        }
        if (traducao.length > RegrasNegocio.MAX_TRADUCAO) {
            mostrarErro(etTraducao, getString(R.string.erro_tamanho, RegrasNegocio.MAX_TRADUCAO))
            return false
        }
        if (idioma.isEmpty()) {
            mostrarErro(etIdioma, getString(R.string.erro_idioma_vazio))
            return false
        }
        if (idioma.length > RegrasNegocio.MAX_IDIOMA) {
            mostrarErro(etIdioma, getString(R.string.erro_tamanho, RegrasNegocio.MAX_IDIOMA))
            return false
        }
        if (frase.length > RegrasNegocio.MAX_FRASE) {
            mostrarErro(etFrase, getString(R.string.erro_tamanho, RegrasNegocio.MAX_FRASE))
            return false
        }
        return true
    }

    private fun mostrarErro(campo: EditText, mensagem: String) {
        campo.error = mensagem
        campo.requestFocus()
        Toast.makeText(this, mensagem, Toast.LENGTH_LONG).show()
    }

    fun excluirFlashcard(view: View) {
        if (flashcardID == 0) return

        AlertDialog.Builder(this)
            .setTitle(R.string.confirmar_exclusao_titulo)
            .setMessage(getString(R.string.confirmar_exclusao, etPalavra.text.toString()))
            .setNegativeButton(R.string.cancelar, null)
            .setPositiveButton(R.string.excluir) { _, _ ->
                flashcardDAO.excluirFlashcard(flashcardID)
                Toast.makeText(this, R.string.flashcard_excluido, Toast.LENGTH_SHORT).show()
                finish()
            }
            .show()
    }
}
