package com.mycompany.testes;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Gravação e leitura de clientes no banco. */
public class ClienteDAO {

    public void inserir(Cliente cli) throws SQLException {
        String sql = "INSERT INTO cliente(nome,cpf,telefone,email,data_cadastro) VALUES(?,?,?,?,?)";
        try (Connection c = Conexao.abrir(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, cli.nome());
            ps.setString(2, cli.cpf());
            ps.setString(3, cli.telefone());
            ps.setString(4, cli.email());
            ps.setString(5, cli.dataCadastro().toString());
            ps.executeUpdate();
        }
    }

    public List<Cliente> listar() throws SQLException {
        List<Cliente> lista = new ArrayList<>();
        try (Connection c = Conexao.abrir(); Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT id,nome,cpf,telefone,email,data_cadastro FROM cliente ORDER BY nome")) {
            while (rs.next()) {
                lista.add(new Cliente(rs.getInt("id"), rs.getString("nome"), rs.getString("cpf"),
                        rs.getString("telefone"), rs.getString("email"), LocalDate.parse(rs.getString("data_cadastro"))));
            }
        }
        return lista;
    }

    public void excluir(int id) throws SQLException {
        try (Connection c = Conexao.abrir(); PreparedStatement ps = c.prepareStatement("DELETE FROM cliente WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }
}
