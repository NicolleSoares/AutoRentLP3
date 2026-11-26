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
    private static final String COLUNAS = "id,nome,cpf,telefone,email,data_cadastro";

    private static Cliente mapear(ResultSet rs) throws SQLException {
        return new Cliente(rs.getInt("id"), rs.getString("nome"), rs.getString("cpf"),
                rs.getString("telefone"), rs.getString("email"), LocalDate.parse(rs.getString("data_cadastro")));
    }

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

    public void atualizar(Cliente cli) throws SQLException {
        String sql = "UPDATE cliente SET nome=?, cpf=?, telefone=?, email=? WHERE id=?";
        try (Connection c = Conexao.abrir(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, cli.nome());
            ps.setString(2, cli.cpf());
            ps.setString(3, cli.telefone());
            ps.setString(4, cli.email());
            ps.setInt(5, cli.id());
            ps.executeUpdate();
        }
    }

    public Cliente buscar(int id) throws SQLException {
        try (Connection c = Conexao.abrir(); PreparedStatement ps = c.prepareStatement("SELECT " + COLUNAS + " FROM cliente WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    public List<Cliente> listar() throws SQLException {
        List<Cliente> lista = new ArrayList<>();
        try (Connection c = Conexao.abrir(); Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT " + COLUNAS + " FROM cliente ORDER BY nome")) {
            while (rs.next()) lista.add(mapear(rs));
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
