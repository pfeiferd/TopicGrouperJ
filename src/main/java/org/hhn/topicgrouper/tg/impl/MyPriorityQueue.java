package org.hhn.topicgrouper.tg.impl;

import java.util.AbstractQueue;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * A binary min-heap that, in contrast to {@link java.util.PriorityQueue},
 * allows to remove an element at a given heap position and exposes its internal
 * array. (An earlier version accessed these internals of
 * <code>PriorityQueue</code> via reflection, which fails on Java 16+.)
 * 
 * @param <E>
 *            the type of the elements
 */
public class MyPriorityQueue<E extends Comparable<? super E>> extends AbstractQueue<E> {
	private Object[] queue;
	private int size;

	/**
	 * @param initialCapacity
	 *            the initial size of the internal array (values below 1 are
	 *            treated as 1)
	 */
	public MyPriorityQueue(int initialCapacity) {
		queue = new Object[Math.max(1, initialCapacity)];
	}

	@Override
	public boolean offer(E e) {
		if (e == null) {
			throw new NullPointerException();
		}
		if (size == queue.length) {
			queue = Arrays.copyOf(queue, size * 2);
		}
		queue[size] = e;
		siftUp(size);
		size++;
		return true;
	}

	@SuppressWarnings("unchecked")
	@Override
	public E peek() {
		return size == 0 ? null : (E) queue[0];
	}

	@Override
	public E poll() {
		return size == 0 ? null : removeAt(0);
	}

	@Override
	public int size() {
		return size;
	}

	@Override
	public void clear() {
		Arrays.fill(queue, 0, size, null);
		size = 0;
	}

	@Override
	public Iterator<E> iterator() {
		return new Iterator<E>() {
			private int next = 0;

			public boolean hasNext() {
				return next < size;
			}

			@SuppressWarnings("unchecked")
			public E next() {
				if (next >= size) {
					throw new NoSuchElementException();
				}
				return (E) queue[next++];
			}
		};
	}

	/**
	 * Removes the element at the given position of the internal heap array.
	 * 
	 * @return the removed element
	 */
	@SuppressWarnings("unchecked")
	public E removeAt(int i) {
		if (i < 0 || i >= size) {
			throw new IndexOutOfBoundsException(String.valueOf(i));
		}
		E removed = (E) queue[i];
		size--;
		E last = (E) queue[size];
		queue[size] = null;
		if (i != size) {
			queue[i] = last;
			siftDown(i);
			if (queue[i] == last) {
				siftUp(i);
			}
		}
		return removed;
	}

	/**
	 * @return the internal heap array (only the first {@link #size()} entries
	 *         are valid).
	 */
	public Object[] getQueue() {
		return queue;
	}

	@SuppressWarnings("unchecked")
	private void siftUp(int k) {
		E x = (E) queue[k];
		while (k > 0) {
			int parent = (k - 1) >>> 1;
			E p = (E) queue[parent];
			if (x.compareTo(p) >= 0) {
				break;
			}
			queue[k] = p;
			k = parent;
		}
		queue[k] = x;
	}

	@SuppressWarnings("unchecked")
	private void siftDown(int k) {
		E x = (E) queue[k];
		int half = size >>> 1;
		while (k < half) {
			int child = 2 * k + 1;
			E c = (E) queue[child];
			int right = child + 1;
			if (right < size && c.compareTo((E) queue[right]) > 0) {
				c = (E) queue[child = right];
			}
			if (x.compareTo(c) <= 0) {
				break;
			}
			queue[k] = c;
			k = child;
		}
		queue[k] = x;
	}
}
