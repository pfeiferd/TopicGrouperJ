package org.hhn.topicgrouper.util;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Runs index ranges of a loop on a fixed set of worker threads that are kept
 * ready between calls (the calling thread works as well, so one thread means
 * no extra threads at all). The ranges are handed out dynamically in chunks,
 * which balances uneven work. {@link #run(int, RangeTask)} returns when the
 * whole range is done; everything the tasks wrote is then visible to the
 * caller. Exceptions of the tasks are rethrown by the caller.
 */
public class ParallelRunner implements Closeable {
	/**
	 * A piece of work over an index range.
	 */
	public interface RangeTask {
		/**
		 * Processes the indices <code>from</code> (inclusive) to <code>to</code>
		 * (exclusive). Called concurrently for disjoint ranges.
		 *
		 * @param from
		 *            the first index
		 * @param to
		 *            the index after the last one
		 */
		public void run(int from, int to);
	}

	private final int threads;
	private final ExecutorService pool;

	/**
	 * Creates the runner and its worker threads (daemon threads named
	 * <code>topic-grouper-N</code>).
	 *
	 * @param threads
	 *            the number of threads including the calling thread (values
	 *            below 1 are treated as 1)
	 */
	public ParallelRunner(int threads) {
		this.threads = Math.max(1, threads);
		if (this.threads > 1) {
			pool = Executors.newFixedThreadPool(this.threads - 1, new ThreadFactory() {
				private final AtomicInteger n = new AtomicInteger();

				public Thread newThread(Runnable r) {
					Thread t = new Thread(r, "topic-grouper-" + n.incrementAndGet());
					t.setDaemon(true);
					return t;
				}
			});
		} else {
			pool = null;
		}
	}

	/**
	 * The number of threads including the calling thread.
	 *
	 * @return the number of threads
	 */
	public int getThreads() {
		return threads;
	}

	/**
	 * Runs the task over <code>0 .. n - 1</code> with a chunk size derived from
	 * <code>n</code> and the number of threads.
	 *
	 * @param n
	 *            the number of indices
	 * @param task
	 *            the task
	 */
	public void run(int n, RangeTask task) {
		run(n, Math.max(1, Math.min(64, n / (threads * 16))), task);
	}

	/**
	 * Runs the task over <code>0 .. n - 1</code>.
	 *
	 * @param n
	 *            the number of indices
	 * @param chunk
	 *            the number of consecutive indices handed to a thread at a time
	 * @param task
	 *            the task
	 */
	public void run(final int n, int chunk, final RangeTask task) {
		if (n <= 0) {
			return;
		}
		final int c = Math.max(1, chunk);
		if (pool == null || n <= c) {
			task.run(0, n);
			return;
		}
		final AtomicInteger next = new AtomicInteger();
		Runnable worker = new Runnable() {
			public void run() {
				for (int from = next.getAndAdd(c); from < n; from = next.getAndAdd(c)) {
					task.run(from, Math.min(n, from + c));
				}
			}
		};
		List<Future<?>> futures = new ArrayList<Future<?>>(threads - 1);
		for (int i = 1; i < threads; i++) {
			futures.add(pool.submit(worker));
		}
		Throwable failure = null;
		try {
			worker.run();
		} catch (RuntimeException e) {
			failure = e;
		} catch (Error e) {
			failure = e;
		}
		for (Future<?> f : futures) {
			try {
				f.get();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new RuntimeException(e);
			} catch (ExecutionException e) {
				if (failure == null) {
					failure = e.getCause();
				}
			}
		}
		if (failure instanceof RuntimeException) {
			throw (RuntimeException) failure;
		}
		if (failure instanceof Error) {
			throw (Error) failure;
		}
		if (failure != null) {
			throw new RuntimeException(failure);
		}
	}

	/**
	 * Stops the worker threads; the runner must not be used afterwards.
	 */
	@Override
	public void close() {
		if (pool != null) {
			pool.shutdown();
		}
	}
}
