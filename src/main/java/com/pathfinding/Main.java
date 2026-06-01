package com.pathfinding;

import com.pathfinding.ui.MainMenu;
import com.pathfinding.ui.theme.Theme;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Point d'entrée. Configure le Look and Feel système, applique le thème
 * Apple-like et ouvre le menu principal.
 */
public final class Main {

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // Look & Feel par défaut sinon
        }
        SwingUtilities.invokeLater(() -> {
            Theme.applyGlobalDefaults();
            MainMenu.open();
        });
    }

    private Main() {}
}
