package com.pathfinding.algorithms.internal;

import java.util.Arrays;

/**
 * Tas binaire min indexé : variante du tas binaire classique qui ajoute deux
 * opérations cruciales pour A* et apparentés :
 *
 * <ul>
 *   <li>{@link #contains(int)} en O(1)</li>
 *   <li>{@link #decreaseKey(int, double)} en O(log n)</li>
 * </ul>
 *
 * <p>Avec un tas classique (java.util.PriorityQueue), quand un nœud est
 * relâché plusieurs fois on insère un nouveau record et on "lazy-deletes" les
 * anciens. Le tas grossit inutilement et chaque {@code poll()} doit ignorer
 * des entrées obsolètes. Avec decrease-key, on met à jour la priorité d'un
 * nœud sans l'insérer deux fois. Gain net pour les grilles avec voisinage
 * dense : moins d'objets, moins d'entrées dans le tas, accès O(log n).</p>
 *
 * <p>Pré-requis : chaque nœud doit posséder un identifiant entier unique dans
 * {@code [0, capacity)}. Pour une grille on utilise typiquement
 * {@code idx = x * height + y}.</p>
 */
public final class IndexedMinHeap {

    private final int[] heap;       // heap[i] = identifiant de nœud à la position i (1-indexé)
    private final int[] pos;        // pos[nodeIdx] = position dans le tas, -1 si absent
    private final double[] keys;    // keys[nodeIdx] = priorité courante
    private int size;

    public IndexedMinHeap(int capacity) {
        if (capacity < 1) throw new IllegalArgumentException("capacity >= 1");
        this.heap = new int[capacity + 1];   // index 0 inutilisé pour simplifier l'arithmétique parent/enfant
        this.pos  = new int[capacity];
        this.keys = new double[capacity];
        Arrays.fill(pos, -1);
    }

    public boolean isEmpty() { return size == 0; }
    public int size() { return size; }

    public boolean contains(int nodeIdx) { return pos[nodeIdx] >= 0; }

    /** Priorité enregistrée pour ce nœud (résultat indéfini s'il n'est pas dans le tas). */
    public double key(int nodeIdx) { return keys[nodeIdx]; }

    /** Insère un nouveau nœud avec sa priorité initiale. */
    public void insert(int nodeIdx, double key) {
        size++;
        heap[size] = nodeIdx;
        pos[nodeIdx] = size;
        keys[nodeIdx] = key;
        siftUp(size);
    }

    /** Diminue la priorité d'un nœud présent, et le remonte si nécessaire. */
    public void decreaseKey(int nodeIdx, double newKey) {
        keys[nodeIdx] = newKey;
        siftUp(pos[nodeIdx]);
    }

    /**
     * Insère le nœud s'il n'est pas présent, sinon diminue sa priorité si
     * {@code newKey < key actuelle}. Combine les deux opérations courantes
     * d'A*.
     */
    public void insertOrDecrease(int nodeIdx, double newKey) {
        if (pos[nodeIdx] < 0) {
            insert(nodeIdx, newKey);
        } else if (newKey < keys[nodeIdx]) {
            decreaseKey(nodeIdx, newKey);
        }
    }

    /** Retire et renvoie le nœud de priorité minimale. */
    public int extractMin() {
        int min = heap[1];
        swap(1, size);
        pos[min] = -1;
        size--;
        siftDown(1);
        return min;
    }

    // ---------- internes ----------

    private void siftUp(int i) {
        while (i > 1) {
            int parent = i >>> 1;
            if (keys[heap[parent]] <= keys[heap[i]]) break;
            swap(parent, i);
            i = parent;
        }
    }

    private void siftDown(int i) {
        while ((i << 1) <= size) {
            int child = i << 1;
            if (child < size && keys[heap[child + 1]] < keys[heap[child]]) child++;
            if (keys[heap[i]] <= keys[heap[child]]) break;
            swap(i, child);
            i = child;
        }
    }

    private void swap(int i, int j) {
        int ti = heap[i], tj = heap[j];
        heap[i] = tj;
        heap[j] = ti;
        pos[tj] = i;
        pos[ti] = j;
    }
}
