package org.hhn.topicgrouper.tg.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import junit.framework.TestCase;

/**
 * Tests the heap: ordering, removal at a position, linear time construction and
 * the position hook.
 */
public class MyPriorityQueueTest extends TestCase {
	/** An element that remembers its heap position via the hook. */
	private static class Item implements Comparable<Item> {
		final int key;
		int pos = -1;

		Item(int key) {
			this.key = key;
		}

		public int compareTo(Item o) {
			return key < o.key ? -1 : (key == o.key ? 0 : 1);
		}
	}

	private static class TrackingQueue extends MyPriorityQueue<Item> {
		TrackingQueue(int capacity) {
			super(capacity);
		}

		TrackingQueue(Item[] items, int size) {
			super(items, size);
		}

		@Override
		protected void moved(Item e, int index) {
			e.pos = index;
		}

		/** Every element's remembered position must be where it actually is. */
		void assertPositions() {
			Object[] q = getQueue();
			for (int i = 0; i < size(); i++) {
				assertEquals(i, ((Item) q[i]).pos);
			}
			for (int i = 1; i < size(); i++) {
				assertTrue(((Item) q[(i - 1) >>> 1]).key <= ((Item) q[i]).key);
			}
		}
	}

	public void testOfferPollOrder() {
		TrackingQueue q = new TrackingQueue(2);
		Random random = new Random(1);
		List<Integer> keys = new ArrayList<Integer>();
		for (int i = 0; i < 200; i++) {
			int k = random.nextInt(50);
			keys.add(k);
			q.offer(new Item(k));
			q.assertPositions();
		}
		Collections.sort(keys);
		for (int k : keys) {
			assertEquals(k, q.peek().key);
			assertEquals(k, q.poll().key);
			q.assertPositions();
		}
		assertNull(q.poll());
		assertNull(q.peek());
		assertEquals(0, q.size());
	}

	public void testRemoveAtByRememberedPosition() {
		TrackingQueue q = new TrackingQueue(4);
		List<Item> items = new ArrayList<Item>();
		Random random = new Random(2);
		for (int i = 0; i < 100; i++) {
			Item it = new Item(random.nextInt(30));
			items.add(it);
			q.offer(it);
		}
		Collections.shuffle(items, random);
		for (Item it : items) {
			assertSame(it, q.removeAt(it.pos));
			q.assertPositions();
		}
		assertEquals(0, q.size());
		try {
			q.removeAt(0);
			fail();
		} catch (IndexOutOfBoundsException e) {
			// expected
		}
	}

	public void testHeapifyConstructor() {
		Random random = new Random(3);
		Item[] items = new Item[301];
		for (int i = 0; i < 300; i++) {
			items[i] = new Item(random.nextInt(100));
		}
		TrackingQueue q = new TrackingQueue(items, 300);
		assertEquals(300, q.size());
		assertSame(items, q.getQueue());
		q.assertPositions();
		int last = -1;
		while (q.size() > 0) {
			int k = q.poll().key;
			assertTrue(k >= last);
			last = k;
		}
		assertEquals(0, new TrackingQueue(new Item[0], 0).size());
		try {
			new TrackingQueue(new Item[2], 3);
			fail();
		} catch (IllegalArgumentException e) {
			// expected
		}
	}

	public void testIteratorAndClear() {
		TrackingQueue q = new TrackingQueue(1);
		for (int i = 5; i >= 0; i--) {
			q.offer(new Item(i));
		}
		int count = 0;
		for (Item it : q) {
			assertNotNull(it);
			count++;
		}
		assertEquals(6, count);
		q.clear();
		assertEquals(0, q.size());
		assertFalse(q.iterator().hasNext());
		try {
			q.offer(null);
			fail();
		} catch (NullPointerException e) {
			// expected
		}
	}
}
