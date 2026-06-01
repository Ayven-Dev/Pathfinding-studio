package com.pathfinding.ui.theme;

import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Charte graphique inspirée des design tokens d'Apple (macOS Big Sur / iOS).
 * Tout est constant, pas de configuration runtime : on garde la cohérence
 * visuelle simple. Les couleurs viennent de la palette système Apple.
 */
public final class Theme {

    // ---------- Couleurs ----------
    public static final Color BG_WINDOW    = new Color(0xF2F2F7);
    public static final Color BG_CARD      = new Color(0xFFFFFF);
    public static final Color BG_SUBTLE    = new Color(0xF7F7FA);
    public static final Color BG_HOVER     = new Color(0xE5E5EA);

    public static final Color BORDER       = new Color(0xD1D1D6);
    public static final Color BORDER_FOCUS = new Color(0x007AFF);

    public static final Color TEXT_PRIMARY   = new Color(0x1C1C1E);
    public static final Color TEXT_SECONDARY = new Color(0x6C6C70);
    public static final Color TEXT_ON_ACCENT = new Color(0xFFFFFF);

    public static final Color ACCENT       = new Color(0x007AFF);  // SF Blue
    public static final Color ACCENT_HOVER = new Color(0x0064D8);
    public static final Color ACCENT_PRESS = new Color(0x0050B5);

    public static final Color DESTRUCTIVE  = new Color(0xFF3B30);
    public static final Color SUCCESS      = new Color(0x34C759);
    public static final Color WARNING      = new Color(0xFF9500);
    public static final Color VIOLET       = new Color(0x5856D6);

    // ---------- Couleurs spécifiques à la visualisation ----------
    public static final Color CANVAS_BG    = new Color(0xFAFAFA);
    public static final Color GRID_LINE    = new Color(0xE5E5EA);
    public static final Color OBSTACLE     = new Color(0x3A3A3C);
    public static final Color START        = new Color(0x0A84FF);  // bleu
    public static final Color GOAL         = new Color(0x34C759);  // vert
    public static final Color PATH         = new Color(0x000000);  // noir : trajet final
    /** Cases visitées par la recherche mais qui ne font pas partie du chemin final. */
    public static final Color EXPLORED      = new Color(255,  59,  48, 110);  // rouge translucide
    public static final Color EXPLORED_DOT  = new Color(255,  59,  48, 200);  // rouge soutenu pour points

    // ---------- Typographie ----------
    public static final Font FONT_TITLE_XL;
    public static final Font FONT_TITLE;
    public static final Font FONT_HEADER;
    public static final Font FONT_BODY;
    public static final Font FONT_CAPTION;
    public static final Font FONT_MONO;

    static {
        // On choisit la première police disponible dans une liste préférée
        // proche de SF (San Francisco) — Inter, Segoe UI Variable, Segoe UI.
        String family = pickFont(
                "SF Pro Display", "SF Pro Text", "Inter",
                "Segoe UI Variable Display", "Segoe UI",
                "Helvetica Neue", "Helvetica", "Arial");
        String mono = pickFont("SF Mono", "JetBrains Mono", "Cascadia Mono",
                "Consolas", "Menlo", "Monospaced");
        FONT_TITLE_XL = new Font(family, Font.BOLD,  28);
        FONT_TITLE    = new Font(family, Font.BOLD,  20);
        FONT_HEADER   = new Font(family, Font.BOLD,  13);
        FONT_BODY     = new Font(family, Font.PLAIN, 13);
        FONT_CAPTION  = new Font(family, Font.PLAIN, 11);
        FONT_MONO     = new Font(mono,   Font.PLAIN, 12);
    }

    // ---------- Rayons / paddings ----------
    public static final int CARD_RADIUS    = 12;
    public static final int BUTTON_RADIUS  = 8;
    public static final int PADDING_SM     = 8;
    public static final int PADDING_MD     = 16;
    public static final int PADDING_LG     = 24;

    private Theme() {}

    /** Renvoie la première police existant dans l'environnement. */
    private static String pickFont(String... candidates) {
        Set<String> available = new HashSet<>(Arrays.asList(
                GraphicsEnvironment.getLocalGraphicsEnvironment()
                        .getAvailableFontFamilyNames()));
        for (String c : candidates) {
            if (available.contains(c)) return c;
        }
        return candidates[candidates.length - 1];
    }

    /** À appeler une fois au démarrage pour propager certains tokens à Swing. */
    public static void applyGlobalDefaults() {
        UIManager.put("Panel.background", BG_WINDOW);
        UIManager.put("Label.foreground", TEXT_PRIMARY);
        UIManager.put("Label.font", FONT_BODY);
        UIManager.put("TextField.font", FONT_BODY);
        UIManager.put("TextArea.font", FONT_MONO);
        UIManager.put("ComboBox.font", FONT_BODY);
        UIManager.put("Spinner.font", FONT_BODY);
        UIManager.put("CheckBox.font", FONT_BODY);
        UIManager.put("RadioButton.font", FONT_BODY);
        UIManager.put("Button.font", FONT_BODY);
        UIManager.put("ToolTip.font", FONT_CAPTION);
        UIManager.put("Slider.background", BG_WINDOW);
    }
}
