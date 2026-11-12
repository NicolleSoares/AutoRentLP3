package com.mycompany.testes;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;
import java.time.Year;

/** Janela de formulário: cadastro de veículo (grava no banco). */
public class FormVeiculo extends JDialog {
    private final JTextField txtModelo = new JTextField();
    private final JTextField txtPlaca = new JTextField();
    private final JTextField txtAno = new JTextField();
    private final JTextField txtDiaria = new JTextField();
    private final JComboBox<String> cbCategoria = new JComboBox<>(new String[]{"Hatch", "Sedan", "SUV", "Picape", "Utilitário"});
    private final JComboBox<String> cbStatus = new JComboBox<>(new String[]{"Disponível", "Alugado", "Manutenção"});
    private final VeiculoDAO dao = new VeiculoDAO();
    private final Runnable aoSalvar;

    public FormVeiculo(Frame dono, Runnable aoSalvar) {
        super(dono, "Cadastro de veículo", true);
        this.aoSalvar = aoSalvar;
        setLayout(new BorderLayout());
        add(Estilo.cabecalho("Novo veículo", "Preencha os dados para cadastrar na frota."), BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Estilo.BG);
        form.setBorder(BorderFactory.createEmptyBorder(20, 26, 8, 26));
        int y = 0;
        y = Estilo.linha(form, y, "Modelo *", txtModelo);
        y = Estilo.linha(form, y, "Placa *", txtPlaca);
        y = Estilo.linha(form, y, "Ano *", txtAno);
        y = Estilo.linha(form, y, "Categoria", cbCategoria);
        y = Estilo.linha(form, y, "Diária (R$) *", txtDiaria);
        Estilo.linha(form, y, "Situação", cbStatus);
        add(form, BorderLayout.CENTER);
        add(Estilo.rodape(this::dispose, this::salvar, getRootPane()), BorderLayout.SOUTH);

        pack();
        setResizable(false);
        setLocationRelativeTo(dono);
    }

    private void salvar() {
        String modelo = txtModelo.getText().trim();
        if (modelo.isEmpty()) { Estilo.aviso(this, "Informe o modelo do veículo."); txtModelo.requestFocus(); return; }

        String placa = txtPlaca.getText().trim().toUpperCase().replace("-", "");
        if (!placa.matches("[A-Z]{3}[0-9][A-Z0-9][0-9]{2}")) {
            Estilo.aviso(this, "Placa inválida. Use o formato ABC-1234 ou ABC-1D23.");
            txtPlaca.requestFocus(); return;
        }
        placa = placa.substring(0, 3) + "-" + placa.substring(3);

        int ano;
        try {
            ano = Integer.parseInt(txtAno.getText().trim());
            if (ano < 1980 || ano > Year.now().getValue() + 1) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            Estilo.aviso(this, "Informe um ano válido (entre 1980 e " + (Year.now().getValue() + 1) + ").");
            txtAno.requestFocus(); return;
        }

        double diaria;
        try {
            diaria = Double.parseDouble(txtDiaria.getText().replace("R$", "").trim().replace(".", "").replace(",", "."));
            if (diaria <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            Estilo.aviso(this, "Informe um valor de diária maior que zero (ex.: 150,00).");
            txtDiaria.requestFocus(); return;
        }

        try {
            dao.inserir(new Veiculo(0, modelo, placa, ano, (String) cbCategoria.getSelectedItem(), diaria, (String) cbStatus.getSelectedItem()));
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().contains("UNIQUE")) {
                Estilo.aviso(this, "Já existe um veículo cadastrado com esta placa.");
                txtPlaca.requestFocus();
            } else {
                JOptionPane.showMessageDialog(this, "Erro ao gravar no banco:\n" + e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            }
            return;
        }
        aoSalvar.run();
        dispose();
    }
}
