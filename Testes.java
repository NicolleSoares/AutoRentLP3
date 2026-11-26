package com.mycompany.testes;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Locale;
import java.util.function.IntConsumer;

public class Testes {
    private static final VeiculoDAO veiculoDAO = new VeiculoDAO();
    private static final ClienteDAO clienteDAO = new ClienteDAO();
    private static final NumberFormat MOEDA = NumberFormat.getCurrencyInstance(Locale.of("pt", "BR"));
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DefaultTableModel modeloVeiculos = novoModelo("Código", "Modelo", "Placa", "Ano", "Categoria", "Diária", "Situação");
    private static final DefaultTableModel modeloClientes = novoModelo("Código", "Nome", "CPF", "Telefone", "E-mail", "Cadastro");
    private static final AluguelDAO aluguelDAO = new AluguelDAO();
    private static final MovimentacaoDAO movimentacaoDAO = new MovimentacaoDAO();
    private static final DefaultTableModel modeloAlugueis = novoModelo("Contrato", "Cliente", "Veículo", "Retirada", "Prevista", "Devolução", "Total", "Situação");
    private static final DefaultTableModel modeloMov = novoModelo("Código", "Data", "Descrição", "Categoria", "Tipo", "Valor");
    private static final Color CINZA = new Color(120, 134, 152), CINZA_H = new Color(100, 114, 132);
    private static final Color VERMELHO = new Color(190, 94, 82), VERMELHO_H = new Color(165, 75, 64);
    private static final Color VERDE = new Color(39, 150, 112), VERDE_H = new Color(30, 125, 93);
    private static JPanel cardTotal, cardDisp, cardAlug, cardFat, painelFrota, cardReceita, cardDespesa, cardSaldo;
    private static JLabel lblProxima;
    private static JFrame janelaPrincipal;

    /** Botão de ação de uma tela de consulta; executa sobre a linha selecionada (código da primeira coluna). */
    private record Acao(String texto, Color cor, Color hover, String confirmacao, IntConsumer executar) { }
    private static final Color NAVY = new Color(23, 36, 58);
    private static final Color BLUE = new Color(35, 52, 78);
    private static final Color BG = new Color(244, 247, 251);
    private static final Color TEXT = new Color(40, 52, 68);
    private static final Color MUTED = new Color(112, 125, 142);
    private static final CardLayout cards = new CardLayout();
    private static JPanel conteudo;

