package com.parallelsymmetry.updater;

public interface UpdateTask {
	
	public boolean needsElevation();

	public void execute() throws Throwable;
	
}
