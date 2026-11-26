package com.mycompany.testes;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Aluguéis e suas regras de negócio:
 * 1) só veículo "Disponível" pode ser alugado; 2) o cliente só pode ter um aluguel "Em andamento";
 * 3) a devolução prevista não pode ser anterior à retirada; 4) cobra-se no mínimo 1 diária;
 * 5) atraso gera multa de 50% da diária por dia; 6) a devolução libera o veículo e lança a receita.
 */
public class AluguelDAO {
    public static final String EM_ANDAMENTO = "Em andamento";
    public static final String CONCLUIDO = "Concluído";
    public static final String CANCELADO = "Cancelado";
    public static final double ADICIONAL_ATRASO = 0.5;

    private static final String SELECT =
            "SELECT a.id, a.cliente_id, a.veiculo_id, c.nome AS cliente_nome, "
            + "v.modelo || ' (' || v.placa || ')' AS veiculo_desc, a.data_retirada, a.data_prevista, "
            + "a.data_devolucao, a.diaria, a.multa, a.total, a.status "
            + "FROM aluguel a JOIN cliente c ON c.id = a.cliente_id JOIN veiculo v ON v.id = a.veiculo_id ";

    /** Resultado do cálculo de uma devolução. */
    public record Fechamento(long diasCobrados, long diasAtraso, double multa, double total) { }

    /** Dias cobrados entre duas datas (mínimo de 1 diária). */
    public static long dias(LocalDate de, LocalDate ate) {
        return Math.max(1, ChronoUnit.DAYS.between(de, ate));
    }

    public static Fechamento calcular(Aluguel a, LocalDate devolucao) {
        long cobrados = dias(a.retirada(), devolucao);
        long atraso = Math.max(0, ChronoUnit.DAYS.between(a.prevista(), devolucao));
        double multa = atraso * a.diaria() * ADICIONAL_ATRASO;
        return new Fechamento(cobrados, atraso, multa, cobrados * a.diaria() + multa);
    }

    private static Aluguel mapear(ResultSet rs) throws SQLException {
        String dev = rs.getString("data_devolucao");
        return new Aluguel(rs.getInt("id"), rs.getInt("cliente_id"), rs.getInt("veiculo_id"),
                rs.getString("cliente_nome"), rs.getString("veiculo_desc"),
                LocalDate.parse(rs.getString("data_retirada")), LocalDate.parse(rs.getString("data_prevista")),
                dev == null ? null : LocalDate.parse(dev),
                rs.getDouble("diaria"), rs.getDouble("multa"), rs.getDouble("total"), rs.getString("status"));
    }

