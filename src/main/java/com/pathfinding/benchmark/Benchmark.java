package com.pathfinding.benchmark;

import com.pathfinding.api.PathRequest;
import com.pathfinding.api.PathResult;
import com.pathfinding.api.Pathfinder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Harnais de benchmark : exécute chaque algorithme enregistré N fois et
 * agrège les statistiques. Le premier run est traité comme un échauffement
 * JIT et écarté de la moyenne.
 *
 * <p>Le harnais ne connaît que l'interface {@link Pathfinder} et le type
 * {@link PathRequest} : aucun couplage avec l'UI ni avec un type de monde
 * particulier.</p>
 */
public final class Benchmark {

    private final List<Pathfinder> algorithms = new ArrayList<>();
    private int runsPerAlgorithm = 5;
    private boolean discardFirst = true;

    public Benchmark register(Pathfinder pf) {
        algorithms.add(pf);
        return this;
    }

    public Benchmark runs(int n) {
        if (n < 1) throw new IllegalArgumentException("runs >= 1");
        this.runsPerAlgorithm = n;
        return this;
    }

    public Benchmark discardFirst(boolean v) {
        this.discardFirst = v;
        return this;
    }

    public Map<String, BenchmarkResult> run(PathRequest request) {
        Map<String, BenchmarkResult> out = new LinkedHashMap<>();
        for (Pathfinder pf : algorithms) {
            List<PathResult> kept = new ArrayList<>(runsPerAlgorithm);
            for (int i = 0; i < runsPerAlgorithm; i++) {
                PathResult r = pf.find(request);
                if (discardFirst && i == 0 && runsPerAlgorithm > 1) continue;
                kept.add(r);
            }
            out.put(pf.name(), BenchmarkResult.of(pf.name(), kept));
        }
        return out;
    }

    public static String formatTable(Map<String, BenchmarkResult> results) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format(
                "%-26s %4s %8s %8s %8s %8s %8s %8s %8s %8s %8s%n",
                "Algorithme", "OK",
                "avg ms", "min ms", "max ms", "σ ms",
                "expand", "gen", "openMx",
                "long.", "tour°"));
        sb.append("-".repeat(118)).append('\n');
        for (BenchmarkResult r : results.values()) {
            sb.append(String.format(
                    "%-26s %4s %8.3f %8.3f %8.3f %8.3f %8d %8d %8d %8.2f %8.1f%n",
                    truncate(r.algorithmName(), 26),
                    r.success() ? "oui" : "non",
                    r.avgMillis(), r.minMillis(), r.maxMillis(), r.stdDevMillis(),
                    r.nodesExpanded(), r.nodesGenerated(), r.maxOpenSize(),
                    r.pathLength(), r.totalTurnDegrees()));
        }
        return sb.toString();
    }

    private static String truncate(String s, int len) {
        if (s == null) return "";
        return s.length() <= len ? s : s.substring(0, len);
    }
}
