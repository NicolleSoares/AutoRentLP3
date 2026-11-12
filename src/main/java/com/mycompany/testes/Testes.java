package com.mycompany.testes;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.function.IntConsumer;

public class Testes {
    private static final VeiculoDAO veiculoDAO = new VeiculoDAO();
    private static final ClienteDAO clienteDAO = new ClienteDAO();
    private static final NumberFormat MOEDA = NumberFormat.getCurrencyInstance(Locale.of("pt", "BR"));
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DefaultTableModel modeloVeiculos = novoModelo("Código", "Modelo", "Placa", "Ano", "Categoria", "Diária", "Situação");
    private static final DefaultTableModel modeloClientes = novoModelo("Código", "Nome", "CPF", "Telefone", "E-mail", "Cadastro");
    private static JFrame janelaPrincipal;
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
        JLabel versao = new JLabel("AUTO RENT • PP1");
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

        recarregarVeiculos();
        recarregarClientes();

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
        indicadores.add(cartaoIndicador("VEÍCULOS", "18", "Total cadastrado", new Color(65, 117, 190)));
        indicadores.add(cartaoIndicador("DISPONÍVEIS", "12", "Prontos para locação", new Color(39, 150, 112)));
        indicadores.add(cartaoIndicador("ALUGADOS", "06", "Em circulação", new Color(218, 151, 54)));
        indicadores.add(cartaoIndicador("FATURAMENTO", "R$ 18.450", "Neste mês", new Color(117, 94, 177)));
        centro.add(indicadores);
        centro.add(Box.createVerticalStrut(24));

        JLabel tituloFrota = new JLabel("Veículos em destaque");
        tituloFrota.setFont(new Font("Arial", Font.BOLD, 19));
        tituloFrota.setForeground(TEXT);
        tituloFrota.setAlignmentX(Component.LEFT_ALIGNMENT);
        centro.add(tituloFrota);
        centro.add(Box.createVerticalStrut(12));

        JPanel frota = new JPanel(new GridLayout(1, 3, 14, 0));
        frota.setOpaque(false);
        frota.add(cartaoCarro("SEDAN", "Toyota Corolla", "2023 • Automático", "R$ 180 / diária", "Disponível", new Color(225, 235, 245)));
        frota.add(cartaoCarro("HATCH", "Hyundai HB20", "2022 • Manual", "R$ 120 / diária", "Alugado", new Color(233, 237, 242)));
        frota.add(cartaoCarro("SUV", "Jeep Renegade", "2024 • Automático", "R$ 220 / diária", "Disponível", new Color(226, 235, 230)));
        centro.add(frota);
        centro.add(Box.createVerticalStrut(20));

