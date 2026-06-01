package com.pathfinding.ui.theme;

import javax.swing.JComponent;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

/**
 * SegmentedControl à la iOS : une barre horizontale de N segments, l'un
 * sélectionné, fond unique avec rectangle blanc indiquant la sélection.
 * Plus compact et plus moderne qu'un groupe de radios.
 */
public final class SegmentedControl extends JComponent {

    private final List<String> labels;
    private int selected = 0;
    private IntConsumer listener;

    public SegmentedControl(List<String> labels) {
        this(labels, 0);
    }

    public SegmentedControl(List<String> labels, int initial) {
        this.labels = new ArrayList<>(labels);
        this.selected = initial;
        setFont(Theme.FONT_BODY.deriveFont(java.awt.Font.BOLD));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setOpaque(false);
        addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                int seg = e.getX() * SegmentedControl.this.labels.size() / Math.max(1, getWidth());
                if (seg >= 0 && seg < SegmentedControl.this.labels.size()) {
                    setSelectedIndex(seg);
                }
            }
        });
    }

    public int getSelectedIndex() { return selected; }

    public void setSelectedIndex(int i) {
        if (i == selected) return;
        selected = i;
        if (listener != null) listener.accept(i);
        repaint();
    }

    public void onChange(IntConsumer listener) { this.listener = listener; }

    @Override
    public Dimension getPreferredSize() {
        FontMetrics fm = getFontMetrics(getFont());
        int maxW = 0;
        for (String s : labels) maxW = Math.max(maxW, fm.stringWidth(s));
        int w = (maxW + 32) * labels.size();
        return new Dimension(w, fm.getHeight() + 16);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = getWidth(), h = getHeight();
        int r = Math.min(10, h / 2);

        // Fond
        g2.setColor(new Color(0x78788028, true));
        g2.fillRoundRect(0, 0, w, h, r, r);

        // Sélection (pastille blanche)
        int segW = w / labels.size();
        int selX = selected * segW;
        g2.setColor(Theme.BG_CARD);
        g2.fillRoundRect(selX + 2, 2, segW - 4, h - 4, r, r);
        g2.setColor(new Color(0, 0, 0, 12));
        g2.drawRoundRect(selX + 2, 2, segW - 4, h - 4, r, r);

        // Labels
        FontMetrics fm = g2.getFontMetrics(getFont());
        for (int i = 0; i < labels.size(); i++) {
            Rectangle seg = new Rectangle(i * segW, 0, segW, h);
            String s = labels.get(i);
            int tx = seg.x + (seg.width - fm.stringWidth(s)) / 2;
            int ty = seg.y + (seg.height - fm.getHeight()) / 2 + fm.getAscent();
            g2.setFont(getFont());
            g2.setColor(i == selected ? Theme.TEXT_PRIMARY : Theme.TEXT_SECONDARY);
            g2.drawString(s, tx, ty);
        }

        g2.dispose();
    }
}
