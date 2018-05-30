package org.hhn.topicgrouper.util;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class OutputStreamMultiplexer extends OutputStream {
	private final List<OutputStream> streams;
	
	public OutputStreamMultiplexer() {
		this.streams = new ArrayList<OutputStream>();
	}
	
	public void addOutputStream(OutputStream stream) {
		streams.add(stream);
	}
	
	@Override
	public void write(int b) throws IOException {
		for (OutputStream s : streams) {
			s.write(b);
		}
	}
	
	@Override
	public void close() throws IOException {
		for (OutputStream s : streams) {
			s.close();
		}
	}
	
	@Override
	public void flush() throws IOException {
		for (OutputStream s : streams) {
			s.flush();;
		}
	}
}
