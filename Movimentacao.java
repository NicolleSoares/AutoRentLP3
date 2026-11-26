package com.mycompany.testes;

import java.time.LocalDate;

/** Lançamento financeiro (tipo "Entrada" ou "Saída"). aluguelId é nulo para lançamentos avulsos. */
public record Movimentacao(int id, LocalDate data, String descricao, String categoria,
                           double valor, String tipo, Integer aluguelId) { }
