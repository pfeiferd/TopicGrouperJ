package org.hhn.topicgrouper.ldaimpl;


public interface LDASolutionListener {
	public void beforeInitialization();

	public void initalizing(int document);

	public void initialized();

	public void updatedSolution(int iteration);
	
	public void done();
	
}
