package org.hhn.topicgrouper.tg.impl;

import java.util.AbstractQueue;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * A binary min-heap that, in contrast to {@link java.util.PriorityQueue},
 * allows to remove an element at a given heap position, exposes its internal
 * array, can be built from an array in linear time and tells subclasses where
 * an element is placed ({@link #moved(Comparable, int)}), which allows an
 * element to remember its position for a removal in logarithmic time. (An
 * earlier version accessed the internals of <code>PriorityQueue</code> via
 * reflection, which fails on Java 16+.)
 *
 * @param <E>
 *            the type of the elements
 */
public class MyPriorityQueue<E extends Comparable<? super E>> extends AbstractQueue<E> {
	private Object[] queue;
	private int size;

	/**
	 * Creates an empty queue.
	 *
	 * @param initialCapacity
	 *            the initial size of the internal array (values below 1 are
	 *            treated as 1)
	 */
	public MyPriorityQueue(int initialCapacity) {
		queue = new Object[Math.max(1, initialCapacity)];
	}

	/**
	 * Creates a queue over the given elements in linear time; the array becomes
	 * the internal array (it is not copied and must have at least one entry).
	 *
	 * @param elements
	 *            the elements, in the first <code>size</code> entries
	 * @param size
	 *            the number of elements
	 */
	public MyPriorityQueue(E[] elements, int size) {
		if (size < 0 || size > elements.length) {
			throw new IllegalArgumentException("size out of range: " + size);
		}
		queue = elements.length == 0 ? new Object[1] : elements;
		this.size = size;
		for (int i = (size >>> 1) - 1; i >= 0; i--) {
			siftDown(i);
		}
		for (int i = 0; i < size; i++) {
			moved(element(i), i);
		}
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

	@Override
	public E peek() {
		return size == 0 ? null : element(0);
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

			public E next() {
				if (next >= size) {
					throw new NoSuchElementException();
				}
				return element(next++);
			}
		};
	}

	/**
	 * Removes the element at the given position of the internal heap array.
	 *
	 * @param i
	 *            the position (0 is the head)
	 * @return the removed element
	 */
	public E removeAt(int i) {
		if (i < 0 || i >= size) {
			throw new IndexOutOfBoundsException(String.valueOf(i));
		}
		E removed = element(i);
		size--;
		E last = element(size);
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
	 * Exposes the internal heap array for a direct scan of the elements.
	 *
	 * @return the internal heap array (only the first {@link #size()} entries
	 *         are valid).
	 */
	public Object[] getQueue() {
		return queue;
	}

	/**
	 * Called whenever an element is placed at a position of the heap array
	 * (including the initial placement). Does nothing by default.
	 *
	 * @param e
	 *            the element
	 * @param index
	 *            its new position
	 */
	protected void moved(E e, int index) {
	}

	@SuppressWarnings("unchecked")
	private E element(int i) {
		return (E) queue[i];
	}

	private void siftUp(int k) {
		E x = element(k);
		while (k > 0) {
			int parent = (k - 1) >>> 1;
			E p = element(parent);
			if (x.compareTo(p) >= 0) {
				break;
			}
			queue[k] = p;
			moved(p, k);
			k = parent;
		}
		queue[k] = x;
		moved(x, k);
	}

	private void siftDown(int k) {
		E x = element(k);
		int half = size >>> 1;
		while (k < half) {
			int child = 2 * k + 1;
			E c = element(child);
			int right = child + 1;
			if (right < size && c.compareTo(element(right)) > 0) {
				c = element(child = right);
			}
			if (x.compareTo(c) <= 0) {
				break;
			}
			queue[k] = c;
			moved(c, k);
			k = child;
		}
		queue[k] = x;
		moved(x, k);
	}
}
