package com.parallelsymmetry.updater;

import java.io.File;
import java.util.List;

import com.parallelsymmetry.utility.OperatingSystem;
import com.parallelsymmetry.utility.TextUtil;
import com.parallelsymmetry.utility.log.Log;
import com.parallelsymmetry.utility.product.Product;

public class ProcessLaunchTask implements LaunchTask {

	private Product product;

	private List<String> values;

	private String workFolder;

	private boolean launchElevated;

	public ProcessLaunchTask( Product product, List<String> commands, String folder, boolean elevated ) {
		this.product = product;
		this.values = commands;
		this.workFolder = folder;
		this.launchElevated = elevated;
	}

	public void execute() throws Throwable {
		boolean processElevated = OperatingSystem.isProcessElevated();

		ProcessBuilder builder = null;

		if( launchElevated ) {
			if( processElevated ) {
				builder = new ProcessBuilder( values );
			} else {
				builder = OperatingSystem.elevateProcessBuilder( product.getCard().getName(), new ProcessBuilder( values ) );
			}
		} else {
			if( processElevated ) {
				builder = OperatingSystem.reduceProcessBuilder( new ProcessBuilder( values ) );
			} else {
				builder = new ProcessBuilder( values );
			}
		}

		if( workFolder != null ) builder.directory( new File( workFolder ) );
		Log.write( Log.INFO, "Launching: " + TextUtil.toString( builder.command(), " " ) );
		builder.start();
		Log.write( Log.TRACE, "Updater process started." );
	}

}
