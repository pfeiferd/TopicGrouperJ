package org.hhn.topicgrouper.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public abstract class EvalRunner {
	protected abstract Class[] getMainClasses();

	public void run(int cores) {
		try {
			final PrintStream output = new PrintStream(new FileOutputStream(new File(
					"./target/" + getClass().getSimpleName() + ".txt")));
			final Class[] classes = getMainClasses();

			ThreadPoolExecutor executor = new ThreadPoolExecutor(cores, cores, 1,
					TimeUnit.SECONDS, new LinkedBlockingDeque<Runnable>());

			for (int i = 0; i < classes.length; i++) {
				Runnable runnable = new MyRunnable(classes[i], output);
				executor.execute(runnable);
			}
		} catch (FileNotFoundException e1) {
			throw new RuntimeException(e1);
		}
	}
	
	protected static class MyRunnable implements Runnable {
		private Class<?> clazz;
		private PrintStream output;
		
		public MyRunnable(Class<?> clazz, PrintStream output) {
			this.clazz = clazz;
			this.output = output;
		}
		
		@Override
		public void run() {
			long start = System.currentTimeMillis();
			output.println("Starting class " + clazz.getName() + " at "
					+ start + " ms.");
			boolean mainFound = false;
			for (Method method : clazz.getMethods()) {
				if (method.getName().equals("main")) {
					mainFound = true;
					try {
						method.invoke(null, (Object) new String[0]);
					} catch (IllegalAccessException e) {
						e.printStackTrace(output);
					} catch (IllegalArgumentException e) {
						e.printStackTrace(output);
					} catch (InvocationTargetException e) {
						e.printStackTrace(output);
					}
				}
			}
			if (!mainFound) {
				output.println("Cannot start class - no main method found.");
			}
			output.println("Class execution terminated after "
					+ (System.currentTimeMillis() - start) + "ms.");
			output.println();
		}
	}
}
