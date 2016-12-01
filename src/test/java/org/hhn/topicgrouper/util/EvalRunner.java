package org.hhn.topicgrouper.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public abstract class EvalRunner {
	protected abstract Class[] getMainClasses();

	public void run() {
		try {
			PrintStream output = new PrintStream(new FileOutputStream(new File(
					"./target/" + getClass().getSimpleName() + ".txt")));
			Class[] classes = getMainClasses();

			for (Class<?> clazz : classes) {
				long start = System.currentTimeMillis();
				output.println("Starting class " + clazz.getName() + " at " +  start + " ms.");
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
				output.println("Class execution terminated after " + (System.currentTimeMillis() - start)  + "ms.");				
				output.println();
			}
		} catch (FileNotFoundException e1) {
			throw new RuntimeException(e1);
		}
	}
}
