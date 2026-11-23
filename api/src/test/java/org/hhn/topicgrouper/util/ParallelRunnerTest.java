package org.hhn.topicgrouper.util;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;

import junit.framework.TestCase;

/**
 * Tests the loop runner: coverage of the range, parallel execution, the single
 * threaded path and exception propagation.
 */
public class ParallelRunnerTest extends TestCase {
	public void testCoversEveryIndexOnce() {
		for (int threads : new int[] { 1, 2, 4 }) {
			ParallelRunner runner = new ParallelRunner(threads);
			try {
				assertEquals(threads, runner.getThreads());
				for (int n : new int[] { 0, 1, 5, 100, 1001 }) {
					final AtomicIntegerArray hits = new AtomicIntegerArray(n);
					final AtomicInteger calls = new AtomicInteger();
					runner.run(n, new ParallelRunner.RangeTask() {
						public void run(int from, int to) {
							assertTrue(from < to);
							calls.incrementAndGet();
							for (int i = from; i < to; i++) {
								hits.incrementAndGet(i);
							}
						}
					});
					for (int i = 0; i < n; i++) {
						assertEquals(1, hits.get(i));
					}
					if (n == 0) {
						assertEquals(0, calls.get());
					}
				}
			} finally {
				runner.close();
			}
		}
	}

	public void testSeveralThreadsTakePart() {
		ParallelRunner runner = new ParallelRunner(4);
		try {
			final java.util.Set<String> names = java.util.Collections
					.synchronizedSet(new java.util.HashSet<String>());
			runner.run(100000, 10, new ParallelRunner.RangeTask() {
				public void run(int from, int to) {
					names.add(Thread.currentThread().getName());
					double x = 0;
					for (int i = from; i < to; i++) {
						x += Math.sqrt(i);
					}
					assertTrue(x >= 0);
				}
			});
			assertTrue("threads used: " + names, names.size() > 1);
			assertTrue(names.contains(Thread.currentThread().getName()));
		} finally {
			runner.close();
		}
	}

	public void testExceptionsArePropagated() {
		ParallelRunner runner = new ParallelRunner(3);
		try {
			try {
				runner.run(1000, 1, new ParallelRunner.RangeTask() {
					public void run(int from, int to) {
						if (from == 500) {
							throw new IllegalStateException("boom");
						}
					}
				});
				fail();
			} catch (IllegalStateException e) {
				assertEquals("boom", e.getMessage());
			}
			// The runner is still usable afterwards.
			final AtomicInteger sum = new AtomicInteger();
			runner.run(10, new ParallelRunner.RangeTask() {
				public void run(int from, int to) {
					sum.addAndGet(to - from);
				}
			});
			assertEquals(10, sum.get());
		} finally {
			runner.close();
		}
	}

	public void testValuesBelowOneMeanOneThread() {
		ParallelRunner runner = new ParallelRunner(0);
		assertEquals(1, runner.getThreads());
		runner.close();
	}
}