    public void abrir(int clienteId, int veiculoId, LocalDate prevista) throws SQLException, RegraException {
        LocalDate hoje = LocalDate.now();
        if (prevista.isBefore(hoje)) {
            throw new RegraException("A data de devolução não pode ser anterior à data de retirada.");
        }
        try (Connection c = Conexao.abrir()) {
            c.setAutoCommit(false);
            try {
                double diaria;
                try (PreparedStatement ps = c.prepareStatement("SELECT diaria, status FROM veiculo WHERE id = ?")) {
                    ps.setInt(1, veiculoId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw new RegraException("Veículo não encontrado.");
                        if (!"Disponível".equals(rs.getString("status"))) {
                            throw new RegraException("Este veículo não está disponível para locação.");
                        }
                        diaria = rs.getDouble("diaria");
                    }
                }
                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT COUNT(*) FROM aluguel WHERE cliente_id = ? AND status = ?")) {
                    ps.setInt(1, clienteId);
                    ps.setString(2, EM_ANDAMENTO);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next() && rs.getInt(1) > 0) {
                            throw new RegraException("Este cliente já possui um aluguel em andamento.");
                        }
                    }
                }
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO aluguel(cliente_id,veiculo_id,data_retirada,data_prevista,diaria,multa,total,status) "
                        + "VALUES(?,?,?,?,?,0,?,?)")) {
                    ps.setInt(1, clienteId);
                    ps.setInt(2, veiculoId);
                    ps.setString(3, hoje.toString());
                    ps.setString(4, prevista.toString());
                    ps.setDouble(5, diaria);
                    ps.setDouble(6, dias(hoje, prevista) * diaria);
                    ps.setString(7, EM_ANDAMENTO);
                    ps.executeUpdate();
                }
                definirStatusVeiculo(c, veiculoId, "Alugado");
                c.commit();
            } catch (SQLException | RegraException e) {
                c.rollback();
                throw e;
            }
        }
    }

    /** Registra a devolução na data informada: fecha o contrato, libera o veículo e lança a receita. */
    public void devolver(int id, LocalDate data) throws SQLException, RegraException {
        try (Connection c = Conexao.abrir()) {
            c.setAutoCommit(false);
            try {
                Aluguel a = buscar(c, id);
                if (a == null || !EM_ANDAMENTO.equals(a.status())) {
                    throw new RegraException("Só é possível devolver um aluguel em andamento.");
                }
                Fechamento f = calcular(a, data);
                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE aluguel SET data_devolucao=?, multa=?, total=?, status=? WHERE id=?")) {
                    ps.setString(1, data.toString());
                    ps.setDouble(2, f.multa());
                    ps.setDouble(3, f.total());
                    ps.setString(4, CONCLUIDO);
                    ps.setInt(5, id);
                    ps.executeUpdate();
                }
                definirStatusVeiculo(c, a.veiculoId(), "Disponível");
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO movimentacao(data,descricao,categoria,valor,tipo,aluguel_id) VALUES(?,?,?,?,?,?)")) {
                    ps.setString(1, data.toString());
                    ps.setString(2, "Aluguel " + Fmt.codigo(id) + " - " + a.veiculo());
                    ps.setString(3, "Locação");
                    ps.setDouble(4, f.total());
                    ps.setString(5, "Entrada");
                    ps.setInt(6, id);
                    ps.executeUpdate();
                }
                c.commit();
            } catch (SQLException | RegraException e) {
                c.rollback();
                throw e;
            }
        }
    }

    /** Cancela um aluguel em andamento (sem cobrança) e libera o veículo. */
    public void cancelar(int id) throws SQLException, RegraException {
        try (Connection c = Conexao.abrir()) {
            c.setAutoCommit(false);
            try {
                Aluguel a = buscar(c, id);
                if (a == null || !EM_ANDAMENTO.equals(a.status())) {
                    throw new RegraException("Só é possível cancelar um aluguel em andamento.");
                }
                try (PreparedStatement ps = c.prepareStatement("UPDATE aluguel SET status=? WHERE id=?")) {
                    ps.setString(1, CANCELADO);
                    ps.setInt(2, id);
                    ps.executeUpdate();
                }
                definirStatusVeiculo(c, a.veiculoId(), "Disponível");
                c.commit();
            } catch (SQLException | RegraException e) {
                c.rollback();
                throw e;
            }
        }
    }

    private static void definirStatusVeiculo(Connection c, int veiculoId, String status) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("UPDATE veiculo SET status=? WHERE id=?")) {
            ps.setString(1, status);
            ps.setInt(2, veiculoId);
            ps.executeUpdate();
        }
    }

    private static Aluguel buscar(Connection c, int id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(SELECT + "WHERE a.id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    public Aluguel buscar(int id) throws SQLException {
        try (Connection c = Conexao.abrir()) {
            return buscar(c, id);
        }
    }

    public List<Aluguel> listar() throws SQLException {
        List<Aluguel> lista = new ArrayList<>();
        try (Connection c = Conexao.abrir(); PreparedStatement ps = c.prepareStatement(SELECT + "ORDER BY a.id DESC");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(mapear(rs));
        }
        return lista;
    }

    /** Aluguéis com retirada no período; status nulo traz todos. */
    public List<Aluguel> listarPeriodo(LocalDate de, LocalDate ate, String status) throws SQLException {
        List<Aluguel> lista = new ArrayList<>();
        String sql = SELECT + "WHERE a.data_retirada BETWEEN ? AND ? AND (? IS NULL OR a.status = ?) ORDER BY a.data_retirada, a.id";
        try (Connection c = Conexao.abrir(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, de.toString());
            ps.setString(2, ate.toString());
            ps.setString(3, status);
            ps.setString(4, status);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        }
        return lista;
    }

    /** Aluguel em andamento com a devolução prevista mais próxima (ou nulo). */
    public Aluguel proximaDevolucao() throws SQLException {
        try (Connection c = Conexao.abrir();
             PreparedStatement ps = c.prepareStatement(SELECT + "WHERE a.status = ? ORDER BY a.data_prevista, a.id LIMIT 1")) {
            ps.setString(1, EM_ANDAMENTO);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }
}
