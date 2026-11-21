package org.hhn.topicgrouper.util;

import junit.framework.TestCase;

public class UnionFindTest extends TestCase {
	public void testUnionAndFind() {
		UnionFind uf = new UnionFind(5);
		assertEquals(5, uf.count());
		uf.union(0, 1);
		uf.union(1, 2);
		assertEquals(3, uf.count());
		assertTrue(uf.connected(0, 2));
		assertFalse(uf.connected(0, 3));
		// The representative of the second argument wins.
		assertEquals(uf.find(2), uf.find(0));
		uf.union(0, 2);
		assertEquals(3, uf.count());
	}

	public void testInvalidIndex() {
		try {
			new UnionFind(2).find(2);
			fail();
		} catch (IndexOutOfBoundsException e) {
			// expected
		}
	}
}
