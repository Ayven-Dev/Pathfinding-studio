package com.pathfinding.ui.shared;

import com.pathfinding.api.PathResult;
import com.pathfinding.benchmark.Benchmark;
import com.pathfinding.benchmark.BenchmarkResult;
import com.pathfinding.ui.theme.Card;
import com.pathfinding.ui.theme.Theme;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.util.Map;

/**
 * Carte d'affichage des résultats : un titre, puis un grand bloc monospaced
 * pour le détail d'un run ou le tableau du benchmark.
 */
public final class ResultsCard extends Card {

    private final JTextArea text = new JTextArea();
    private final JLabel title = new JLabel("Résultats");

    public ResultsCard() {
        super(new BorderLayout(0, Theme.PADDING_SM));
        title.setFont(Theme.FONT_TITLE);
        title.setForeground(Theme.TEXT_PRIMARY);
        add(title, BorderLayout.NORTH);

        text.setFont(Theme.FONT_MONO);
        text.setEditable(false);
        text.setBackground(Theme.BG_SUBTLE);
        text.setForeground(Theme.TEXT_PRIMARY);
        text.setBorder(BorderFactory.createEmptyBorder(
                Theme.PADDING_SM, Theme.PADDING_SM,
                Theme.PADDING_SM, Theme.PADDING_SM));
        text.setLineWrap(false);

        JScrollPane scroll = new JScrollPane(text,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroll.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        scroll.setPreferredSize(new Dimension(0, 220));
        scroll.getViewport().setBackground(Theme.BG_SUBTLE);
        add(scroll, BorderLayout.CENTER);
    }

    public void clear() {
        text.setText("");
    }

    public void showRun(PathResult r) {
        StringBuilder sb = new StringBuilder();
        sb.append("Run : ").append(r.algorithmName()).append('\n');
        sb.append("─".repeat(40)).append('\n');
        if (!r.success()) {
            sb.append("Échec : ").append(r.failureReason() == null ? "?" : r.failureReason()).append('\n');
        } else {
            sb.append(String.format("  temps          : %8.3f ms%n", r.elapsedMillis()));
            sb.append(String.format("  nœuds expansés : %8d%n", r.nodesExpanded()));
            sb.append(String.format("  nœuds générés  : %8d%n", r.nodesGenerated()));
            sb.append(String.format("  open max       : %8d%n", r.maxOpenSize()));
            sb.append(String.format("  longueur       : %8.2f%n", r.pathLength()));
            sb.append(String.format("  virages totaux : %7.1f° / %d virages%n",
                    r.totalTurnDegrees(), r.turnCount()));
            sb.append(String.format("  segments       : %8d%n", r.path().size()));
        }
        text.setText(sb.toString());
        text.setCaretPosition(0);
    }

    public void showBenchmark(Map<String, BenchmarkResult> results) {
        if (results.isEmpty()) { text.setText("Aucun algorithme à évaluer."); return; }
        StringBuilder sb = new StringBuilder();
        BenchmarkResult any = results.values().iterator().next();
        sb.append("Benchmark — ").append(any.runs()).append(" runs / algorithme")
                .append(" (premier run écarté)\n");
        sb.append(Benchmark.formatTable(results));

        // Synthèse
        BenchmarkResult fastest = null, shortest = null, fewest = null, smoothest = null;
        for (BenchmarkResult r : results.values()) {
            if (!r.success()) continue;
            if (fastest  == null || r.avgMillis() < fastest.avgMillis())     fastest  = r;
            if (shortest == null || r.pathLength() < shortest.pathLength())  shortest = r;
            if (fewest   == null || r.nodesExpanded() < fewest.nodesExpanded()) fewest = r;
            if (smoothest == null || r.totalTurnDegrees() < smoothest.totalTurnDegrees()) smoothest = r;
        }
        sb.append('\n');
        if (fastest  != null) sb.append("Le + rapide          : ").append(fastest.algorithmName()).append('\n');
        if (fewest   != null) sb.append("Le – d'expansions    : ").append(fewest.algorithmName()).append('\n');
        if (shortest != null) sb.append("Le chemin + court    : ").append(shortest.algorithmName()).append('\n');
        if (smoothest != null) sb.append("Le chemin + lisse    : ").append(smoothest.algorithmName()).append('\n');

        text.setText(sb.toString());
        text.setCaretPosition(0);
    }

    public void setTitle(String s) {
        title.setText(s);
    }
}
