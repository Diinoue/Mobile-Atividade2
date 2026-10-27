package com.example.lexicards.controlador

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.lexicards.R
import com.example.lexicards.dao.FlashcardDAO
import com.example.lexicards.modelo.Flashcard

class TelaEstudo : AppCompatActivity() {

    private lateinit var flashcardDAO: FlashcardDAO

    private lateinit var layoutCartao: LinearLayout
    private lateinit var tvProgresso: TextView
    private lateinit var tvPalavra: TextView
    private lateinit var tvIdioma: TextView
    private lateinit var tvToque: TextView
    private lateinit var tvTraducao: TextView
    private lateinit var tvFrase: TextView
    private lateinit var painelResposta: LinearLayout

    private var fila: List<Flashcard> = emptyList()
    private var posicao = 0
    private var revelado = false

    private var acertosDaSessao = 0
    private var errosDaSessao = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.tela_estudo)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        layoutCartao = findViewById(R.id.cardFlashcard)
        tvProgresso = findViewById(R.id.tvProgresso)
        tvPalavra = findViewById(R.id.tvPalavra)
        tvIdioma = findViewById(R.id.tvIdioma)
        tvToque = findViewById(R.id.tvToque)
        tvTraducao = findViewById(R.id.tvTraducao)
        tvFrase = findViewById(R.id.tvFrase)
        painelResposta = findViewById(R.id.painelResposta)

        flashcardDAO = FlashcardDAO(this)

        fila = flashcardDAO.listar(
            intent.getStringExtra("termo"),
            intent.getStringExtra("status"),
            intent.getStringExtra("idioma")
        )

        if (fila.isEmpty()) {
            Toast.makeText(this, R.string.nada_para_estudar, Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        layoutCartao.setOnClickListener { revelarSignificado() }

        mostrarCartaoAtual()
    }

    private fun mostrarCartaoAtual() {
        val flashcard = fila[posicao]
        revelado = false

        tvProgresso.text = getString(R.string.progresso, posicao + 1, fila.size)
        tvPalavra.text = flashcard.palavra
        tvIdioma.text = flashcard.idioma

        tvTraducao.text = flashcard.traducao
        tvFrase.text = flashcard.fraseExemplo

        tvTraducao.visibility = View.GONE
        tvFrase.visibility = View.GONE
        painelResposta.visibility = View.GONE
        tvToque.visibility = View.VISIBLE
    }

    private fun revelarSignificado() {
        if (revelado) return
        revelado = true

        tvToque.visibility = View.GONE
        tvTraducao.visibility = View.VISIBLE
        tvFrase.visibility = View.VISIBLE
        painelResposta.visibility = View.VISIBLE
    }

    fun acertei(view: View) {
        registrar(true)
    }

    fun errei(view: View) {
        registrar(false)
    }

    private fun registrar(acertou: Boolean) {
        flashcardDAO.registrarRevisao(fila[posicao].id, acertou)

        if (acertou) acertosDaSessao++ else errosDaSessao++

        posicao++
        if (posicao < fila.size) {
            mostrarCartaoAtual()
        } else {
            encerrarSessao()
        }
    }

    fun encerrar(view: View) {
        encerrarSessao()
    }

    private fun encerrarSessao() {
        val revisadas = acertosDaSessao + errosDaSessao
        val mensagem = if (revisadas == 0) {
            getString(R.string.sessao_sem_revisoes)
        } else {
            getString(R.string.sessao_encerrada, revisadas, acertosDaSessao, errosDaSessao)
        }
        Toast.makeText(this, mensagem, Toast.LENGTH_LONG).show()
        finish()
    }
}