    public static void main(String[] args) {
        try {
            Conexao.inicializar();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Não foi possível iniciar o banco de dados:\n" + e.getMessage(),
                    "AUTO RENT", JOptionPane.ERROR_MESSAGE);
            return;
        }
        SwingUtilities.invokeLater(Testes::criarJanela);
    }

    private static void criarJanela() {
        JFrame janela = new JFrame("AUTO RENT | Gestão de Locadora");
        janelaPrincipal = janela;
        janela.setJMenuBar(criarBarraMenu());
        janela.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        janela.setSize(1180, 720);
        janela.setMinimumSize(new Dimension(1000, 620));
        janela.setLocationRelativeTo(null);
        janela.setLayout(new BorderLayout());

        JPanel menu = new JPanel();
        menu.setBackground(NAVY);
        menu.setPreferredSize(new Dimension(215, 700));
        menu.setBorder(BorderFactory.createEmptyBorder(18, 14, 18, 14));
        menu.setLayout(new BoxLayout(menu, BoxLayout.Y_AXIS));

        JLabel marca = new JLabel("AUTO RENT");
        marca.setForeground(Color.WHITE);
        marca.setFont(new Font("Arial", Font.BOLD, 23));
        marca.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel subtitulo = new JLabel("GESTÃO DE LOCADORA");
        subtitulo.setForeground(new Color(165, 180, 200));
        subtitulo.setFont(new Font("Arial", Font.PLAIN, 9));
        subtitulo.setAlignmentX(Component.LEFT_ALIGNMENT);
        menu.add(marca);
        menu.add(Box.createVerticalStrut(4));
        menu.add(subtitulo);
        menu.add(Box.createVerticalStrut(35));

        adicionarBotaoMenu(menu, "Início", "inicio");
        adicionarBotaoMenu(menu, "Veículos", "veiculos");
        adicionarBotaoMenu(menu, "Clientes", "clientes");
        adicionarBotaoMenu(menu, "Aluguéis", "alugueis");
        adicionarBotaoMenu(menu, "Financeiro", "financeiro");
        adicionarBotaoMenu(menu, "Relatórios", "relatorios");
        menu.add(Box.createVerticalGlue());
        JLabel versao = new JLabel("AUTO RENT • PP2");
        versao.setForeground(new Color(135, 151, 173));
        versao.setFont(new Font("Arial", Font.PLAIN, 10));
        versao.setAlignmentX(Component.LEFT_ALIGNMENT);
        menu.add(versao);

        conteudo = new JPanel(cards);
        conteudo.setBackground(BG);
        conteudo.add(telaInicio(), "inicio");
        conteudo.add(telaVeiculos(), "veiculos");
        conteudo.add(telaClientes(), "clientes");
        conteudo.add(telaAlugueis(), "alugueis");
        conteudo.add(telaFinanceiro(), "financeiro");
        conteudo.add(telaRelatorios(), "relatorios");

        recarregarTudo();

        janela.add(menu, BorderLayout.WEST);
        janela.add(conteudo, BorderLayout.CENTER);
        cards.show(conteudo, "inicio");
        janela.setVisible(true);
    }

    private static void adicionarBotaoMenu(JPanel menu, String texto, String tela) {
        BotaoArredondado botao = new BotaoArredondado(texto);
        botao.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        botao.setAlignmentX(Component.LEFT_ALIGNMENT);
        botao.addActionListener(e -> cards.show(conteudo, tela));
        menu.add(botao);
        menu.add(Box.createVerticalStrut(9));
    }

    private static JPanel baseTela(String titulo, String descricao) {
        JPanel painel = new JPanel(new BorderLayout(0, 20));
        painel.setBackground(BG);
        painel.setBorder(BorderFactory.createEmptyBorder(28, 32, 28, 32));
        JPanel topo = new JPanel();
        topo.setOpaque(false);
        topo.setLayout(new BoxLayout(topo, BoxLayout.Y_AXIS));
        JLabel nome = new JLabel(titulo);
        nome.setFont(new Font("Arial", Font.BOLD, 27));
        nome.setForeground(TEXT);
        JLabel desc = new JLabel(descricao);
        desc.setFont(new Font("Arial", Font.PLAIN, 13));
        desc.setForeground(MUTED);
        topo.add(nome);
        topo.add(Box.createVerticalStrut(6));
        topo.add(desc);
        painel.add(topo, BorderLayout.NORTH);
        return painel;
    }

    private static JPanel telaInicio() {
        JPanel painel = baseTela("Painel principal", "Acompanhe os indicadores e a situação da sua locadora.");
        JPanel centro = new JPanel();
        centro.setOpaque(false);
        centro.setLayout(new BoxLayout(centro, BoxLayout.Y_AXIS));

        JPanel indicadores = new JPanel(new GridLayout(1, 4, 14, 0));
        indicadores.setOpaque(false);
        cardTotal = cartaoIndicador("VEÍCULOS", "0", "Total cadastrado", new Color(65, 117, 190));
        cardDisp = cartaoIndicador("DISPONÍVEIS", "0", "Prontos para locação", new Color(39, 150, 112));
        cardAlug = cartaoIndicador("ALUGADOS", "0", "Em circulação", new Color(218, 151, 54));
        cardFat = cartaoIndicador("FATURAMENTO", "R$ 0,00", "Receitas deste mês", new Color(117, 94, 177));
        indicadores.add(cardTotal);
        indicadores.add(cardDisp);
        indicadores.add(cardAlug);
        indicadores.add(cardFat);
        centro.add(indicadores);
        centro.add(Box.createVerticalStrut(24));

        JLabel tituloFrota = new JLabel("Veículos disponíveis para locação");
        tituloFrota.setFont(new Font("Arial", Font.BOLD, 19));
        tituloFrota.setForeground(TEXT);
        tituloFrota.setAlignmentX(Component.LEFT_ALIGNMENT);
        centro.add(tituloFrota);
        centro.add(Box.createVerticalStrut(12));

        painelFrota = new JPanel(new GridLayout(1, 3, 14, 0));
        painelFrota.setOpaque(false);
        centro.add(painelFrota);
        centro.add(Box.createVerticalStrut(20));

        JPanel aviso = new JPanel(new BorderLayout());
        aviso.setBackground(Color.WHITE);
        aviso.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(226, 231, 238)), BorderFactory.createEmptyBorder(14, 17, 14, 17)));
        JLabel avisoTitulo = new JLabel("Próxima devolução");
        avisoTitulo.setFont(new Font("Arial", Font.BOLD, 14));
        avisoTitulo.setForeground(TEXT);
        lblProxima = new JLabel(" ");
        lblProxima.setForeground(MUTED);
        aviso.add(avisoTitulo, BorderLayout.WEST);
        aviso.add(lblProxima, BorderLayout.CENTER);
        centro.add(aviso);

        painel.add(centro, BorderLayout.CENTER);
        return painel;
    }

    private static void setValor(JPanel card, String texto) {
        ((JLabel) card.getClientProperty("valor")).setText(texto);
    }

    /** Recalcula indicadores, veículos em destaque e próxima devolução a partir do banco. */
    private static void atualizarPainel() {
        if (cardTotal == null) return;
        try {
            setValor(cardTotal, String.valueOf(veiculoDAO.contar(null)));
            setValor(cardDisp, String.valueOf(veiculoDAO.contar("Disponível")));
            setValor(cardAlug, String.valueOf(veiculoDAO.contar("Alugado")));
            setValor(cardFat, MOEDA.format(movimentacaoDAO.somaMes("Entrada", YearMonth.now())));

            painelFrota.removeAll();
            List<Veiculo> disp = veiculoDAO.listarDisponiveis();
            Color[] cores = {new Color(225, 235, 245), new Color(233, 237, 242), new Color(226, 235, 230)};
            for (int i = 0; i < 3; i++) {
                if (i < disp.size()) {
                    Veiculo v = disp.get(i);
                    painelFrota.add(cartaoCarro(v.categoria().toUpperCase(), v.modelo(), v.ano() + " • " + v.placa(),
                            MOEDA.format(v.diaria()) + " / diária", v.status(), cores[i]));
                } else {
                    JPanel vazio = new JPanel();
                    vazio.setOpaque(false);
                    painelFrota.add(vazio);
                }
            }
            painelFrota.revalidate();
            painelFrota.repaint();

            Aluguel p = aluguelDAO.proximaDevolucao();
            if (p == null) {
                lblProxima.setText("  Nenhum aluguel em andamento");
            } else {
                LocalDate hoje = LocalDate.now();
                String quando = p.prevista().isBefore(hoje) ? "ATRASADA desde " + Fmt.data(p.prevista())
                        : p.prevista().equals(hoje) ? "Hoje" : Fmt.data(p.prevista());
                lblProxima.setText("  " + p.veiculo() + " • Cliente: " + p.clienteNome() + " • " + quando);
            }
        } catch (SQLException e) {
            erroBD(e);
        }
    }

    private static JPanel cartaoIndicador(String titulo, String valor, String detalhe, Color destaque) {
        JPanel card = new JPanel();
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(228, 233, 240)), BorderFactory.createEmptyBorder(15, 15, 15, 15)));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        JLabel t = new JLabel(titulo);
        t.setFont(new Font("Arial", Font.BOLD, 10));
        t.setForeground(MUTED);
        JLabel v = new JLabel(valor);
        v.setFont(new Font("Arial", Font.BOLD, 25));
        v.setForeground(destaque);
        JLabel d = new JLabel(detalhe);
        d.setFont(new Font("Arial", Font.PLAIN, 11));
        d.setForeground(MUTED);
        card.add(t); card.add(Box.createVerticalStrut(12)); card.add(v); card.add(Box.createVerticalStrut(5)); card.add(d);
        card.putClientProperty("valor", v);
        return card;
    }

    private static JPanel cartaoCarro(String categoria, String modelo, String detalhes, String preco, String status, Color cor) {
        JPanel card = new JPanel(new BorderLayout(0, 9));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(226, 231, 238)), BorderFactory.createEmptyBorder(12, 12, 12, 12)));
        JPanel imagem = new JPanel(new GridBagLayout());
        imagem.setBackground(cor);
        imagem.setPreferredSize(new Dimension(180, 105));
        JLabel desenho = new JLabel("CARRO");
        desenho.setFont(new Font("Arial", Font.BOLD, 22));
        desenho.setForeground(new Color(93, 111, 130));
        imagem.add(desenho);
        JPanel info = new JPanel();
        info.setOpaque(false);
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        JLabel cat = new JLabel(categoria);
        cat.setFont(new Font("Arial", Font.BOLD, 9)); cat.setForeground(MUTED);
        JLabel mod = new JLabel(modelo);
        mod.setFont(new Font("Arial", Font.BOLD, 16)); mod.setForeground(TEXT);
        JLabel det = new JLabel(detalhes);
        det.setFont(new Font("Arial", Font.PLAIN, 11)); det.setForeground(MUTED);
        JLabel p = new JLabel(preco);
        p.setFont(new Font("Arial", Font.BOLD, 13)); p.setForeground(BLUE);
        JLabel st = new JLabel(status);
        st.setFont(new Font("Arial", Font.BOLD, 11)); st.setForeground(status.equals("Alugado") ? new Color(190, 126, 36) : new Color(39, 150, 112));
        info.add(cat); info.add(Box.createVerticalStrut(4)); info.add(mod); info.add(Box.createVerticalStrut(3)); info.add(det); info.add(Box.createVerticalStrut(9)); info.add(p); info.add(Box.createVerticalStrut(5)); info.add(st);
        card.add(imagem, BorderLayout.NORTH); card.add(info, BorderLayout.CENTER);
        return card;
    }

    private static JPanel telaVeiculos() {
        JPanel painel = baseTela("Veículos", "Consulte a frota e cadastre, edite ou exclua automóveis.");
        painel.add(painelCadastroBD(modeloVeiculos, "+ Novo veículo", Testes::abrirFormVeiculo,
                new Acao("Editar", CINZA, CINZA_H, null, Testes::editarVeiculo),
                new Acao("Excluir", VERMELHO, VERMELHO_H, "Deseja realmente excluir este veículo?", Testes::excluirVeiculo)), BorderLayout.CENTER);
        return painel;
    }

    private static JPanel telaClientes() {
        JPanel painel = baseTela("Clientes", "Cadastro e consulta dos clientes da locadora.");
        painel.add(painelCadastroBD(modeloClientes, "+ Novo cliente", Testes::abrirFormCliente,
                new Acao("Editar", CINZA, CINZA_H, null, Testes::editarCliente),
                new Acao("Excluir", VERMELHO, VERMELHO_H, "Deseja realmente excluir este cliente?", Testes::excluirCliente)), BorderLayout.CENTER);
        return painel;
    }

    private static JPanel telaAlugueis() {
        JPanel painel = baseTela("Aluguéis", "Abra contratos, registre devoluções e acompanhe os aluguéis.");
        painel.add(painelCadastroBD(modeloAlugueis, "+ Novo aluguel", Testes::abrirFormAluguel,
                new Acao("Devolução", VERDE, VERDE_H, null, Testes::registrarDevolucao),
                new Acao("Cancelar", CINZA, CINZA_H, "Cancelar este aluguel? O veículo será liberado e nada será cobrado.", Testes::cancelarAluguel)), BorderLayout.CENTER);
        return painel;
    }

    private static JPanel telaFinanceiro() {
        JPanel painel = baseTela("Financeiro", "Entradas e saídas de caixa. As receitas de aluguel entram na devolução.");
        JPanel centro = new JPanel(new BorderLayout(0, 18));
        centro.setOpaque(false);
        JPanel resumo = new JPanel(new GridLayout(1, 3, 14, 0));
        resumo.setOpaque(false);
        cardReceita = cartaoIndicador("RECEITA DO MÊS", "R$ 0,00", "Entradas registradas", new Color(39, 150, 112));
        cardDespesa = cartaoIndicador("DESPESAS DO MÊS", "R$ 0,00", "Saídas registradas", new Color(190, 94, 82));
        cardSaldo = cartaoIndicador("SALDO DO MÊS", "R$ 0,00", "Receitas menos despesas", new Color(65, 117, 190));
        resumo.add(cardReceita);
        resumo.add(cardDespesa);
        resumo.add(cardSaldo);
        centro.add(resumo, BorderLayout.NORTH);
        centro.add(painelCadastroBD(modeloMov, "+ Nova movimentação", Testes::abrirFormMov,
                new Acao("Excluir", VERMELHO, VERMELHO_H, "Deseja realmente excluir esta movimentação?", Testes::excluirMov)), BorderLayout.CENTER);
        painel.add(centro, BorderLayout.CENTER);
        return painel;
    }

    private static JPanel telaRelatorios() {
        JPanel painel = baseTela("Relatórios", "Consulte, filtre e exporte as informações da operação.");
        JPanel lista = new JPanel(new GridLayout(3, 1, 0, 13));
        lista.setOpaque(false);
        lista.add(cartaoRelatorio("Relatório de veículos", "Consulte veículos disponíveis, alugados e em manutenção.", () -> JanelaRelatorio.veiculos(janelaPrincipal)));
        lista.add(cartaoRelatorio("Relatório de aluguéis", "Visualize contratos por período e situação.", () -> JanelaRelatorio.alugueis(janelaPrincipal)));
        lista.add(cartaoRelatorio("Relatório financeiro", "Acompanhe receitas, despesas e saldo por período.", () -> JanelaRelatorio.financeiro(janelaPrincipal)));
        painel.add(lista, BorderLayout.CENTER);
        return painel;
    }

    private static JPanel cartaoRelatorio(String titulo, String descricao, Runnable abrir) {
        JPanel p = new JPanel(new BorderLayout(12, 0));
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(226, 231, 238)), BorderFactory.createEmptyBorder(17, 18, 17, 18)));
        JLabel icone = new JLabel("▤");
        icone.setFont(new Font("Arial", Font.BOLD, 25));
        icone.setForeground(new Color(65, 117, 190));
        JPanel textos = new JPanel();
        textos.setOpaque(false);
        textos.setLayout(new BoxLayout(textos, BoxLayout.Y_AXIS));
        JLabel t = new JLabel(titulo);
        t.setFont(new Font("Arial", Font.BOLD, 15));
        t.setForeground(TEXT);
        JLabel d = new JLabel(descricao);
        d.setFont(new Font("Arial", Font.PLAIN, 12));
        d.setForeground(MUTED);
        textos.add(t);
        textos.add(Box.createVerticalStrut(5));
        textos.add(d);
        JButton btn = new JButton("Visualizar");
        btn.setFocusPainted(false);
        btn.addActionListener(e -> abrir.run());
        p.add(icone, BorderLayout.WEST);
        p.add(textos, BorderLayout.CENTER);
        p.add(btn, BorderLayout.EAST);
        return p;
    }

    private static void configurarTabela(JTable tabela) {
        tabela.setRowHeight(38);
        tabela.setFont(new Font("Arial", Font.PLAIN, 12));
        tabela.setForeground(TEXT);
        tabela.setGridColor(new Color(235, 238, 243));
        tabela.setSelectionBackground(new Color(222, 233, 247));
        tabela.setSelectionForeground(TEXT);
        tabela.getTableHeader().setFont(new Font("Arial", Font.BOLD, 11));
        tabela.getTableHeader().setBackground(new Color(237, 241, 246));
        tabela.getTableHeader().setForeground(TEXT);
        tabela.getTableHeader().setPreferredSize(new Dimension(0, 38));
    }

    // ===== Barra de menus =====

    private static JMenuBar criarBarraMenu() {
        JMenuBar barra = new JMenuBar();

        JMenu arquivo = menu("Arquivo", KeyEvent.VK_A);
        arquivo.add(item("Sair", e -> System.exit(0)));

        JMenu cadastros = menu("Cadastros", KeyEvent.VK_C);
        cadastros.add(item("Novo veículo...", e -> abrirFormVeiculo()));
        cadastros.add(item("Novo cliente...", e -> abrirFormCliente()));
        cadastros.addSeparator();
        cadastros.add(item("Consultar veículos", e -> cards.show(conteudo, "veiculos")));
        cadastros.add(item("Consultar clientes", e -> cards.show(conteudo, "clientes")));

        JMenu locacoes = menu("Locações", KeyEvent.VK_L);
        locacoes.add(item("Novo aluguel...", e -> abrirFormAluguel()));
        locacoes.add(item("Consultar aluguéis e devoluções", e -> cards.show(conteudo, "alugueis")));

        JMenu financeiro = menu("Financeiro", KeyEvent.VK_F);
        financeiro.add(item("Nova movimentação...", e -> abrirFormMov()));
        financeiro.add(item("Consultar movimentações", e -> cards.show(conteudo, "financeiro")));

        JMenu relatorios = menu("Relatórios", KeyEvent.VK_R);
        relatorios.add(item("Veículos", e -> JanelaRelatorio.veiculos(janelaPrincipal)));
        relatorios.add(item("Aluguéis", e -> JanelaRelatorio.alugueis(janelaPrincipal)));
        relatorios.add(item("Financeiro", e -> JanelaRelatorio.financeiro(janelaPrincipal)));

        JMenu ajuda = menu("Ajuda", KeyEvent.VK_J);
        ajuda.add(item("Sobre", e -> JOptionPane.showMessageDialog(janelaPrincipal,
                "AUTO RENT | Gestão de Locadora\nLinguagem de Programação III - IC805\nTrabalho prático - PP2",
                "Sobre", JOptionPane.INFORMATION_MESSAGE)));

        barra.add(arquivo); barra.add(cadastros); barra.add(locacoes);
        barra.add(financeiro); barra.add(relatorios); barra.add(ajuda);
        return barra;
    }

    private static JMenu menu(String texto, int mnemonico) {
        JMenu m = new JMenu(texto);
        m.setMnemonic(mnemonico);
        return m;
    }

    private static JMenuItem item(String texto, java.awt.event.ActionListener acao) {
        JMenuItem i = new JMenuItem(texto);
        i.addActionListener(acao);
        return i;
    }

    // ===== Ligação das telas com o banco de dados =====

    private static DefaultTableModel novoModelo(String... colunas) {
        return new DefaultTableModel(colunas, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
    }

    private static void aviso(String msg) {
        Estilo.aviso(janelaPrincipal, msg);
    }

    private static void erroBD(SQLException e) {
        JOptionPane.showMessageDialog(janelaPrincipal, "Erro de banco de dados:\n" + e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
    }

    private static void tratarExclusao(SQLException e, String item) {
        if (e.getMessage() != null && e.getMessage().toUpperCase().contains("FOREIGN KEY")) {
            aviso("Este " + item + " possui aluguéis vinculados e não pode ser excluído.");
        } else {
            erroBD(e);
        }
    }

    /** Recarrega todas as tabelas e o painel inicial (um cadastro pode afetar várias telas). */
    private static void recarregarTudo() {
        recarregarVeiculos();
        recarregarClientes();
        recarregarAlugueis();
        recarregarFinanceiro();
    }

    private static void abrirFormVeiculo() {
        new FormVeiculo(janelaPrincipal, Testes::recarregarTudo).setVisible(true);
    }

    private static void abrirFormCliente() {
        new FormCliente(janelaPrincipal, Testes::recarregarTudo).setVisible(true);
    }

    private static void abrirFormAluguel() {
        FormAluguel.abrir(janelaPrincipal, Testes::recarregarTudo);
    }

    private static void abrirFormMov() {
        new FormMovimentacao(janelaPrincipal, Testes::recarregarTudo).setVisible(true);
    }

    private static void editarVeiculo(int id) {
        try {
            Veiculo v = veiculoDAO.buscar(id);
            if (v != null) new FormVeiculo(janelaPrincipal, Testes::recarregarTudo, v).setVisible(true);
        } catch (SQLException e) {
            erroBD(e);
        }
    }

    private static void editarCliente(int id) {
        try {
            Cliente c = clienteDAO.buscar(id);
            if (c != null) new FormCliente(janelaPrincipal, Testes::recarregarTudo, c).setVisible(true);
        } catch (SQLException e) {
            erroBD(e);
        }
    }

    private static void recarregarVeiculos() {
        modeloVeiculos.setRowCount(0);
        try {
            for (Veiculo v : veiculoDAO.listar()) {
                modeloVeiculos.addRow(new Object[]{v.id(), v.modelo(), v.placa(), v.ano(), v.categoria(), MOEDA.format(v.diaria()), v.status()});
            }
        } catch (SQLException e) {
            erroBD(e);
        }
        atualizarPainel();
    }

    private static void recarregarClientes() {
        modeloClientes.setRowCount(0);
        try {
            for (Cliente c : clienteDAO.listar()) {
                modeloClientes.addRow(new Object[]{c.id(), c.nome(), c.cpf(), c.telefone(), c.email(), c.dataCadastro().format(DATA)});
            }
        } catch (SQLException e) {
            erroBD(e);
        }
    }

    private static void recarregarAlugueis() {
        modeloAlugueis.setRowCount(0);
        try {
            for (Aluguel a : aluguelDAO.listar()) {
                modeloAlugueis.addRow(new Object[]{a.id(), a.clienteNome(), a.veiculo(), Fmt.data(a.retirada()),
                    Fmt.data(a.prevista()), Fmt.data(a.devolucao()), MOEDA.format(a.total()), a.status()});
            }
        } catch (SQLException e) {
            erroBD(e);
        }
        atualizarPainel();
    }

    private static void recarregarFinanceiro() {
        modeloMov.setRowCount(0);
        try {
            for (Movimentacao m : movimentacaoDAO.listar()) {
                modeloMov.addRow(new Object[]{m.id(), Fmt.data(m.data()), m.descricao(), m.categoria(), m.tipo(), MOEDA.format(m.valor())});
            }
            double receita = movimentacaoDAO.somaMes("Entrada", YearMonth.now());
            double despesa = movimentacaoDAO.somaMes("Saída", YearMonth.now());
            if (cardReceita != null) {
                setValor(cardReceita, MOEDA.format(receita));
                setValor(cardDespesa, MOEDA.format(despesa));
                setValor(cardSaldo, MOEDA.format(receita - despesa));
            }
        } catch (SQLException e) {
            erroBD(e);
        }
        atualizarPainel();
    }

    private static void excluirVeiculo(int id) {
        try { veiculoDAO.excluir(id); } catch (SQLException e) { tratarExclusao(e, "veículo"); }
        recarregarTudo();
    }

    private static void excluirCliente(int id) {
        try { clienteDAO.excluir(id); } catch (SQLException e) { tratarExclusao(e, "cliente"); }
        recarregarTudo();
    }

    private static void excluirMov(int id) {
        try {
            movimentacaoDAO.excluir(id);
        } catch (RegraException e) {
            aviso(e.getMessage());
        } catch (SQLException e) {
            erroBD(e);
        }
        recarregarTudo();
    }

    private static void registrarDevolucao(int id) {
        try {
            Aluguel a = aluguelDAO.buscar(id);
            if (a == null || !AluguelDAO.EM_ANDAMENTO.equals(a.status())) {
                aviso("Selecione um aluguel em andamento para registrar a devolução.");
                return;
            }
            LocalDate hoje = LocalDate.now();
            AluguelDAO.Fechamento f = AluguelDAO.calcular(a, hoje);
            String msg = "Contrato " + Fmt.codigo(id) + " - " + a.veiculo()
                    + "\nCliente: " + a.clienteNome()
                    + "\nRetirada: " + Fmt.data(a.retirada()) + "   Prevista: " + Fmt.data(a.prevista()) + "   Devolução: hoje"
                    + "\n\nDiárias cobradas: " + f.diasCobrados() + " x " + MOEDA.format(a.diaria()) + " = " + MOEDA.format(f.diasCobrados() * a.diaria())
                    + "\nDias de atraso: " + f.diasAtraso() + " (multa de 50% da diária por dia)  " + MOEDA.format(f.multa())
                    + "\nTOTAL: " + MOEDA.format(f.total())
                    + "\n\nConfirmar a devolução?";
            if (JOptionPane.showConfirmDialog(janelaPrincipal, msg, "Registrar devolução", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                aluguelDAO.devolver(id, hoje);
                recarregarTudo();
            }
        } catch (RegraException e) {
            aviso(e.getMessage());
        } catch (SQLException e) {
            erroBD(e);
        }
    }

    private static void cancelarAluguel(int id) {
        try {
            aluguelDAO.cancelar(id);
        } catch (RegraException e) {
            aviso(e.getMessage());
        } catch (SQLException e) {
            erroBD(e);
        }
        recarregarTudo();
    }

    /** Painel padrão de consulta: busca, tabela, botão de novo cadastro e botões de ação sobre a linha selecionada. */
    private static JPanel painelCadastroBD(DefaultTableModel modelo, String textoNovo, Runnable novo, Acao... acoes) {
        JPanel p = new JPanel(new BorderLayout(0, 14));
        p.setOpaque(false);

        JTextField busca = new JTextField();
        busca.setPreferredSize(new Dimension(250, 34));
        busca.setToolTipText("Pesquisar");
        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        botoes.setOpaque(false);

        JTable tabela = new JTable(modelo);
        configurarTabela(tabela);
        javax.swing.table.TableRowSorter<DefaultTableModel> sorter = new javax.swing.table.TableRowSorter<>(modelo);
        tabela.setRowSorter(sorter);

        for (Acao a : acoes) {
            BotaoArredondado b = new BotaoArredondado(a.texto(), a.cor(), a.hover());
            b.setPreferredSize(new Dimension(110, 36));
            b.addActionListener(e -> {
                int linha = tabela.getSelectedRow();
                if (linha < 0) {
                    JOptionPane.showMessageDialog(p, "Selecione um registro na tabela.", a.texto(), JOptionPane.INFORMATION_MESSAGE);
                    return;
                }
                if (a.confirmacao() != null
                        && JOptionPane.showConfirmDialog(p, a.confirmacao(), "Confirmar", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
                    return;
                }
                a.executar().accept((Integer) modelo.getValueAt(tabela.convertRowIndexToModel(linha), 0));
            });
            botoes.add(b);
        }
        BotaoArredondado btNovo = new BotaoArredondado(textoNovo, BLUE, new Color(48, 70, 101));
        btNovo.setPreferredSize(new Dimension(170, 36));
        btNovo.addActionListener(e -> novo.run());
        botoes.add(btNovo);

        JPanel barra = new JPanel(new BorderLayout());
        barra.setOpaque(false);
        barra.add(busca, BorderLayout.WEST);
        barra.add(botoes, BorderLayout.EAST);

        busca.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            private void filtrar() {
                String termo = busca.getText().trim();
                sorter.setRowFilter(termo.isEmpty() ? null : RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(termo)));
            }
            public void insertUpdate(javax.swing.event.DocumentEvent e) { filtrar(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { filtrar(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { filtrar(); }
        });
        JScrollPane scroll = new JScrollPane(tabela);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(226, 231, 238)));

        p.add(barra, BorderLayout.NORTH);
        p.add(scroll, BorderLayout.CENTER);
        return p;
    }
}
