package com.mycompany.testes;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;
import java.time.LocalDate;

/** Janela de formulário: cadastro de cliente (grava no banco). */
public class FormCliente extends JDialog {
    private final JTextField txtNome = new JTextField();
    private final JTextField txtCpf = new JTextField();
    private final JTextField txtTelefone = new JTextField();
    private final JTextField txtEmail = new JTextField();
    private final ClienteDAO dao = new ClienteDAO();
    private final Runnable aoSalvar;

    public FormCliente(Frame dono, Runnable aoSalvar) {
        super(dono, "Cadastro de cliente", true);
        this.aoSalvar = aoSalvar;
        setLayout(new BorderLayout());
        add(Estilo.cabecalho("Novo cliente", "Preencha os dados para cadastrar o cliente."), BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Estilo.BG);
        form.setBorder(BorderFactory.createEmptyBorder(20, 26, 8, 26));
        int y = 0;
        y = Estilo.linha(form, y, "Nome completo *", txtNome);
        y = Estilo.linha(form, y, "CPF *", txtCpf);
        y = Estilo.linha(form, y, "Telefone *", txtTelefone);
        Estilo.linha(form, y, "E-mail", txtEmail);
        add(form, BorderLayout.CENTER);
        add(Estilo.rodape(this::dispose, this::salvar, getRootPane()), BorderLayout.SOUTH);

        pack();
        setResizable(false);
        setLocationRelativeTo(dono);
    }

    private void salvar() {
        String nome = txtNome.getText().trim();
        if (nome.isEmpty()) { Estilo.aviso(this, "Informe o nome do cliente."); txtNome.requestFocus(); return; }

        String cpf = txtCpf.getText().replaceAll("\\D", "");
        if (cpf.length() != 11) { Estilo.aviso(this, "CPF deve ter 11 dígitos."); txtCpf.requestFocus(); return; }
        cpf = cpf.substring(0, 3) + "." + cpf.substring(3, 6) + "." + cpf.substring(6, 9) + "-" + cpf.substring(9);

        String tel = txtTelefone.getText().replaceAll("\\D", "");
        if (tel.length() != 10 && tel.length() != 11) {
            Estilo.aviso(this, "Telefone deve ter DDD + 8 ou 9 dígitos.");
            txtTelefone.requestFocus(); return;
        }
        tel = tel.length() == 11
                ? "(" + tel.substring(0, 2) + ") " + tel.substring(2, 7) + "-" + tel.substring(7)
                : "(" + tel.substring(0, 2) + ") " + tel.substring(2, 6) + "-" + tel.substring(6);

        String email = txtEmail.getText().trim();
        if (!email.isEmpty() && !email.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) {
            Estilo.aviso(this, "E-mail inválido.");
            txtEmail.requestFocus(); return;
        }

        try {
            dao.inserir(new Cliente(0, nome, cpf, tel, email, LocalDate.now()));
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().contains("UNIQUE")) {
                Estilo.aviso(this, "Já existe um cliente cadastrado com este CPF.");
                txtCpf.requestFocus();
            } else {
                JOptionPane.showMessageDialog(this, "Erro ao gravar no banco:\n" + e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            }
            return;
        }
        aoSalvar.run();
        dispose();
    }
}
