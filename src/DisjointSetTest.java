/**
 * Checks of Mission 0. Run:  java -cp out DisjointSetTest
 * Add your own checks below: a large chain, a union of a set with itself, count after many unions.
 */
public class DisjointSetTest {

    static int failed = 0;

    static void check(boolean ok, String what) {
        System.out.println((ok ? "ok    " : "FAIL  ") + what);
        if (!ok) failed++;
    }

    public static void main(String[] args) {
        DisjointSet d = new DisjointSet(6);
        check(d.count() == 6, "6 sets at the start");
        check(!d.connected(0, 1), "0 and 1 are apart at the start");
        check(d.union(0, 1), "union(0, 1) joins two sets");
        check(d.connected(0, 1), "0 and 1 are together");
        check(!d.union(1, 0), "union(1, 0) again returns false");
        check(d.count() == 5, "5 sets after one union");
        d.union(2, 3); d.union(1, 3);
        check(d.connected(0, 2), "0 and 2 are together through 1 and 3");
        check(d.count() == 3, "3 sets: {0,1,2,3} {4} {5}");
        check(d.find(4) == 4 && d.find(5) == 5, "a single cell is its own root");

        check(!d.union(2, 2), "union with itself returns false");
        check(d.count() == 3, "self-union does not change count");
        d.union(4, 5);
        d.union(0, 4);
        check(d.count() == 1, "all six cells form one set");
        check(!d.union(5, 2) && d.count() == 1, "repeated union keeps one set");

        DisjointSet bySize = new DisjointSet(5);
        bySize.union(0, 1);
        bySize.union(0, 2);
        bySize.union(3, 4);
        int largerRoot = bySize.find(0);
        bySize.union(3, 0);
        check(bySize.find(3) == largerRoot, "smaller set attaches to larger set");

        DisjointSet single = new DisjointSet(1);
        check(single.count() == 1 && single.find(0) == 0, "one cell is one set");
        check(!single.union(0, 0), "single cell cannot be joined to itself again");
        check(new DisjointSet(0).count() == 0, "empty DSU has zero sets");

        int n = 100_000;
        DisjointSet chain = new DisjointSet(n);
        boolean allJoined = true;
        for (int i = 1; i < n; i++) allJoined &= chain.union(i - 1, i);
        check(allJoined && chain.count() == 1, "99,999 successful unions leave one set");
        check(chain.connected(0, n - 1), "ends of a large chain are connected");
        boolean sameRoot = true;
        int root = chain.find(0);
        for (int i = 0; i < n; i++) sameRoot &= chain.find(i) == root;
        check(sameRoot, "every cell of a large chain has the same root");
        check(!chain.union(n - 1, 0) && chain.count() == 1,
                "joining a large set again leaves its count unchanged");

        System.out.println(failed == 0 ? "All checks passed." : failed + " checks failed.");
        if (failed != 0) throw new AssertionError(failed + " DSU checks failed");
    }
}
