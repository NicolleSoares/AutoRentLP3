package com.mycompany.testes;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

/** Janela de formulário: abertura de um novo aluguel. */
public class FormAluguel extends JDialog {
    private final JComboBox<Cliente> cbCliente;
    private final JComboBox<Veiculo> cbVeiculo;
    private final JTextField txtRetirada = new JTextField(Fmt.data(LocalDate.now()));
    private final JTextField txtPrevista = new JTextField(Fmt.data(LocalDate.now().plusDays(1)));
    private final JLabel lblTotal = new JLabel(" ");
    private final AluguelDAO dao = new AluguelDAO();
    private final Runnable aoSalvar;

    /** Abre o formulário, avisando antes se faltar cliente ou veículo disponível. */
    public static void abrir(Frame dono, Runnable aoSalvar) {
        try {
            List<Cliente> clientes = new ClienteDAO().listar();
            List<Veiculo> veiculos = new VeiculoDAO().listarDisponiveis();
            if (clientes.isEmpty()) { Estilo.aviso(dono, "Cadastre ao menos um cliente antes de abrir um aluguel."); return; }
            if (veiculos.isEmpty()) { Estilo.aviso(dono, "Não há veículos disponíveis para locação no momento."); return; }
            new FormAluguel(dono, aoSalvar, clientes, veiculos).setVisible(true);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(dono, "Erro de banco de dados:\n" + e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private FormAluguel(Frame dono, Runnable aoSalvar, List<Cliente> clientes, List<Veiculo> veiculos) {
        super(dono, "Novo aluguel", true);
        this.aoSalvar = aoSalvar;
        cbCliente = new JComboBox<>(clientes.toArray(new Cliente[0]));
        cbVeiculo = new JComboBox<>(veiculos.toArray(new Veiculo[0]));
        ListCellRenderer<Object> renderer = new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> l, Object v, int i, boolean s, boolean f) {
                super.getListCellRendererComponent(l, v, i, s, f);
                if (v instanceof Cliente c) setText(c.nome() + " — " + c.cpf());
                else if (v instanceof Veiculo ve) setText(ve.modelo() + " (" + ve.placa() + ") — " + Fmt.MOEDA.format(ve.diaria()) + "/dia");
                return this;
            }
        };
        cbCliente.setRenderer(renderer);
        cbVeiculo.setRenderer(renderer);
        txtRetirada.setEditable(false);

        setLayout(new BorderLayout());
        add(Estilo.cabecalho("Novo aluguel", "A retirada é hoje; informe a devolução prevista."), BorderLayout.NORTH);
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Estilo.BG);
        form.setBorder(BorderFactory.createEmptyBorder(20, 26, 8, 26));
        int y = 0;
        y = Estilo.linha(form, y, "Cliente *", cbCliente);
        y = Estilo.linha(form, y, "Veículo *", cbVeiculo);
        y = Estilo.linha(form, y, "Retirada", txtRetirada);
        y = Estilo.linha(form, y, "Devolução prevista *", txtPrevista);
        Estilo.linha(form, y, "Valor previsto", lblTotal);
        lblTotal.setFont(new Font("Arial", Font.BOLD, 13));
        add(form, BorderLayout.CENTER);
        add(Estilo.rodape(this::dispose, this::salvar, getRootPane()), BorderLayout.SOUTH);

        DocumentListener dl = new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { atualizarTotal(); }
            public void removeUpdate(DocumentEvent e) { atualizarTotal(); }
            public void changedUpdate(DocumentEvent e) { atualizarTotal(); }
        };
        txtPrevista.getDocument().addDocumentListener(dl);
        cbVeiculo.addActionListener(e -> atualizarTotal());
        atualizarTotal();

        pack();
        setResizable(false);
        setLocationRelativeTo(dono);
    }

    private void atualizarTotal() {
        try {
            LocalDate prevista = Fmt.parseData(txtPrevista.getText());
            Veiculo v = (Veiculo) cbVeiculo.getSelectedItem();
            if (v == null || prevista.isBefore(LocalDate.now())) { lblTotal.setText("—"); return; }
            long dias = AluguelDAO.dias(LocalDate.now(), prevista);
            lblTotal.setText(Fmt.MOEDA.format(dias * v.diaria()) + " (" + dias + " dia" + (dias > 1 ? "s" : "") + ")");
        } catch (DateTimeParseException e) {
            lblTotal.setText("—");
        }
    }

    private void salvar() {
        LocalDate prevista;
        try {
            prevista = Fmt.parseData(txtPrevista.getText());
        } catch (DateTimeParseException e) {
            Estilo.aviso(this, "Data inválida. Use o formato dd/mm/aaaa.");
            txtPrevista.requestFocus();
            return;
        }
        Cliente cli = (Cliente) cbCliente.getSelectedItem();
        Veiculo vei = (Veiculo) cbVeiculo.getSelectedItem();
        try {
            dao.abrir(cli.id(), vei.id(), prevista);
        } catch (RegraException e) {
            Estilo.aviso(this, e.getMessage());
            return;
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Erro ao gravar no banco:\n" + e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }
        aoSalvar.run();
        dispose();
    }
}
