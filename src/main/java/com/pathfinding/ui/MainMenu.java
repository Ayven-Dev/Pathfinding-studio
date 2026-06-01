package com.pathfinding.ui;

import com.pathfinding.ui.continuous.ContinuousEditorFrame;
import com.pathfinding.ui.grid.GridEditorFrame;
import com.pathfinding.ui.theme.Card;
import com.pathfinding.ui.theme.PillButton;
import com.pathfinding.ui.theme.Screen;
import com.pathfinding.ui.theme.Theme;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;

/**
 * Écran d'accueil : deux cartes (« Mode grille » / « Mode carte continue »)
 * qui ouvrent chacune leur éditeur.
 *
 * <p>Toute la mise en page vise à rester lisible quelle que soit la taille de
 * la fenêtre. Le principe : aucun texte n'a de largeur fixe en pixels. Les
 * paragraphes sont des {@link JTextArea} en retour à la ligne automatique,
 * posés dans des zones {@link BorderLayout#CENTER} qui leur donnent la largeur
 * disponible — le texte se replie au lieu d'être tronqué. Un
 * {@link JScrollPane} sert de filet de sécurité si la fenêtre devient vraiment
 * petite.</p>
 */
public final class MainMenu extends JFrame {

    private static MainMenu instance;

    public static void open() {
        if (instance == null) instance = new MainMenu();
        instance.setVisible(true);
        instance.toFront();
    }

    private MainMenu() {
        super("Pathfinding · Studio");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        // Contenu = en-tête + cartes + pied.
        JPanel content = new VerticallyScrollablePanel(new BorderLayout(0, Theme.PADDING_LG));
        content.setOpaque(false);
        content.setBorder(BorderFactory.createEmptyBorder(
                Theme.PADDING_LG, Theme.PADDING_LG, Theme.PADDING_LG, Theme.PADDING_LG));
        content.add(buildHeader(), BorderLayout.NORTH);
        content.add(buildCards(), BorderLayout.CENTER);
        content.add(buildFooter(), BorderLayout.SOUTH);

        // Scroll vertical de secours pour les fenêtres très basses. Le contenu
        // suit la largeur du viewport (VerticallyScrollablePanel) : jamais de
        // scroll horizontal, donc le texte se replie sur la largeur réelle.
        JScrollPane scroll = new JScrollPane(content,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Theme.BG_WINDOW);
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        setContentPane(scroll);

        setSize(Screen.menuSize());
        setMinimumSize(new Dimension(560, 460));
        setLocationRelativeTo(null);
    }

