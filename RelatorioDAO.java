package com.mycompany.testes;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Consultas dos relatórios: devolvem colunas, linhas e uma linha de resumo. */
public class RelatorioDAO {

    public record Resultado(String[] colunas, List<Object[]> linhas, String resumo) { }

    public Resultado veiculos(String situacao) throws SQLException {
        List<Object[]> linhas = new ArrayList<>();
        double soma = 0;
        for (Veiculo v : new VeiculoDAO().listar()) {
            if (situacao != null && !situacao.equals(v.status())) continue;
            linhas.add(new Object[]{v.modelo(), v.placa(), v.ano(), v.categoria(), Fmt.MOEDA.format(v.diaria()), v.status()});
            soma += v.diaria();
        }
        String resumo = linhas.size() + " veículo(s)" + (linhas.isEmpty() ? "" : " • diária média " + Fmt.MOEDA.format(soma / linhas.size()));
        return new Resultado(new String[]{"Modelo", "Placa", "Ano", "Categoria", "Diária", "Situação"}, linhas, resumo);
    }

    public Resultado alugueis(LocalDate de, LocalDate ate, String status) throws SQLException {
        List<Object[]> linhas = new ArrayList<>();
        double faturado = 0;
        for (Aluguel a : new AluguelDAO().listarPeriodo(de, ate, status)) {
            linhas.add(new Object[]{Fmt.codigo(a.id()), a.clienteNome(), a.veiculo(), Fmt.data(a.retirada()),
                Fmt.data(a.prevista()), Fmt.data(a.devolucao()), Fmt.MOEDA.format(a.total()), a.status()});
            if (AluguelDAO.CONCLUIDO.equals(a.status())) faturado += a.total();
        }
        String resumo = linhas.size() + " aluguel(is) • faturado nos concluídos: " + Fmt.MOEDA.format(faturado);
        return new Resultado(new String[]{"Contrato", "Cliente", "Veículo", "Retirada", "Prevista", "Devolução", "Total", "Situação"}, linhas, resumo);
    }

    public Resultado financeiro(LocalDate de, LocalDate ate) throws SQLException {
        List<Object[]> linhas = new ArrayList<>();
        double entradas = 0, saidas = 0;
        for (Movimentacao m : new MovimentacaoDAO().listarPeriodo(de, ate)) {
            linhas.add(new Object[]{Fmt.data(m.data()), m.descricao(), m.categoria(), m.tipo(), Fmt.MOEDA.format(m.valor())});
            if ("Entrada".equals(m.tipo())) entradas += m.valor(); else saidas += m.valor();
        }
        String resumo = "Entradas " + Fmt.MOEDA.format(entradas) + " • Saídas " + Fmt.MOEDA.format(saidas)
                + " • Saldo " + Fmt.MOEDA.format(entradas - saidas);
        return new Resultado(new String[]{"Data", "Descrição", "Categoria", "Tipo", "Valor"}, linhas, resumo);
    }
}
