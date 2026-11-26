package com.mycompany.testes;

import javax.swing.*;
import java.awt.*;

/** Cores e componentes visuais compartilhados pelos formulários (define o design do sistema). */
final class Estilo {
    static final Color NAVY = new Color(23, 36, 58);
    static final Color BLUE = new Color(35, 52, 78);
    static final Color BG = new Color(244, 247, 251);
    static final Color TEXT = new Color(40, 52, 68);
    static final Color MUTED = new Color(112, 125, 142);
    static final Color BORDA = new Color(226, 231, 238);

    private Estilo() { }

    static JPanel cabecalho(String titulo, String subtitulo) {
        JPanel p = new JPanel();
        p.setBackground(NAVY);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(BorderFactory.createEmptyBorder(16, 26, 16, 26));
        JLabel t = new JLabel(titulo);
        t.setForeground(Color.WHITE);
        t.setFont(new Font("Arial", Font.BOLD, 20));
        JLabel s = new JLabel(subtitulo);
        s.setForeground(new Color(165, 180, 200));
        s.setFont(new Font("Arial", Font.PLAIN, 12));
        p.add(t);
        p.add(Box.createVerticalStrut(4));
        p.add(s);
        return p;
    }

    /** Adiciona uma linha "rótulo + campo" ao formulário e devolve o índice da próxima linha. */
    static int linha(JPanel form, int y, String rotulo, JComponent campo) {
        JLabel l = new JLabel(rotulo);
        l.setFont(new Font("Arial", Font.BOLD, 12));
        l.setForeground(TEXT);
        campo.setFont(new Font("Arial", Font.PLAIN, 13));
        campo.setPreferredSize(new Dimension(270, 34));
        if (campo instanceof JTextField) {
            campo.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BORDA), BorderFactory.createEmptyBorder(4, 8, 4, 8)));
        }
        GridBagConstraints g = new GridBagConstraints();
        g.gridy = y;
        g.insets = new Insets(6, 0, 6, 14);
        g.anchor = GridBagConstraints.WEST;
        g.gridx = 0;
        form.add(l, g);
        g.gridx = 1;
        g.weightx = 1;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(6, 0, 6, 0);
        form.add(campo, g);
        return y + 1;
    }

    static JPanel rodape(Runnable cancelar, Runnable salvar, JRootPane raiz) {
        JPanel r = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        r.setBackground(BG);
        r.setBorder(BorderFactory.createEmptyBorder(0, 16, 4, 16));
        BotaoArredondado bCancelar = new BotaoArredondado("Cancelar", new Color(120, 134, 152), new Color(100, 114, 132));
        BotaoArredondado bSalvar = new BotaoArredondado("Salvar", BLUE, new Color(48, 70, 101));
        bCancelar.setPreferredSize(new Dimension(110, 38));
        bSalvar.setPreferredSize(new Dimension(110, 38));
        bCancelar.addActionListener(e -> cancelar.run());
        bSalvar.addActionListener(e -> salvar.run());
        r.add(bCancelar);
        r.add(bSalvar);
        raiz.setDefaultButton(bSalvar);
        raiz.registerKeyboardAction(e -> cancelar.run(), KeyStroke.getKeyStroke("ESCAPE"), JComponent.WHEN_IN_FOCUSED_WINDOW);
        return r;
    }

    static void aviso(Component pai, String msg) {
        JOptionPane.showMessageDialog(pai, msg, "Verifique os dados", JOptionPane.WARNING_MESSAGE);
    }
}
