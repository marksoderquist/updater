package com.parallelsymmetry.updater;

import com.parallelsymmetry.utility.TextUtil;
import com.parallelsymmetry.utility.log.Log;

import java.io.File;
import java.util.List;

public class ProcessLaunchTask implements LaunchTask {

	private List<String> values;

	private String workFolder;

	public ProcessLaunchTask( List<String> commands, String folder ) {
		this.values = commands;
		this.workFolder = folder;
	}

	public void execute() throws Throwable {
		ProcessBuilder builder = new ProcessBuilder( values );
		if( workFolder != null ) builder.directory( new File( workFolder ) );

		Log.write( Log.INFO, "Launching program: " + TextUtil.toString( builder.command(), " " ) );
		builder.start();
	}

}
