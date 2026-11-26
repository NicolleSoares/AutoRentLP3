package com.mycompany.testes;

import java.time.LocalDate;

/** Modelo de um cliente da locadora. */
public record Cliente(int id, String nome, String cpf, String telefone, String email, LocalDate dataCadastro) { }
