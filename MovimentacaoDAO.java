package com.mycompany.testes;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/** Lançamentos financeiros (entradas e saídas). */
public class MovimentacaoDAO {

    private static Movimentacao mapear(ResultSet rs) throws SQLException {
        int al = rs.getInt("aluguel_id");
        Integer aluguelId = rs.wasNull() ? null : al;
        return new Movimentacao(rs.getInt("id"), LocalDate.parse(rs.getString("data")), rs.getString("descricao"),
                rs.getString("categoria"), rs.getDouble("valor"), rs.getString("tipo"), aluguelId);
    }

    public void inserir(Movimentacao m) throws SQLException {
        String sql = "INSERT INTO movimentacao(data,descricao,categoria,valor,tipo) VALUES(?,?,?,?,?)";
        try (Connection c = Conexao.abrir(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, m.data().toString());
            ps.setString(2, m.descricao());
            ps.setString(3, m.categoria());
            ps.setDouble(4, m.valor());
            ps.setString(5, m.tipo());
            ps.executeUpdate();
        }
    }

    public List<Movimentacao> listar() throws SQLException {
        return listarPeriodo(LocalDate.of(1900, 1, 1), LocalDate.of(2999, 12, 31));
    }

    public List<Movimentacao> listarPeriodo(LocalDate de, LocalDate ate) throws SQLException {
        List<Movimentacao> lista = new ArrayList<>();
        String sql = "SELECT id,data,descricao,categoria,valor,tipo,aluguel_id FROM movimentacao "
                + "WHERE data BETWEEN ? AND ? ORDER BY data DESC, id DESC";
        try (Connection c = Conexao.abrir(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, de.toString());
            ps.setString(2, ate.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        }
        return lista;
    }

    /** Lançamentos gerados por um aluguel não podem ser apagados (fazem parte do contrato). */
    public void excluir(int id) throws SQLException, RegraException {
        try (Connection c = Conexao.abrir()) {
            try (PreparedStatement ps = c.prepareStatement("SELECT aluguel_id FROM movimentacao WHERE id = ?")) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        rs.getInt(1);
                        if (!rs.wasNull()) throw new RegraException("Este lançamento foi gerado por um aluguel e não pode ser excluído.");
                    }
                }
            }
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM movimentacao WHERE id = ?")) {
                ps.setInt(1, id);
                ps.executeUpdate();
            }
        }
    }

    /** Soma dos lançamentos de um tipo ("Entrada" ou "Saída") em um mês. */
    public double somaMes(String tipo, YearMonth mes) throws SQLException {
        String sql = "SELECT COALESCE(SUM(valor), 0) FROM movimentacao WHERE tipo = ? AND data LIKE ?";
        try (Connection c = Conexao.abrir(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, tipo);
            ps.setString(2, mes + "%");
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getDouble(1) : 0;
            }
        }
    }
}
