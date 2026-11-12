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

    public List<Veiculo> listar() throws SQLException {
        List<Veiculo> lista = new ArrayList<>();
        try (Connection c = Conexao.abrir(); Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT id,modelo,placa,ano,categoria,diaria,status FROM veiculo ORDER BY modelo")) {
            while (rs.next()) {
                lista.add(new Veiculo(rs.getInt("id"), rs.getString("modelo"), rs.getString("placa"),
                        rs.getInt("ano"), rs.getString("categoria"), rs.getDouble("diaria"), rs.getString("status")));
            }
        }
        return lista;
    }

    public void excluir(int id) throws SQLException {
        try (Connection c = Conexao.abrir(); PreparedStatement ps = c.prepareStatement("DELETE FROM veiculo WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }
}
