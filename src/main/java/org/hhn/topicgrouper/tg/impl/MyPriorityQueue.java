package org.hhn.topicgrouper.tg.impl;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.PriorityQueue;

// Workaround to get access to a private field and a private method of Java's priority queue.
// Its sad: Cannot drop in the source code of PriorityQueue because of GPL License of Open Java.
@SuppressWarnings("serial")
public class MyPriorityQueue<E> extends PriorityQueue<E> {
	private Method removeAtMethod;
	private Field queue;
	
	public MyPriorityQueue(int initialCapacity) {
		super(initialCapacity);
		try {
			removeAtMethod = PriorityQueue.class.getDeclaredMethod("removeAt", int.class);
			removeAtMethod.setAccessible(true);
			queue = PriorityQueue.class.getDeclaredField("queue");
			queue.setAccessible(true);
		} catch (NoSuchMethodException e) {
			throw new RuntimeException(e);
		} catch (SecurityException e) {
			throw new RuntimeException(e);
		} catch (NoSuchFieldException e) {
			throw new RuntimeException(e);			
		}
	}
	
	public E removeAt(int i) {
		E res;
		try {
			res = (E) removeAtMethod.invoke(this, i);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		} catch (IllegalArgumentException e) {
			throw new RuntimeException(e);
		} catch (InvocationTargetException e) {
			throw new RuntimeException(e);
		}
		return res;
	}
	
	public Object[] getQueue() {
		try {
			return (Object[]) queue.get(this);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		} catch (IllegalArgumentException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static void main(String[] args) {
		MyPriorityQueue<Integer> test = new MyPriorityQueue<>(2);
		test.add(1);
		test.add(2);
		System.out.println(test.getQueue());
		System.out.println(test.removeAt(1));
	}
}
