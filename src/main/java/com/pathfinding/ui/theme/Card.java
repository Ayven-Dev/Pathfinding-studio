package com.pathfinding.ui.theme;

import javax.swing.JPanel;
import javax.swing.Scrollable;
import javax.swing.border.EmptyBorder;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.awt.RenderingHints;

/**
 * Conteneur "carte" : fond blanc, coins arrondis, ombre subtile sous le bord
 * inférieur. C'est l'élément structurant principal de l'UI, à la manière des
 * widgets macOS / iOS.
 *
 * <p>Implémente {@link Scrollable} pour bien se comporter à l'intérieur d'un
 * {@link javax.swing.JScrollPane} : la largeur suit le viewport (donc pas de
 * scroll horizontal parasite) mais la hauteur reste celle du contenu (donc
 * scroll vertical si nécessaire).</p>
 */
public class Card extends JPanel implements Scrollable {

    private static final Color SHADOW = new Color(0, 0, 0, 18);
    private static final int SHADOW_OFFSET = 2;

    public Card() {
        this(null);
    }

    public Card(LayoutManager layout) {
        if (layout != null) setLayout(layout);
        setOpaque(false);
        setBorder(new EmptyBorder(Theme.PADDING_MD, Theme.PADDING_MD,
                                  Theme.PADDING_MD, Theme.PADDING_MD));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();
        int r = Theme.CARD_RADIUS;

        // Ombre douce sous la carte
        g2.setColor(SHADOW);
        g2.fillRoundRect(0, SHADOW_OFFSET, w, h - SHADOW_OFFSET, r, r);

        // Carte
        g2.setColor(Theme.BG_CARD);
        g2.fillRoundRect(0, 0, w, h - SHADOW_OFFSET, r, r);

        g2.dispose();
        super.paintComponent(g);
    }

    // ---------- Scrollable ----------

    @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
    @Override public int getScrollableUnitIncrement(Rectangle r, int orientation, int direction) { return 16; }
    @Override public int getScrollableBlockIncrement(Rectangle r, int orientation, int direction) { return 64; }
    @Override public boolean getScrollableTracksViewportWidth() { return true; }
    @Override public boolean getScrollableTracksViewportHeight() { return false; }
}
