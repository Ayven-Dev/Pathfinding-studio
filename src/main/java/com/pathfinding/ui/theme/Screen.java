package com.pathfinding.ui.theme;

import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;

/**
 * Petit utilitaire de dimensionnement responsive. Toutes les fenêtres
 * passent par ces helpers pour obtenir une taille initiale qui s'adapte à la
 * résolution de l'écran (en tenant compte de la barre des tâches /
 * dock / etc. via {@link GraphicsEnvironment#getMaximumWindowBounds()}).
 */
public final class Screen {

    private Screen() {}

    /** Aire utilisable d'écran (sans la barre des tâches). */
    public static Rectangle usable() {
        return GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
    }

    /** Taille initiale du menu principal : confortable mais discrète. */
    public static Dimension menuSize() {
        Rectangle b = usable();
        int w = clamp((int) (b.width  * 0.55), 720, 980);
        int h = clamp((int) (b.height * 0.65), 480, 640);
        return new Dimension(w, h);
    }

    /** Taille initiale d'un éditeur (grille ou continu). */
    public static Dimension editorSize() {
        Rectangle b = usable();
        int w = clamp((int) (b.width  * 0.90), 900, 1500);
        int h = clamp((int) (b.height * 0.90), 600,  960);
        return new Dimension(w, h);
    }

    /** Taille minimum sous laquelle on refuse de descendre (utile sur petit écran). */
    public static Dimension editorMin() {
        Rectangle b = usable();
        int w = Math.min(820, b.width  - 40);
        int h = Math.min(560, b.height - 60);
        return new Dimension(w, h);
    }

    /** Largeur recommandée pour la barre latérale. */
    public static int sidebarWidth() {
        Rectangle b = usable();
        return clamp(b.width / 4, 280, 340);
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
