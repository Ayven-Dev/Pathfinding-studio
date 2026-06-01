package com.pathfinding.ui.shared;

import com.pathfinding.ui.theme.Screen;
import com.pathfinding.ui.theme.Theme;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import java.awt.Dimension;

/**
 * Petit conteneur pour une section de la barre latérale : un titre en
 * majuscules grisé et un empilement vertical d'éléments. Donne le rythme
 * visuel "type Preferences pane" caractéristique des UIs Apple.
 */
public final class SidebarSection extends JPanel {

    public SidebarSection(String title) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setOpaque(false);
        setAlignmentX(LEFT_ALIGNMENT);

        JLabel header = new JLabel(title.toUpperCase());
        header.setFont(Theme.FONT_CAPTION.deriveFont(java.awt.Font.BOLD));
        header.setForeground(Theme.TEXT_SECONDARY);
        header.setAlignmentX(LEFT_ALIGNMENT);
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0));
        add(header);
    }

    public SidebarSection row(JComponent c) {
        c.setAlignmentX(LEFT_ALIGNMENT);
        // On fige la hauteur (la largeur restant étirable) pour empiler
        // proprement boutons, curseurs et listes sans qu'ils ne grandissent.
        c.setMaximumSize(new Dimension(Integer.MAX_VALUE, c.getPreferredSize().height));
        add(c);
        add(Box.createRigidArea(new Dimension(0, 6)));
        return this;
    }

    /**
     * Ajoute un texte d'aide qui se replie. Le repli d'un {@link JTextArea}
     * dépend de sa largeur ; comme la barre latérale a une largeur fixe
     * ({@link Screen#sidebarWidth()}), on la calcule ici pour en déduire la
     * hauteur exacte du texte replié — sinon Swing afficherait tout sur une
     * ligne, tronquée.
     */
    public SidebarSection paragraph(String text) {
        JTextArea area = new JTextArea(text);
        area.setFont(Theme.FONT_CAPTION);
        area.setForeground(Theme.TEXT_SECONDARY);
        area.setEditable(false);
        area.setFocusable(false);
        area.setOpaque(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setBorder(null);
        area.setAlignmentX(LEFT_ALIGNMENT);

        // Largeur utile = largeur sidebar - marges de la carte - barre de défilement.
        int width = Math.max(120, Screen.sidebarWidth() - 2 * Theme.PADDING_MD - 22);
        area.setSize(width, Short.MAX_VALUE);   // déclenche le calcul de hauteur repliée
        int height = area.getPreferredSize().height;
        area.setPreferredSize(new Dimension(width, height));
        area.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));

        add(area);
        add(Box.createRigidArea(new Dimension(0, 6)));
        return this;
    }

    public SidebarSection gap(int h) {
        add(Box.createRigidArea(new Dimension(0, h)));
        return this;
    }
}
