package com.mycompany.testes;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/** Janela genérica de relatório: filtros, tabela de resultado, resumo e exportação para CSV. */
public class JanelaRelatorio extends JDialog {

    public interface Gerador { RelatorioDAO.Resultado gerar() throws Exception; }

    private final DefaultTableModel modelo = new DefaultTableModel() {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JLabel lblResumo = new JLabel(" ");
    private final Gerador gerador;
    private RelatorioDAO.Resultado atual;

    private JanelaRelatorio(Frame dono, String titulo, String subtitulo, JComponent filtros, Gerador gerador) {
        super(dono, titulo, true);
        this.gerador = gerador;
        setLayout(new BorderLayout());
        add(Estilo.cabecalho(titulo, subtitulo), BorderLayout.NORTH);

        JPanel topo = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        topo.setOpaque(false);
        topo.add(filtros);
        BotaoArredondado btGerar = new BotaoArredondado("Gerar", Estilo.BLUE, new Color(48, 70, 101));
        btGerar.setPreferredSize(new Dimension(100, 34));
        btGerar.addActionListener(e -> gerar());
        topo.add(btGerar);

        JTable tabela = new JTable(modelo);
        tabela.setRowHeight(30);
        tabela.setFont(new Font("Arial", Font.PLAIN, 12));
        tabela.setGridColor(new Color(235, 238, 243));
        tabela.getTableHeader().setFont(new Font("Arial", Font.BOLD, 11));
        tabela.setAutoCreateRowSorter(true);

        JPanel corpo = new JPanel(new BorderLayout(0, 12));
        corpo.setBackground(Estilo.BG);
        corpo.setBorder(BorderFactory.createEmptyBorder(16, 20, 8, 20));
        corpo.add(topo, BorderLayout.NORTH);
        corpo.add(new JScrollPane(tabela), BorderLayout.CENTER);
        add(corpo, BorderLayout.CENTER);

        lblResumo.setFont(new Font("Arial", Font.BOLD, 12));
        lblResumo.setForeground(Estilo.TEXT);
        BotaoArredondado btCsv = new BotaoArredondado("Exportar CSV", Estilo.BLUE, new Color(48, 70, 101));
        BotaoArredondado btFechar = new BotaoArredondado("Fechar", new Color(120, 134, 152), new Color(100, 114, 132));
        btCsv.setPreferredSize(new Dimension(130, 36));
        btFechar.setPreferredSize(new Dimension(100, 36));
        btCsv.addActionListener(e -> exportar());
        btFechar.addActionListener(e -> dispose());
        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        botoes.setOpaque(false);
        botoes.add(btCsv);
        botoes.add(btFechar);
        JPanel rodape = new JPanel(new BorderLayout());
        rodape.setBackground(Estilo.BG);
        rodape.setBorder(BorderFactory.createEmptyBorder(6, 20, 14, 20));
        rodape.add(lblResumo, BorderLayout.WEST);
        rodape.add(botoes, BorderLayout.EAST);
        add(rodape, BorderLayout.SOUTH);

        getRootPane().registerKeyboardAction(e -> dispose(), KeyStroke.getKeyStroke("ESCAPE"), JComponent.WHEN_IN_FOCUSED_WINDOW);
        setSize(920, 580);
        setLocationRelativeTo(dono);
        gerar();
    }

    private void gerar() {
        try {
            atual = gerador.gerar();
            modelo.setDataVector(atual.linhas().toArray(new Object[0][]), atual.colunas());
            lblResumo.setText(atual.resumo());
        } catch (DateTimeParseException e) {
            Estilo.aviso(this, "Data inválida. Use o formato dd/mm/aaaa.");
        } catch (RegraException e) {
            Estilo.aviso(this, e.getMessage());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erro ao gerar o relatório:\n" + e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void exportar() {
        if (atual == null) return;
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("relatorio.csv"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try (Writer w = new OutputStreamWriter(new FileOutputStream(fc.getSelectedFile()), StandardCharsets.UTF_8)) {
            w.write('\uFEFF'); // BOM: o Excel reconhece os acentos
            w.write(String.join(";", atual.colunas()) + "\n");
            for (Object[] linha : atual.linhas()) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < linha.length; i++) {
                    if (i > 0) sb.append(';');
                    sb.append('"').append(String.valueOf(linha[i]).replace("\"", "\"\"")).append('"');
                }
                w.write(sb + "\n");
            }
            JOptionPane.showMessageDialog(this, "Relatório exportado com sucesso.", "Exportar", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Não foi possível salvar o arquivo:\n" + e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static JPanel filtro(Object... itens) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        p.setOpaque(false);
        for (Object o : itens) p.add(o instanceof String s ? new JLabel(s) : (Component) o);
        return p;
    }

    private static JTextField campoData(LocalDate d) {
        JTextField t = new JTextField(Fmt.data(d), 8);
        t.setToolTipText("dd/mm/aaaa");
        return t;
    }

    public static void veiculos(Frame dono) {
        JComboBox<String> cb = new JComboBox<>(new String[]{"Todas", "Disponível", "Alugado", "Manutenção"});
        new JanelaRelatorio(dono, "Relatório de veículos", "Frota filtrada por situação.", filtro("Situação:", cb),
                () -> new RelatorioDAO().veiculos("Todas".equals(cb.getSelectedItem()) ? null : (String) cb.getSelectedItem())).setVisible(true);
    }

    public static void alugueis(Frame dono) {
        JTextField de = campoData(LocalDate.now().withDayOfMonth(1));
        JTextField ate = campoData(LocalDate.now());
        JComboBox<String> cb = new JComboBox<>(new String[]{"Todos", AluguelDAO.EM_ANDAMENTO, AluguelDAO.CONCLUIDO, AluguelDAO.CANCELADO});
        new JanelaRelatorio(dono, "Relatório de aluguéis", "Contratos com retirada no período.",
                filtro("De:", de, "Até:", ate, "Situação:", cb), () -> {
                    LocalDate d1 = Fmt.parseData(de.getText()), d2 = Fmt.parseData(ate.getText());
                    if (d2.isBefore(d1)) throw new RegraException("A data final não pode ser anterior à inicial.");
                    return new RelatorioDAO().alugueis(d1, d2, "Todos".equals(cb.getSelectedItem()) ? null : (String) cb.getSelectedItem());
                }).setVisible(true);
    }

    public static void financeiro(Frame dono) {
        JTextField de = campoData(LocalDate.now().withDayOfMonth(1));
        JTextField ate = campoData(LocalDate.now());
        new JanelaRelatorio(dono, "Relatório financeiro", "Entradas, saídas e saldo no período.",
                filtro("De:", de, "Até:", ate), () -> {
                    LocalDate d1 = Fmt.parseData(de.getText()), d2 = Fmt.parseData(ate.getText());
                    if (d2.isBefore(d1)) throw new RegraException("A data final não pode ser anterior à inicial.");
                    return new RelatorioDAO().financeiro(d1, d2);
                }).setVisible(true);
    }
}
