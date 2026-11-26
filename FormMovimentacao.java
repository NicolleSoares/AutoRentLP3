package com.mycompany.testes;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/** Janela de formulário: lançamento financeiro avulso (as receitas de aluguel são lançadas na devolução). */
public class FormMovimentacao extends JDialog {
    private final JTextField txtData = new JTextField(Fmt.data(LocalDate.now()));
    private final JTextField txtDescricao = new JTextField();
    private final JComboBox<String> cbCategoria = new JComboBox<>(new String[]{"Locação", "Manutenção", "Combustível", "Seguro", "Impostos", "Outros"});
    private final JComboBox<String> cbTipo = new JComboBox<>(new String[]{"Saída", "Entrada"});
    private final JTextField txtValor = new JTextField();
    private final MovimentacaoDAO dao = new MovimentacaoDAO();
    private final Runnable aoSalvar;

    public FormMovimentacao(Frame dono, Runnable aoSalvar) {
        super(dono, "Nova movimentação", true);
        this.aoSalvar = aoSalvar;
        setLayout(new BorderLayout());
        add(Estilo.cabecalho("Nova movimentação", "Registre uma entrada ou saída de caixa."), BorderLayout.NORTH);
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Estilo.BG);
        form.setBorder(BorderFactory.createEmptyBorder(20, 26, 8, 26));
        int y = 0;
        y = Estilo.linha(form, y, "Data *", txtData);
        y = Estilo.linha(form, y, "Descrição *", txtDescricao);
        y = Estilo.linha(form, y, "Categoria", cbCategoria);
        y = Estilo.linha(form, y, "Tipo", cbTipo);
        Estilo.linha(form, y, "Valor (R$) *", txtValor);
        add(form, BorderLayout.CENTER);
        add(Estilo.rodape(this::dispose, this::salvar, getRootPane()), BorderLayout.SOUTH);
        pack();
        setResizable(false);
        setLocationRelativeTo(dono);
    }

    private void salvar() {
        LocalDate data;
        try {
            data = Fmt.parseData(txtData.getText());
        } catch (DateTimeParseException e) {
            Estilo.aviso(this, "Data inválida. Use o formato dd/mm/aaaa.");
            txtData.requestFocus();
            return;
        }
        String desc = txtDescricao.getText().trim();
        if (desc.isEmpty()) { Estilo.aviso(this, "Informe a descrição."); txtDescricao.requestFocus(); return; }
        double valor;
        try {
            valor = Double.parseDouble(txtValor.getText().replace("R$", "").trim().replace(".", "").replace(",", "."));
            if (valor <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            Estilo.aviso(this, "Informe um valor maior que zero (ex.: 250,00).");
            txtValor.requestFocus();
            return;
        }
        try {
            dao.inserir(new Movimentacao(0, data, desc, (String) cbCategoria.getSelectedItem(), valor, (String) cbTipo.getSelectedItem(), null));
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Erro ao gravar no banco:\n" + e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }
        aoSalvar.run();
        dispose();
    }
}