        JPanel aviso = new JPanel(new BorderLayout());
        aviso.setBackground(Color.WHITE);
        aviso.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(226, 231, 238)), BorderFactory.createEmptyBorder(14, 17, 14, 17)));
        JLabel avisoTitulo = new JLabel("Próxima devolução");
        avisoTitulo.setFont(new Font("Arial", Font.BOLD, 14));
        avisoTitulo.setForeground(TEXT);
        JLabel avisoTexto = new JLabel("  Hyundai HB20 • Cliente: Mariana Costa • Hoje, às 18:00");
        avisoTexto.setForeground(MUTED);
        aviso.add(avisoTitulo, BorderLayout.WEST);
        aviso.add(avisoTexto, BorderLayout.CENTER);
        centro.add(aviso);

        painel.add(centro, BorderLayout.CENTER);
        return painel;
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
        JPanel painel = baseTela("Veículos", "Consulte a frota e cadastre novos automóveis.");
        painel.add(painelCadastroBD(modeloVeiculos, "+ Novo veículo", Testes::abrirFormVeiculo, Testes::excluirVeiculo, "veículo"), BorderLayout.CENTER);
        return painel;
    }

    private static JPanel telaClientes() {
        JPanel painel = baseTela("Clientes", "Cadastro e consulta dos clientes da locadora.");
        painel.add(painelCadastroBD(modeloClientes, "+ Novo cliente", Testes::abrirFormCliente, Testes::excluirCliente, "cliente"), BorderLayout.CENTER);
        return painel;
    }

    private static JPanel telaAlugueis() {
        JPanel painel = baseTela("Aluguéis", "Acompanhe contratos, retiradas e devoluções.");
        String[] colunas = {"Contrato", "Cliente", "Veículo", "Retirada", "Devolução", "Status"};
        Object[][] dados = {
            {"#0018", "Mariana Costa", "Hyundai HB20", "08/11/2026", "10/11/2026", "Em andamento"},
            {"#0017", "Lucas Almeida", "Toyota Corolla", "05/11/2026", "09/11/2026", "Concluído"},
            {"#0019", "Ana Ribeiro", "Jeep Renegade", "10/11/2026", "14/11/2026", "Agendado"}
        };
        painel.add(painelTabela(colunas, dados, "+ Novo aluguel", "Novo aluguel", new String[]{"Cliente", "Veículo", "Data de retirada", "Data de devolução"}), BorderLayout.CENTER);
        return painel;
    }

    private static JPanel telaFinanceiro() {
        JPanel painel = baseTela("Financeiro", "Visão demonstrativa das movimentações da locadora.");
        JPanel centro = new JPanel(new BorderLayout(0, 18)); centro.setOpaque(false);
        JPanel resumo = new JPanel(new GridLayout(1, 3, 14, 0)); resumo.setOpaque(false);
        resumo.add(cartaoIndicador("RECEITA DO MÊS", "R$ 18.450,00", "Entradas registradas", new Color(39, 150, 112)));
        resumo.add(cartaoIndicador("DESPESAS", "R$ 4.280,00", "Despesas registradas", new Color(190, 94, 82)));
        resumo.add(cartaoIndicador("SALDO", "R$ 14.170,00", "Resultado do período", new Color(65, 117, 190)));
        centro.add(resumo, BorderLayout.NORTH);
        String[] colunas = {"Data", "Descrição", "Categoria", "Valor", "Tipo"};
        Object[][] dados = {
            {"10/11/2026", "Aluguel Hyundai HB20", "Locação", "R$ 360,00", "Entrada"},
            {"09/11/2026", "Manutenção Chevrolet Onix", "Manutenção", "R$ 480,00", "Saída"},
            {"08/11/2026", "Aluguel Toyota Corolla", "Locação", "R$ 720,00", "Entrada"}
        };
        centro.add(tabela(colunas, dados), BorderLayout.CENTER);
        painel.add(centro, BorderLayout.CENTER);
        return painel;
    }

    private static JPanel telaRelatorios() {
        JPanel painel = baseTela("Relatórios", "Resumo de informações para acompanhamento da operação.");
        JPanel lista = new JPanel(new GridLayout(3, 1, 0, 13)); lista.setOpaque(false);
        lista.add(cartaoRelatorio("Relatório de veículos", "Consulte veículos disponíveis, alugados e em manutenção."));
        lista.add(cartaoRelatorio("Relatório de aluguéis", "Visualize contratos por período e situação."));
        lista.add(cartaoRelatorio("Relatório financeiro", "Acompanhe receitas, despesas e resultados."));
        painel.add(lista, BorderLayout.CENTER);
        return painel;
    }

    private static JPanel cartaoRelatorio(String titulo, String descricao) {
        JPanel p = new JPanel(new BorderLayout(12, 0)); p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(226, 231, 238)), BorderFactory.createEmptyBorder(17, 18, 17, 18)));
        JLabel icone = new JLabel("▤"); icone.setFont(new Font("Arial", Font.BOLD, 25)); icone.setForeground(new Color(65, 117, 190));
        JPanel textos = new JPanel(); textos.setOpaque(false); textos.setLayout(new BoxLayout(textos, BoxLayout.Y_AXIS));
        JLabel t = new JLabel(titulo); t.setFont(new Font("Arial", Font.BOLD, 15)); t.setForeground(TEXT);
        JLabel d = new JLabel(descricao); d.setFont(new Font("Arial", Font.PLAIN, 12)); d.setForeground(MUTED);
        textos.add(t); textos.add(Box.createVerticalStrut(5)); textos.add(d);
        JButton abrir = new JButton("Visualizar"); abrir.setFocusPainted(false);
        p.add(icone, BorderLayout.WEST); p.add(textos, BorderLayout.CENTER); p.add(abrir, BorderLayout.EAST);
        abrir.addActionListener(e -> JOptionPane.showMessageDialog(p, "Este relatório é ilustrativo neste protótipo.", "Relatório", JOptionPane.INFORMATION_MESSAGE));
        return p;
    }

    private static JPanel painelTabela(String[] colunas, Object[][] dados, String textoBotao, String tituloDialogo, String[] campos) {
        JPanel p = new JPanel(new BorderLayout(0, 14)); p.setOpaque(false);
        JPanel barra = new JPanel(new BorderLayout()); barra.setOpaque(false);
        JTextField busca = new JTextField(); busca.setPreferredSize(new Dimension(250, 34)); busca.setToolTipText("Pesquisar");
        JButton novo = new JButton(textoBotao); novo.setBackground(BLUE); novo.setForeground(Color.WHITE); novo.setFocusPainted(false);
        barra.add(busca, BorderLayout.WEST); barra.add(novo, BorderLayout.EAST);
        DefaultTableModel modelo = new DefaultTableModel(dados, colunas) { public boolean isCellEditable(int r, int c) { return false; } };
        JTable tabela = new JTable(modelo);
        configurarTabela(tabela);
        JScrollPane scroll = new JScrollPane(tabela); scroll.setBorder(BorderFactory.createLineBorder(new Color(226, 231, 238)));
        busca.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            private void filtrar() {
                String termo = busca.getText().toLowerCase();
                javax.swing.table.TableRowSorter<DefaultTableModel> sorter = new javax.swing.table.TableRowSorter<>(modelo);
                tabela.setRowSorter(sorter);
                sorter.setRowFilter(termo.isBlank() ? null : javax.swing.RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(termo)));
            }
            public void insertUpdate(javax.swing.event.DocumentEvent e) { filtrar(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { filtrar(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { filtrar(); }
        });
        novo.addActionListener(e -> {
            JTextField[] entradas = new JTextField[campos.length];
            JPanel formulario = new JPanel(new GridLayout(campos.length, 2, 8, 8));
            for (int i = 0; i < campos.length; i++) { entradas[i] = new JTextField(15); formulario.add(new JLabel(campos[i] + ":")); formulario.add(entradas[i]); }
            int opcao = JOptionPane.showConfirmDialog(p, formulario, tituloDialogo, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (opcao == JOptionPane.OK_OPTION) {
                Object[] linha = new Object[colunas.length];
                for (int i = 0; i < linha.length; i++) linha[i] = i < entradas.length ? entradas[i].getText().trim() : "—";
                modelo.addRow(linha);
            }
        });
        p.add(barra, BorderLayout.NORTH); p.add(scroll, BorderLayout.CENTER);
        return p;
    }

    private static JPanel tabela(String[] colunas, Object[][] dados) {
        JPanel p = new JPanel(new BorderLayout()); p.setOpaque(false);
        DefaultTableModel modelo = new DefaultTableModel(dados, colunas) { public boolean isCellEditable(int r, int c) { return false; } };
        JTable tabela = new JTable(modelo); configurarTabela(tabela);
        JScrollPane scroll = new JScrollPane(tabela); scroll.setBorder(BorderFactory.createLineBorder(new Color(226, 231, 238)));
        p.add(scroll, BorderLayout.CENTER); return p;
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
        locacoes.add(item("Novo aluguel", e -> cards.show(conteudo, "alugueis")));
        locacoes.add(item("Consultar aluguéis", e -> cards.show(conteudo, "alugueis")));

        JMenu financeiro = menu("Financeiro", KeyEvent.VK_F);
        financeiro.add(item("Movimentações", e -> cards.show(conteudo, "financeiro")));

        JMenu relatorios = menu("Relatórios", KeyEvent.VK_R);
        relatorios.add(item("Relatórios disponíveis", e -> cards.show(conteudo, "relatorios")));

        JMenu ajuda = menu("Ajuda", KeyEvent.VK_J);
        ajuda.add(item("Sobre", e -> JOptionPane.showMessageDialog(janelaPrincipal,
                "AUTO RENT | Gestão de Locadora\nLinguagem de Programação III - IC805\nTrabalho prático - 1ª entrega (PP1)",
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

    // ===== Cadastros ligados ao banco de dados =====

    private static DefaultTableModel novoModelo(String... colunas) {
        return new DefaultTableModel(colunas, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
    }

    private static void abrirFormVeiculo() {
        new FormVeiculo(janelaPrincipal, Testes::recarregarVeiculos).setVisible(true);
    }

    private static void abrirFormCliente() {
        new FormCliente(janelaPrincipal, Testes::recarregarClientes).setVisible(true);
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

    private static void excluirVeiculo(int id) {
        try { veiculoDAO.excluir(id); } catch (SQLException e) { erroBD(e); }
        recarregarVeiculos();
    }

    private static void excluirCliente(int id) {
        try { clienteDAO.excluir(id); } catch (SQLException e) { erroBD(e); }
        recarregarClientes();
    }

    private static void erroBD(SQLException e) {
        JOptionPane.showMessageDialog(janelaPrincipal, "Erro de banco de dados:\n" + e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
    }

    /** Painel padrão de consulta: busca, tabela, botão de novo cadastro e exclusão. */
    private static JPanel painelCadastroBD(DefaultTableModel modelo, String textoNovo, Runnable novo, IntConsumer excluir, String item) {
        JPanel p = new JPanel(new BorderLayout(0, 14));
        p.setOpaque(false);

        JTextField busca = new JTextField();
        busca.setPreferredSize(new Dimension(250, 34));
        busca.setToolTipText("Pesquisar");
        BotaoArredondado btNovo = new BotaoArredondado(textoNovo, BLUE, new Color(48, 70, 101));
        BotaoArredondado btExcluir = new BotaoArredondado("Excluir", new Color(190, 94, 82), new Color(165, 75, 64));
        btNovo.setPreferredSize(new Dimension(150, 36));
        btExcluir.setPreferredSize(new Dimension(100, 36));
        JPanel acoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        acoes.setOpaque(false);
        acoes.add(btExcluir);
        acoes.add(btNovo);
        JPanel barra = new JPanel(new BorderLayout());
        barra.setOpaque(false);
        barra.add(busca, BorderLayout.WEST);
        barra.add(acoes, BorderLayout.EAST);

        JTable tabela = new JTable(modelo);
        configurarTabela(tabela);
        javax.swing.table.TableRowSorter<DefaultTableModel> sorter = new javax.swing.table.TableRowSorter<>(modelo);
        tabela.setRowSorter(sorter);
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

        btNovo.addActionListener(e -> novo.run());
        btExcluir.addActionListener(e -> {
            int linha = tabela.getSelectedRow();
            if (linha < 0) {
                JOptionPane.showMessageDialog(p, "Selecione um " + item + " na tabela para excluir.", "Excluir", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            int op = JOptionPane.showConfirmDialog(p, "Deseja realmente excluir este " + item + "?", "Confirmar exclusão", JOptionPane.YES_NO_OPTION);
            if (op == JOptionPane.YES_OPTION) {
                excluir.accept((Integer) modelo.getValueAt(tabela.convertRowIndexToModel(linha), 0));
            }
        });

        p.add(barra, BorderLayout.NORTH);
        p.add(scroll, BorderLayout.CENTER);
        return p;
    }
}
