package com.mycompany.testes;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;

/** Acesso ao banco SQLite (arquivo autorent.db criado na pasta de execução). */
public class Conexao {
    // foreign_keys=on: o SQLite só respeita chaves estrangeiras quando isso é ativado em cada conexão
    private static final String URL = "jdbc:sqlite:autorent.db?foreign_keys=on";

    public static Connection abrir() throws SQLException {
        return DriverManager.getConnection(URL);
    }

    /** Cria as tabelas (se não existirem) e insere dados de exemplo nas tabelas vazias. */
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
            s.executeUpdate("CREATE TABLE IF NOT EXISTS aluguel ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "cliente_id INTEGER NOT NULL REFERENCES cliente(id),"
                    + "veiculo_id INTEGER NOT NULL REFERENCES veiculo(id),"
                    + "data_retirada TEXT NOT NULL,"
                    + "data_prevista TEXT NOT NULL,"
                    + "data_devolucao TEXT,"
                    + "diaria REAL NOT NULL,"
                    + "multa REAL NOT NULL DEFAULT 0,"
                    + "total REAL NOT NULL,"
                    + "status TEXT NOT NULL)");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS movimentacao ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "data TEXT NOT NULL,"
                    + "descricao TEXT NOT NULL,"
                    + "categoria TEXT NOT NULL,"
                    + "valor REAL NOT NULL,"
                    + "tipo TEXT NOT NULL,"
                    + "aluguel_id INTEGER REFERENCES aluguel(id))");
            popular(c, s);
        }
    }

    private static boolean vazia(Statement s, String tabela) throws SQLException {
        try (ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM " + tabela)) {
            return rs.next() && rs.getInt(1) == 0;
        }
    }

    private static void popular(Connection c, Statement s) throws SQLException {
        LocalDate hoje = LocalDate.now();

        if (vazia(s, "veiculo")) {
            Object[][] v = {
                {"Toyota Corolla", "RJK-2A31", 2023, "Sedan", 180.0, "Disponível"},
                {"Hyundai HB20", "LMP-8B42", 2022, "Hatch", 120.0, "Alugado"},
                {"Jeep Renegade", "QWE-4C15", 2024, "SUV", 220.0, "Alugado"},
                {"Chevrolet Onix", "KLM-3D45", 2023, "Hatch", 110.0, "Disponível"},
                {"Fiat Strada", "PIC-5E67", 2022, "Picape", 200.0, "Disponível"},
                {"Fiat Mobi", "FMB-7G89", 2021, "Hatch", 95.0, "Manutenção"}
            };
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO veiculo(modelo,placa,ano,categoria,diaria,status) VALUES(?,?,?,?,?,?)")) {
                for (Object[] e : v) {
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

        if (vazia(s, "cliente")) {
            String[][] cl = {
                {"Mariana Costa", "529.982.247-25", "(21) 98765-4321", "mariana.costa@email.com"},
                {"Lucas Almeida", "111.444.777-35", "(21) 97654-3210", "lucas.almeida@email.com"},
                {"Ana Ribeiro", "123.456.789-09", "(21) 3456-7890", "ana.ribeiro@email.com"},
                {"Pedro Santos", "935.411.347-80", "(21) 99876-5432", "pedro.santos@email.com"}
            };
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO cliente(nome,cpf,telefone,email,data_cadastro) VALUES(?,?,?,?,?)")) {
                for (String[] e : cl) {
                    ps.setString(1, e[0]);
                    ps.setString(2, e[1]);
                    ps.setString(3, e[2]);
                    ps.setString(4, e[3]);
                    ps.setString(5, hoje.minusDays(30).toString());
                    ps.executeUpdate();
                }
            }
        }

        if (vazia(s, "aluguel")) {
            // {cpf, placa, retirada, prevista, devolucao (ou null), diaria, multa, status}
            Object[][] a = {
                {"529.982.247-25", "LMP-8B42", hoje.minusDays(2), hoje, null, 120.0, 0.0, AluguelDAO.EM_ANDAMENTO},
                {"111.444.777-35", "RJK-2A31", hoje.minusDays(9), hoje.minusDays(5), hoje.minusDays(5), 180.0, 0.0, AluguelDAO.CONCLUIDO},
                {"123.456.789-09", "QWE-4C15", hoje.minusDays(1), hoje.plusDays(3), null, 220.0, 0.0, AluguelDAO.EM_ANDAMENTO}
            };
            String sql = "INSERT INTO aluguel(cliente_id,veiculo_id,data_retirada,data_prevista,data_devolucao,diaria,multa,total,status) "
                    + "SELECT c.id, v.id, ?, ?, ?, ?, ?, ?, ? FROM cliente c, veiculo v WHERE c.cpf = ? AND v.placa = ?";
            try (PreparedStatement ps = c.prepareStatement(sql);
                 PreparedStatement upd = c.prepareStatement("UPDATE veiculo SET status = 'Alugado' WHERE placa = ?")) {
                for (Object[] e : a) {
                    LocalDate ret = (LocalDate) e[2], prev = (LocalDate) e[3], dev = (LocalDate) e[4];
                    double diaria = (Double) e[5];
                    ps.setString(1, ret.toString());
                    ps.setString(2, prev.toString());
                    ps.setString(3, dev == null ? null : dev.toString());
                    ps.setDouble(4, diaria);
                    ps.setDouble(5, (Double) e[6]);
                    ps.setDouble(6, AluguelDAO.dias(ret, dev == null ? prev : dev) * diaria);
                    ps.setString(7, (String) e[7]);
                    ps.setString(8, (String) e[0]);
                    ps.setString(9, (String) e[1]);
                    int n = ps.executeUpdate();
                    if (n > 0 && AluguelDAO.EM_ANDAMENTO.equals(e[7])) {
                        upd.setString(1, (String) e[1]);
                        upd.executeUpdate();
                    }
                }
            }
        }

        if (vazia(s, "movimentacao")) {
            String d1 = hoje.withDayOfMonth(1).toString();
            Object[][] m = {
                {d1, "Aluguel Toyota Corolla", "Locação", 720.0, "Entrada"},
                {d1, "Aluguel Fiat Mobi (balcão)", "Locação", 570.0, "Entrada"},
                {d1, "Manutenção Fiat Mobi", "Manutenção", 480.0, "Saída"},
                {d1, "Seguro da frota (mensal)", "Seguro", 1250.0, "Saída"}
            };
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO movimentacao(data,descricao,categoria,valor,tipo) VALUES(?,?,?,?,?)")) {
                for (Object[] e : m) {
                    ps.setString(1, (String) e[0]);
                    ps.setString(2, (String) e[1]);
                    ps.setString(3, (String) e[2]);
                    ps.setDouble(4, (Double) e[3]);
                    ps.setString(5, (String) e[4]);
                    ps.executeUpdate();
                }
            }
        }
    }
}
