package com.parallelsymmetry.updater;

import java.util.Arrays;
import java.util.List;

public class ProcessLaunchTaskTest extends BaseTestCase {

	public void testProcessLaunchTask() throws Throwable {
		List<String> commands = Arrays.asList( new String[] { "java" } );
		ProcessLaunchTask task = new ProcessLaunchTask( commands, System.getProperty( "user.dir" ) );
		task.execute();
	}

}
