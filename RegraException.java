package com.mycompany.testes;

/** Violação de uma regra de negócio (ex.: alugar um veículo que não está disponível). */
public class RegraException extends Exception {
    public RegraException(String mensagem) { super(mensagem); }
}
