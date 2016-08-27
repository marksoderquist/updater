package com.parallelsymmetry.updater;

public interface UpdateTask {

	boolean needsElevation();

	void execute() throws Throwable;

}
