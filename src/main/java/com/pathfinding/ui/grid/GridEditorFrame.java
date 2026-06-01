package com.pathfinding.ui.grid;

import com.pathfinding.api.PathRequest;
import com.pathfinding.api.PathResult;
import com.pathfinding.api.Pathfinder;
import com.pathfinding.benchmark.Benchmark;
import com.pathfinding.benchmark.BenchmarkResult;
import com.pathfinding.ui.MainMenu;
import com.pathfinding.ui.shared.AlgorithmRegistry;
import com.pathfinding.ui.shared.ResultsCard;
import com.pathfinding.ui.shared.SidebarSection;
import com.pathfinding.ui.theme.Card;
import com.pathfinding.ui.theme.PillButton;
import com.pathfinding.ui.theme.Screen;
import com.pathfinding.ui.theme.SegmentedControl;
import com.pathfinding.ui.theme.Theme;
import com.pathfinding.world.GridWorld;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.ScrollPaneConstants;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.List;
import java.util.Map;

/**
 * Fenêtre principale du mode "grille discrète" : canevas à gauche, barre
 * latérale d'options à droite, carte de résultats sous la sidebar.
 */
public final class GridEditorFrame extends JFrame {

    private GridWorld world;
    private final GridCanvas canvas;
    private final ResultsCard results = new ResultsCard();
    private final JLabel statusLabel = new JLabel(" ");

    // Contrôles sidebar
    private final JComboBox<AlgorithmRegistry.Entry> algoCombo;
    private final JSpinner widthSpinner;
    private final JSpinner heightSpinner;
    private final JSpinner runsSpinner = new JSpinner(new SpinnerNumberModel(5, 1, 200, 1));

    public GridEditorFrame() {
        super("Pathfinding — Mode grille");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        world = new GridWorld(40, 25);
        world.setStart(2, 12);
        world.setGoal(37, 12);

        canvas = new GridCanvas(world);
        canvas.setStatusListener(s -> statusLabel.setText("  " + s));

        algoCombo = new JComboBox<>(AlgorithmRegistry.forGrid().toArray(new AlgorithmRegistry.Entry[0]));
        algoCombo.setRenderer(new AlgorithmCellRenderer());
        widthSpinner  = new JSpinner(new SpinnerNumberModel(world.gridWidth(),  5, 500, 1));
        heightSpinner = new JSpinner(new SpinnerNumberModel(world.gridHeight(), 5, 500, 1));

        getContentPane().setBackground(Theme.BG_WINDOW);
        ((JPanel) getContentPane()).setBorder(BorderFactory.createEmptyBorder(
                Theme.PADDING_MD, Theme.PADDING_MD, Theme.PADDING_MD, Theme.PADDING_MD));
        setLayout(new BorderLayout(Theme.PADDING_MD, Theme.PADDING_MD));

        add(buildTopBar(), BorderLayout.NORTH);
        add(buildCenter(), BorderLayout.CENTER);
        add(buildSidebar(), BorderLayout.EAST);

        // Dimensionnement adaptatif à l'écran (et plancher pour petits affichages)
        setSize(Screen.editorSize());
        setMinimumSize(Screen.editorMin());
        setLocationRelativeTo(null);
    }

