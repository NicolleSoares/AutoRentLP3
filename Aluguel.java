package com.mycompany.testes;

import java.time.LocalDate;

/** Contrato de aluguel. A devolução é nula enquanto o aluguel está em andamento. */
public record Aluguel(int id, int clienteId, int veiculoId, String clienteNome, String veiculo,
                      LocalDate retirada, LocalDate prevista, LocalDate devolucao,
                      double diaria, double multa, double total, String status) { }
