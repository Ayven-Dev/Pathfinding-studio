package com.pathfinding.ui.theme;

import javax.swing.JButton;
import javax.swing.border.EmptyBorder;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Bouton "pilule" arrondi à la Apple. Trois variantes :
 *  - PRIMARY  : fond bleu accent, texte blanc (call-to-action)
 *  - SECONDARY: fond clair, bordure, texte primaire (action secondaire)
 *  - GHOST    : transparent, juste texte coloré (lien)
 */
public final class PillButton extends JButton {

    public enum Style { PRIMARY, SECONDARY, GHOST }

    private final Style style;
    private boolean hover;
    private boolean pressed;

    public PillButton(String text, Style style) {
        super(text);
        this.style = style;
        setFont(Theme.FONT_BODY.deriveFont(java.awt.Font.BOLD));
        setBorder(new EmptyBorder(8, 16, 8, 16));
        setContentAreaFilled(false);
        setFocusPainted(false);
        setBorderPainted(false);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setForeground(foreground());
        addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
            @Override public void mouseExited(MouseEvent e) { hover = false; pressed = false; repaint(); }
            @Override public void mousePressed(MouseEvent e) { pressed = true; repaint(); }
            @Override public void mouseReleased(MouseEvent e) { pressed = false; repaint(); }
        });
    }

    private Color foreground() {
        return switch (style) {
            case PRIMARY   -> Theme.TEXT_ON_ACCENT;
            case SECONDARY -> Theme.TEXT_PRIMARY;
            case GHOST     -> Theme.ACCENT;
        };
    }

    private Color background() {
        return switch (style) {
            case PRIMARY -> pressed ? Theme.ACCENT_PRESS : (hover ? Theme.ACCENT_HOVER : Theme.ACCENT);
            case SECONDARY -> hover ? Theme.BG_HOVER : Theme.BG_SUBTLE;
            case GHOST -> hover ? new Color(0, 0, 0, 12) : new Color(0, 0, 0, 0);
        };
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        d.height = Math.max(d.height, 34);
        d.width = Math.max(d.width, 90);
        return d;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = getWidth(), h = getHeight();
        int r = Math.min(Theme.BUTTON_RADIUS * 2, h);
        if (!isEnabled()) {
            g2.setColor(Theme.BG_HOVER);
        } else {
            g2.setColor(background());
        }
        g2.fillRoundRect(0, 0, w, h, r, r);

        if (style == Style.SECONDARY) {
            g2.setColor(Theme.BORDER);
            g2.drawRoundRect(0, 0, w - 1, h - 1, r, r);
        }

        g2.dispose();
        // Texte
        super.paintComponent(g);
    }
}
