package com.example.lexicards.controlador

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ListView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.lexicards.R
import com.example.lexicards.dao.FlashcardDAO
import com.example.lexicards.modelo.Flashcard
import com.example.lexicards.regras.RegrasNegocio

class TelaPrincipal : AppCompatActivity() {

    private lateinit var flashcardDAO: FlashcardDAO
    private lateinit var lvFlashcards: ListView
    private lateinit var tvVazio: TextView
    private lateinit var tvResumo: TextView
    private lateinit var etBusca: EditText
    private lateinit var spStatus: Spinner
    private lateinit var spIdioma: Spinner

    private val opcoesStatus = listOf(
        "Todos os status",
        RegrasNegocio.STATUS_NOVA,
        RegrasNegocio.STATUS_APRENDENDO,
        RegrasNegocio.STATUS_DOMINADA
    )

    private var opcoesIdioma = listOf("Todos os idiomas")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.tela_principal)

        lvFlashcards = findViewById(R.id.lvFlashcards)
        tvVazio = findViewById(R.id.tvEmpty)
        tvResumo = findViewById(R.id.tvResumo)
        etBusca = findViewById(R.id.etBusca)
        spStatus = findViewById(R.id.spStatus)
        spIdioma = findViewById(R.id.spIdioma)

        flashcardDAO = FlashcardDAO(this)

        montarFiltroStatus()

        etBusca.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                aplicarFiltros()
                true
            } else {
                false
            }
        }

        lvFlashcards.setOnItemClickListener { parent, _, position, _ ->
            val flashcardSelecionado = parent.getItemAtPosition(position) as Flashcard
            val intent = Intent(this, TelaCadastro::class.java).apply {
                putExtra("flashcardID", flashcardSelecionado.id)
            }
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        montarFiltroIdioma()
        aplicarFiltros()
    }

    private fun montarFiltroStatus() {
        spStatus.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item, opcoesStatus
        )
        spStatus.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                aplicarFiltros()
            }

            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
    }

    private fun montarFiltroIdioma() {
        val idiomaSelecionado = spIdioma.selectedItem?.toString()

        // Desliga o listener para que a troca do adapter não dispare consultas.
        spIdioma.onItemSelectedListener = null

        opcoesIdioma = listOf("Todos os idiomas") + flashcardDAO.listarIdiomas()
        spIdioma.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item, opcoesIdioma
        )

        val posicao = opcoesIdioma.indexOf(idiomaSelecionado)
        if (posicao >= 0) {
            spIdioma.setSelection(posicao)
        }

        spIdioma.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                aplicarFiltros()
            }

            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
    }

    private fun aplicarFiltros() {
        val termo = etBusca.text.toString().trim()

        val status = if (spStatus.selectedItemPosition > 0) {
            spStatus.selectedItem.toString()
        } else {
            null
        }
        val idioma = if (spIdioma.selectedItemPosition > 0) {
            spIdioma.selectedItem.toString()
        } else {
            null
        }

        val flashcards = flashcardDAO.listar(termo, status, idioma)
        carregarLista(flashcards, termo.isNotEmpty() || status != null || idioma != null)
        atualizarResumo()
    }

    private fun carregarLista(flashcards: List<Flashcard>, comFiltro: Boolean) {
        if (flashcards.isEmpty()) {
            lvFlashcards.visibility = ListView.GONE
            tvVazio.visibility = TextView.VISIBLE
            tvVazio.text = if (comFiltro) {
                getString(R.string.lista_vazia_filtro)
            } else {
                getString(R.string.lista_vazia)
            }
        } else {
            lvFlashcards.visibility = ListView.VISIBLE
            tvVazio.visibility = TextView.GONE
            val adapter: ArrayAdapter<Flashcard> = ArrayAdapter(
                this, android.R.layout.simple_list_item_1, flashcards
            )
            lvFlashcards.adapter = adapter
        }
    }

    private fun atualizarResumo() {
        val novas = flashcardDAO.contarPorStatus(RegrasNegocio.STATUS_NOVA)
        val aprendendo = flashcardDAO.contarPorStatus(RegrasNegocio.STATUS_APRENDENDO)
        val dominadas = flashcardDAO.contarPorStatus(RegrasNegocio.STATUS_DOMINADA)
        val total = novas + aprendendo + dominadas

        tvResumo.text = getString(R.string.resumo, total, novas, aprendendo, dominadas)
    }
    fun buscar(view: View) {
        aplicarFiltros()
    }

    fun limparFiltros(view: View) {
        etBusca.setText("")
        spStatus.setSelection(0)
        spIdioma.setSelection(0)
        aplicarFiltros()
    }

    fun novoFlashcard(view: View) {
        startActivity(Intent(this, TelaCadastro::class.java))
    }

    fun estudar(view: View) {
        val termo = etBusca.text.toString().trim()
        val status = if (spStatus.selectedItemPosition > 0) spStatus.selectedItem.toString() else null
        val idioma = if (spIdioma.selectedItemPosition > 0) spIdioma.selectedItem.toString() else null

        if (flashcardDAO.listar(termo, status, idioma).isEmpty()) {
            Toast.makeText(this, R.string.nada_para_estudar, Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(this, TelaEstudo::class.java).apply {
            putExtra("termo", termo)
            putExtra("status", status)
            putExtra("idioma", idioma)
        }
        startActivity(intent)
    }
}
