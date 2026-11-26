package com.mycompany.testes;

import javax.swing.*;
import java.awt.*;

public class BotaoArredondado extends JButton {
    private Color corHover = new Color(48, 70, 101);

    /** Botão centralizado com cores próprias (usado nos formulários). */
    public BotaoArredondado(String texto, Color cor, Color hover) {
        this(texto);
        setBackground(cor);
        corHover = hover;
        setHorizontalAlignment(SwingConstants.CENTER);
        setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
    }

    public BotaoArredondado(String texto) {
        super(texto);
        setBackground(new Color(35, 52, 78));
        setForeground(Color.WHITE);
        setFocusPainted(false);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setOpaque(false);
        setFont(new Font("Arial", Font.BOLD, 14));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setHorizontalAlignment(SwingConstants.LEFT);
        setBorder(BorderFactory.createEmptyBorder(10, 18, 10, 10));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(getModel().isRollover() ? corHover : getBackground());
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
        g2.dispose();
        super.paintComponent(g);
    }
}