    // ===================== UI building =====================

    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);

        PillButton back = new PillButton("← Retour", PillButton.Style.GHOST);
        back.addActionListener(e -> { dispose(); MainMenu.open(); });
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        left.setOpaque(false);
        left.add(back);
        bar.add(left, BorderLayout.WEST);

        JLabel title = new JLabel("Mode grille");
        title.setFont(Theme.FONT_TITLE);
        title.setForeground(Theme.TEXT_PRIMARY);
        bar.add(title, BorderLayout.CENTER);
        title.setHorizontalAlignment(JLabel.CENTER);

        statusLabel.setFont(Theme.FONT_CAPTION);
        statusLabel.setForeground(Theme.TEXT_SECONDARY);
        bar.add(statusLabel, BorderLayout.EAST);

        return bar;
    }

    private Card buildCenter() {
        Card card = new Card(new BorderLayout());
        JScrollPane scroll = new JScrollPane(canvas,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Theme.CANVAS_BG);
        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildSidebar() {
        JPanel column = new JPanel(new BorderLayout(0, Theme.PADDING_MD));
        column.setOpaque(false);
        // Largeur fixée pour la sidebar, hauteur héritée du frame via BorderLayout.EAST.
        column.setPreferredSize(new Dimension(Screen.sidebarWidth(), 100));

        // Carte des contrôles, contenu vertical
        Card controls = new Card();
        controls.setLayout(new BoxLayout(controls, BoxLayout.Y_AXIS));

        // -- section Édition
        SegmentedControl modeControl = new SegmentedControl(
                List.of("Départ", "Arrivée", "Obstacle", "Effacer"), 2);
        modeControl.onChange(i -> canvas.setMode(switch (i) {
            case 0 -> GridCanvas.EditMode.START;
            case 1 -> GridCanvas.EditMode.GOAL;
            case 2 -> GridCanvas.EditMode.OBSTACLE;
            default -> GridCanvas.EditMode.ERASE;
        }));
        controls.add(new SidebarSection("Édition").row(modeControl));

        // -- section Grille
        JPanel sizeRow = new JPanel(new GridLayout(1, 4, 6, 0));
        sizeRow.setOpaque(false);
        sizeRow.add(new JLabel("L"));
        sizeRow.add(widthSpinner);
        sizeRow.add(new JLabel("H"));
        sizeRow.add(heightSpinner);

        PillButton resize = new PillButton("Redimensionner", PillButton.Style.SECONDARY);
        resize.addActionListener(e -> {
            int w = (int) widthSpinner.getValue();
            int h = (int) heightSpinner.getValue();
            GridWorld ng = new GridWorld(w, h);
            int sx = Math.min(w / 10, w - 1);
            int sy = h / 2;
            int gx = Math.min(w - 1 - w / 10, w - 1);
            int gy = h / 2;
            ng.setStart(sx, sy);
            ng.setGoal(gx, gy);
            world = ng;
            canvas.setWorld(world);
            results.clear();
        });

        JSlider zoomSlider = new JSlider(10, 50, canvas.cellSize());
        zoomSlider.addChangeListener(e -> canvas.setCellSize(zoomSlider.getValue()));

        JCheckBox showExploredBox = new JCheckBox("Afficher les cases parcourues");
        showExploredBox.setToolTipText(
                "Cases visitées par la recherche qui ne font pas partie du trajet final.");
        showExploredBox.addActionListener(e -> canvas.setShowExplored(showExploredBox.isSelected()));

        controls.add(new SidebarSection("Grille & affichage")
                .row(sizeRow)
                .row(resize)
                .row(new JLabel("Zoom"))
                .row(zoomSlider)
                .row(showExploredBox));

        PillButton clearObs = new PillButton("Effacer obstacles", PillButton.Style.SECONDARY);
        clearObs.addActionListener(e -> { world.clearObstacles(); canvas.clearPath(); canvas.repaint(); });
        PillButton clearAll = new PillButton("Tout effacer", PillButton.Style.SECONDARY);
        clearAll.addActionListener(e -> { world.clearAll(); canvas.clearPath(); canvas.repaint(); });
        controls.add(new SidebarSection("Nettoyage")
                .row(clearObs)
                .row(clearAll));

        // -- section Algorithme
        controls.add(new SidebarSection("Algorithme").row(algoCombo));

        // -- section Exécution
        PillButton runBtn = new PillButton("Lancer", PillButton.Style.PRIMARY);
        runBtn.addActionListener(e -> runAlgorithm());

        JPanel benchRow = new JPanel(new BorderLayout(8, 0));
        benchRow.setOpaque(false);
        benchRow.add(new JLabel("Runs"), BorderLayout.WEST);
        benchRow.add(runsSpinner, BorderLayout.CENTER);

        PillButton benchBtn = new PillButton("Benchmark tous", PillButton.Style.SECONDARY);
        benchBtn.addActionListener(e -> runBenchmark());

        controls.add(new SidebarSection("Exécution")
                .row(runBtn)
                .gap(4)
                .row(benchRow)
                .row(benchBtn));

        controls.add(Box.createVerticalGlue());

        // Sidebar = scroll vertical sur les contrôles + résultats fixés en bas.
        // Garantit que sur un écran court, la sidebar reste utilisable.
        JScrollPane controlScroll = new JScrollPane(controls,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        controlScroll.setBorder(BorderFactory.createEmptyBorder());
        controlScroll.setOpaque(false);
        controlScroll.getViewport().setOpaque(false);
        controlScroll.getVerticalScrollBar().setUnitIncrement(16);

        column.add(controlScroll, BorderLayout.CENTER);
        column.add(results, BorderLayout.SOUTH);
        return column;
    }

    // ===================== Actions =====================

    private void runAlgorithm() {
        if (world.startCellX() < 0 || world.goalCellX() < 0) {
            JOptionPane.showMessageDialog(this, "Place un départ et une arrivée.",
                    "Manque", JOptionPane.WARNING_MESSAGE);
            return;
        }
        AlgorithmRegistry.Entry entry = (AlgorithmRegistry.Entry) algoCombo.getSelectedItem();
        if (entry == null) return;
        Pathfinder pf = entry.build();
        PathRequest req = new PathRequest(world, System.nanoTime());
        statusLabel.setText("  Exécution " + pf.name() + "…");
        new SwingWorker<PathResult, Void>() {
            @Override protected PathResult doInBackground() { return pf.find(req); }
            @Override protected void done() {
                try {
                    PathResult r = get();
                    canvas.setPath(r.success() ? r.path() : null);
                    canvas.setExplored(r.explored());
                    results.showRun(r);
                    statusLabel.setText("  " + (r.success() ?
                            String.format("OK · %.2f ms · long. %.2f · virage %.0f°",
                                    r.elapsedMillis(), r.pathLength(), r.totalTurnDegrees())
                            : "Échec: " + r.failureReason()));
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(GridEditorFrame.this, ex.toString(),
                            "Erreur", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void runBenchmark() {
        if (world.startCellX() < 0 || world.goalCellX() < 0) {
            JOptionPane.showMessageDialog(this, "Place un départ et une arrivée.",
                    "Manque", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int runs = (int) runsSpinner.getValue();
        PathRequest req = new PathRequest(world, System.nanoTime());
        statusLabel.setText("  Benchmark · " + runs + " runs/algo…");
        new SwingWorker<Map<String, BenchmarkResult>, Void>() {
            @Override
            protected Map<String, BenchmarkResult> doInBackground() {
                Benchmark b = new Benchmark().runs(runs);
                for (AlgorithmRegistry.Entry e : AlgorithmRegistry.forGrid()) b.register(e.build());
                return b.run(req);
            }
            @Override protected void done() {
                try {
                    Map<String, BenchmarkResult> res = get();
                    results.showBenchmark(res);
                    AlgorithmRegistry.Entry sel = (AlgorithmRegistry.Entry) algoCombo.getSelectedItem();
                    if (sel != null) {
                        BenchmarkResult r = res.get(sel.build().name());
                        if (r != null && r.lastResult() != null) {
                            canvas.setPath(r.lastResult().path());
                            canvas.setExplored(r.lastResult().explored());
                        }
                    }
                    statusLabel.setText("  Benchmark terminé (" + res.size() + " algos).");
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(GridEditorFrame.this, ex.toString(),
                            "Erreur", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    // Renderer pour afficher entry.name() dans le combo
    private static final class AlgorithmCellRenderer extends javax.swing.DefaultListCellRenderer {
        @Override
        public java.awt.Component getListCellRendererComponent(
                javax.swing.JList<?> list, Object value, int index,
                boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof AlgorithmRegistry.Entry e) setText(e.name());
            return this;
        }
    }
}
