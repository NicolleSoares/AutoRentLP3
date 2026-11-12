package com.mycompany.testes;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/** Acesso ao banco SQLite (arquivo autorent.db criado na pasta de execução). */
public class Conexao {
    private static final String URL = "jdbc:sqlite:autorent.db";

    public static Connection abrir() throws SQLException {
        return DriverManager.getConnection(URL);
    }

    /** Cria as tabelas (se não existirem) e insere alguns veículos de exemplo na primeira execução. */
    public static void inicializar() throws SQLException {
        try (Connection c = abrir(); Statement s = c.createStatement()) {
            s.executeUpdate("CREATE TABLE IF NOT EXISTS veiculo ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "modelo TEXT NOT NULL,"
                    + "placa TEXT NOT NULL UNIQUE,"
                    + "ano INTEGER NOT NULL,"
                    + "categoria TEXT NOT NULL,"
                    + "diaria REAL NOT NULL,"
                    + "status TEXT NOT NULL)");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS cliente ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "nome TEXT NOT NULL,"
                    + "cpf TEXT NOT NULL UNIQUE,"
                    + "telefone TEXT NOT NULL,"
                    + "email TEXT,"
                    + "data_cadastro TEXT NOT NULL)");

            try (ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM veiculo")) {
                if (rs.next() && rs.getInt(1) == 0) {
                    String sql = "INSERT INTO veiculo(modelo,placa,ano,categoria,diaria,status) VALUES(?,?,?,?,?,?)";
                    Object[][] exemplos = {
                        {"Toyota Corolla", "RJK-2A31", 2023, "Sedan", 180.0, "Disponível"},
                        {"Hyundai HB20", "LMP-8B42", 2022, "Hatch", 120.0, "Alugado"},
                        {"Jeep Renegade", "QWE-4C15", 2024, "SUV", 220.0, "Disponível"}
                    };
                    try (PreparedStatement ps = c.prepareStatement(sql)) {
                        for (Object[] e : exemplos) {
                            ps.setString(1, (String) e[0]);
                            ps.setString(2, (String) e[1]);
                            ps.setInt(3, (Integer) e[2]);
                            ps.setString(4, (String) e[3]);
                            ps.setDouble(5, (Double) e[4]);
                            ps.setString(6, (String) e[5]);
                            ps.executeUpdate();
                        }
                    }
                }
            }
        }
    }
}