    /**
     * Panneau qui, dans un {@link JScrollPane}, épouse la largeur du viewport
     * mais garde sa hauteur naturelle. C'est ce qui permet aux paragraphes de
     * se replier sur la largeur visible au lieu de déborder horizontalement.
     */
    private static final class VerticallyScrollablePanel extends JPanel
            implements javax.swing.Scrollable {
        VerticallyScrollablePanel(java.awt.LayoutManager layout) { super(layout); }
        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(java.awt.Rectangle r, int o, int d) { return 16; }
        @Override public int getScrollableBlockIncrement(java.awt.Rectangle r, int o, int d) { return 64; }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }
    }

    private JPanel buildHeader() {
        // BorderLayout : titres en haut (hauteur naturelle), sous-titre repliable
        // au centre (prend la largeur restante).
        JPanel head = new JPanel(new BorderLayout(0, 6));
        head.setOpaque(false);

        JPanel titles = new JPanel();
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        titles.setOpaque(false);

        JLabel eyebrow = new JLabel("PATHFINDING STUDIO");
        eyebrow.setFont(Theme.FONT_CAPTION.deriveFont(Font.BOLD));
        eyebrow.setForeground(Theme.ACCENT);
        eyebrow.setAlignmentX(LEFT_ALIGNMENT);

        JLabel title = new JLabel("Choisis un environnement");
        title.setFont(Theme.FONT_TITLE_XL);
        title.setForeground(Theme.TEXT_PRIMARY);
        title.setAlignmentX(LEFT_ALIGNMENT);

        titles.add(eyebrow);
        titles.add(Box.createRigidArea(new Dimension(0, 6)));
        titles.add(title);

        JTextArea sub = paragraph(
                "Compare six algorithmes (A* 4-dir, A* 8-dir, Theta*, JPS, "
              + "RRT*, Theta*-RRT*) sur une grille discrète ou sur une carte "
              + "continue à obstacles polygonaux. Choisis le mode adapté à ton "
              + "scénario.", Theme.FONT_BODY, Theme.TEXT_SECONDARY);

        head.add(titles, BorderLayout.NORTH);
        head.add(sub, BorderLayout.CENTER);
        return head;
    }

    private JPanel buildCards() {
        JPanel row = new JPanel(new GridLayout(1, 2, Theme.PADDING_LG, 0));
        row.setOpaque(false);

        row.add(buildModeCard(
                "GRILLE DISCRÈTE",
                "Mode grille",
                "Quadrillage de cases. Les six algorithmes y sont disponibles : "
              + "la famille A* (4-dir, 8-dir, Theta*, JPS) plus les "
              + "planificateurs par échantillonnage (RRT*, Theta*-RRT*). Idéal "
              + "pour des cartes type tuiles.",
                Theme.ACCENT,
                () -> { setVisible(false); new GridEditorFrame().setVisible(true); }));

        row.add(buildModeCard(
                "CARTE CONTINUE",
                "Mode continu",
                "Espace à coordonnées réelles peuplé d'obstacles de forme "
              + "quelconque. Seuls les algorithmes qui gèrent nativement le "
              + "continu sont proposés ici : RRT* et Theta*-RRT*.",
                Theme.VIOLET,
                () -> { setVisible(false); new ContinuousEditorFrame().setVisible(true); }));

        return row;
    }

    private JPanel buildModeCard(String eyebrow, String title, String body,
                                  Color accent, Runnable action) {
        // BorderLayout : bloc titre en haut, description repliable au centre,
        // bouton en bas. La description occupe toute la largeur de la carte.
        Card card = new Card(new BorderLayout(0, Theme.PADDING_MD));

        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.setOpaque(false);

        JPanel banner = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                g2.setPaint(new GradientPaint(0, 0, accent, w, h, accent.darker()));
                g2.fillRoundRect(0, 0, w, h, 12, 12);
                g2.dispose();
            }
        };
        banner.setOpaque(false);
        banner.setAlignmentX(LEFT_ALIGNMENT);
        banner.setMaximumSize(new Dimension(Integer.MAX_VALUE, 72));
        banner.setPreferredSize(new Dimension(0, 72));

        JLabel eyebrowLabel = new JLabel(eyebrow);
        eyebrowLabel.setFont(Theme.FONT_CAPTION.deriveFont(Font.BOLD));
        eyebrowLabel.setForeground(accent);
        eyebrowLabel.setAlignmentX(LEFT_ALIGNMENT);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(Theme.FONT_TITLE);
        titleLabel.setForeground(Theme.TEXT_PRIMARY);
        titleLabel.setAlignmentX(LEFT_ALIGNMENT);

        top.add(banner);
        top.add(Box.createRigidArea(new Dimension(0, Theme.PADDING_MD)));
        top.add(eyebrowLabel);
        top.add(Box.createRigidArea(new Dimension(0, 4)));
        top.add(titleLabel);

        JTextArea bodyText = paragraph(body, Theme.FONT_BODY, Theme.TEXT_SECONDARY);

        PillButton openBtn = new PillButton("Ouvrir →", PillButton.Style.PRIMARY);
        openBtn.addActionListener(e -> action.run());
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        btnRow.setOpaque(false);
        btnRow.add(openBtn);

        card.add(top, BorderLayout.NORTH);
        card.add(bodyText, BorderLayout.CENTER);
        card.add(btnRow, BorderLayout.SOUTH);
        return card;
    }

    private JPanel buildFooter() {
        JLabel l = new JLabel("Java " + System.getProperty("java.version")
                + " · Pathfinding Studio");
        l.setFont(Theme.FONT_CAPTION);
        l.setForeground(Theme.TEXT_SECONDARY);
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        p.setOpaque(false);
        p.add(l);
        return p;
    }

    /**
     * Construit un paragraphe qui se replie tout seul : un {@link JTextArea}
     * transparent, non éditable, en retour à la ligne par mots. Placé dans une
     * zone qui lui impose une largeur (BorderLayout.CENTER), il calcule sa
     * hauteur en conséquence — le texte n'est donc jamais tronqué.
     */
    private static JTextArea paragraph(String text, Font font, Color fg) {
        JTextArea area = new JTextArea(text);
        area.setFont(font);
        area.setForeground(fg);
        area.setEditable(false);
        area.setFocusable(false);
        area.setOpaque(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setBorder(null);
        return area;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Theme.applyGlobalDefaults();
            open();
        });
    }
}
