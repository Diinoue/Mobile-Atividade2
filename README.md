# Integrantes
| Nome                       | GRR         |
|----------------------------|-------------|
| DIOGO HIROSHI INOUE        | GRR20230952 |
| JOÃO PEDRO QUEIROZ DEGER   | GRR20230941 |
| VITOR HIGEN MARÉS DE SOUZA | GRR20230937 |


# Lexicards — Flashcards de Vocabulário

Aplicativo Android de estudo de anocabulário de idiomas estrangeiros, com persistência local
em SQLite. Desenvolvido para o **App 02 — Aplicativo com Persistência Local (SQLite)**.

## Como funciona

Você cadastra cartões com uma palavra, a tradução, o idioma e uma frase de exemplo. No modo
de estudo o app mostra apenas a palavra — **um toque no cartão revela o significado** — e
você marca se acertou ou errou. O desempenho define o status de domínio de cada cartão, que
controla a ordem em que as palavras voltam a aparecer.

## Regra de negócio principal

Não é possível salvar um flashcard sem escrever uma **frase completa que realmente use a
palavra**: mínimo de 4 palavras, terminada em `.`, `!` ou `?`, e contendo a palavra
cadastrada (ignorando maiúsculas, acentos e aceitando flexões simples).

Além dela, o app aplica o **status de domínio automático** (Nova / Aprendendo / Dominada,
calculado a partir dos acertos e erros) e **não permite palavras duplicadas** no mesmo idioma.

## Executando

Abra a pasta do projeto no Android Studio, aguarde o Gradle sincronizar e execute em um
emulador ou dispositivo com API 29 ou superior.
