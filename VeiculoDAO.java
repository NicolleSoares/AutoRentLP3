package com.mycompany.testes;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** Gravação e leitura de veículos no banco. */
public class VeiculoDAO {
    private static final String COLUNAS = "id,modelo,placa,ano,categoria,diaria,status";

    private static Veiculo mapear(ResultSet rs) throws SQLException {
        return new Veiculo(rs.getInt("id"), rs.getString("modelo"), rs.getString("placa"),
                rs.getInt("ano"), rs.getString("categoria"), rs.getDouble("diaria"), rs.getString("status"));
    }

    public void inserir(Veiculo v) throws SQLException {
        String sql = "INSERT INTO veiculo(modelo,placa,ano,categoria,diaria,status) VALUES(?,?,?,?,?,?)";
        try (Connection c = Conexao.abrir(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, v.modelo());
            ps.setString(2, v.placa());
            ps.setInt(3, v.ano());
            ps.setString(4, v.categoria());
            ps.setDouble(5, v.diaria());
            ps.setString(6, v.status());
            ps.executeUpdate();
        }
    }

    public void atualizar(Veiculo v) throws SQLException {
        String sql = "UPDATE veiculo SET modelo=?, placa=?, ano=?, categoria=?, diaria=?, status=? WHERE id=?";
        try (Connection c = Conexao.abrir(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, v.modelo());
            ps.setString(2, v.placa());
            ps.setInt(3, v.ano());
            ps.setString(4, v.categoria());
            ps.setDouble(5, v.diaria());
            ps.setString(6, v.status());
            ps.setInt(7, v.id());
            ps.executeUpdate();
        }
    }

    public Veiculo buscar(int id) throws SQLException {
        try (Connection c = Conexao.abrir(); PreparedStatement ps = c.prepareStatement("SELECT " + COLUNAS + " FROM veiculo WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    public List<Veiculo> listar() throws SQLException {
        return consultar("SELECT " + COLUNAS + " FROM veiculo ORDER BY modelo");
    }

    public List<Veiculo> listarDisponiveis() throws SQLException {
        return consultar("SELECT " + COLUNAS + " FROM veiculo WHERE status = 'Disponível' ORDER BY modelo");
    }

    /** Conta veículos; com status nulo, conta todos. */
    public int contar(String status) throws SQLException {
        String sql = status == null ? "SELECT COUNT(*) FROM veiculo" : "SELECT COUNT(*) FROM veiculo WHERE status = ?";
        try (Connection c = Conexao.abrir(); PreparedStatement ps = c.prepareStatement(sql)) {
            if (status != null) ps.setString(1, status);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public void excluir(int id) throws SQLException {
        try (Connection c = Conexao.abrir(); PreparedStatement ps = c.prepareStatement("DELETE FROM veiculo WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private List<Veiculo> consultar(String sql) throws SQLException {
        List<Veiculo> lista = new ArrayList<>();
        try (Connection c = Conexao.abrir(); Statement s = c.createStatement(); ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) lista.add(mapear(rs));
        }
        return lista;
    }
}
