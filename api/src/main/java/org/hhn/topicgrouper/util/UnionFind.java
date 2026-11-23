package org.hhn.topicgrouper.util;

/**
 * Union-find (disjoint set) structure over the integers 0 to N-1.
 * After <code>union(p, q)</code> the representative of the joint set is the one of <code>q</code>.
 */
public class UnionFind {
    private int[] parent;  // parent[i] = parent of i
    private int count;     // number of components

    /**
     * Creates the structure with N singleton sets.
     *
     * @param N the number of elements, each initially in its own set.
     */
    public UnionFind(int N) {
        parent = new int[N];
        count = N;
        for (int i = 0; i < N; i++) {
            parent[i] = i;
        }
    }

    /**
     * The current number of disjoint sets.
     *
     * @return the number of sets
     */
    public int count() {
        return count;
    }

    /**
     * Finds the representative of an element's set by following the parent links to the root.
     *
     * @param p the element
     * @return the representative of the set containing p.
     * @throws IndexOutOfBoundsException if p is not in 0 .. N-1
     */
    public int find(int p) {
        validate(p);
        while (p != parent[p])
            p = parent[p];
        return p;
    }

    // validate that p is a valid index
    private void validate(int p) {
        int N = parent.length;
        if (p < 0 || p >= N) {
            throw new IndexOutOfBoundsException("index " + p + " is not between 0 and " + (N-1));  
        }
    }
    
    /**
     * Whether p and q are in the same set.
     *
     * @param p an element
     * @param q another element
     * @return <code>true</code> if both have the same representative
     */
    public boolean connected(int p, int q) {
        return find(p) == find(q);
    }
    /**
     * Merges the sets containing p and q; the representative of q becomes the one of the joint set.
     *
     * @param p an element
     * @param q another element
     */
    public void union(int p, int q) {
        int rootP = find(p);
        int rootQ = find(q);
        if (rootP == rootQ) return;
        parent[rootP] = rootQ; 
        count--;
    }
}
