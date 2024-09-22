package com.parallelsymmetry.updater;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

public class ProcessLaunchTaskTest extends BaseTestCase {

	@Test
	public void testProcessLaunchTask() throws Throwable {
		List<String> commands = List.of( "java" );
		ProcessLaunchTask task = new ProcessLaunchTask( commands, System.getProperty( "user.dir" ) );
		task.execute();
	}

}
