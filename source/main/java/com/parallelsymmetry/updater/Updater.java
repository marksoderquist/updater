package com.parallelsymmetry.updater;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.URI;
import java.nio.charset.Charset;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.FileHandler;

import com.parallelsymmetry.utility.Descriptor;
import com.parallelsymmetry.utility.IoUtil;
import com.parallelsymmetry.utility.JavaUtil;
import com.parallelsymmetry.utility.OperatingSystem;
import com.parallelsymmetry.utility.Parameters;
import com.parallelsymmetry.utility.TextUtil;
import com.parallelsymmetry.utility.ThreadUtil;
import com.parallelsymmetry.utility.log.DefaultFormatter;
import com.parallelsymmetry.utility.log.Log;
import com.parallelsymmetry.utility.log.LogFlag;
import com.parallelsymmetry.utility.product.Product;
import com.parallelsymmetry.utility.product.ProductCard;

/**
 * The Updater class is the entry point for the Updater application.
 * <p>
 * The updater does not need to be started as an elevated process. If there is a
 * need to have elevated privileges to perform any update tasks a new process
 * will be started with elevated privileges to perform those updates.
 * 
 * @author SoderquistMV
 */

public final class Updater implements Product {

	private Parameters parameters;

	private ProductCard card;

	private String logFilePattern;

	private List<UpdateTask> updateTasks;

	private List<LaunchTask> launchTasks;

	private boolean needsElevation;

	public Updater() {
		describe();
	}

	public static final void main( String[] commands ) {
		new Updater().call( commands );
	}

	@Override
	public ProductCard getCard() {
		return card;
	}

	@Override
	public File getDataFolder() {
		return OperatingSystem.getUserProgramDataFolder( card.getArtifact(), card.getName() );
	}

	public void call( String[] commands ) {
		try {
			try {
				parameters = Parameters.parse( commands );
				if( parameters.isSet( UpdaterFlag.STDIN ) ) parameters = Parameters.parse( IoUtil.loadAsLineArray( System.in, TextUtil.DEFAULT_ENCODING ) );
			} catch( InvalidParameterException exception ) {
				Log.write( Log.ERROR, exception.getMessage() );
				printHelp();
				return;
			}

			boolean isElevated = parameters.isTrue( UpdaterFlag.ELEVATED );

			if( !isElevated ) {
				Log.config( parameters );
				if( !parameters.isSet( LogFlag.LOG_FILE ) ) {
					try {
						File folder = getDataFolder();
						folder.mkdirs();

						logFilePattern = new File( folder, "updater.log" ).getCanonicalPath();
						FileHandler handler = new FileHandler( logFilePattern, parameters.isTrue( LogFlag.LOG_FILE_APPEND ) );
						handler.setLevel( Log.INFO );
						if( parameters.isSet( LogFlag.LOG_FILE_LEVEL ) ) handler.setLevel( Log.parseLevel( parameters.get( LogFlag.LOG_FILE_LEVEL ) ) );

						DefaultFormatter formatter = new DefaultFormatter();
						formatter.setShowDate( true );
						handler.setFormatter( formatter );
						Log.addHandler( handler );
					} catch( IOException exception ) {
						Log.write( exception );
					}
				}
			}

			describe();

			printHeader();

			if( !isElevated ) {
				if( parameters.size() == 0 || parameters.isTrue( UpdaterFlag.WHAT ) || parameters.isTrue( UpdaterFlag.HELP ) ) {
					printHelp();
					return;
				} else if( parameters.isTrue( UpdaterFlag.VERSION ) ) {
					printVersion();
					return;
				}
			}

			updateTasks = new ArrayList<UpdateTask>();
			if( parameters.isSet( UpdaterFlag.UPDATE ) ) {
				List<String> files = parameters.getValues( UpdaterFlag.UPDATE );

				try {
					int index = 0;
					int count = files.size();

					if( count == 0 || "true".equals( parameters.get( UpdaterFlag.UPDATE ) ) ) throw new IllegalArgumentException( "No update files specified." );

					while( index < count ) {
						String source = null;
						String target = null;
						if( index < count ) source = files.get( index );
						if( index + 1 < count ) target = files.get( index + 1 );
						if( source == null ) throw new IllegalArgumentException( "Source parameter not specified." );
						if( target == null ) throw new IllegalArgumentException( "Target parameter not specified." );
						FileUpdateTask task = new FileUpdateTask( new File( source ).getCanonicalFile(), new File( target ).getCanonicalFile() );
						updateTasks.add( task );
						needsElevation |= task.needsElevation();
						index += 2;
					}
				} catch( RuntimeException exception ) {
					Log.write( exception );
				}
			}

			if( !isElevated ) {
				launchTasks = new ArrayList<LaunchTask>();
				if( parameters.isSet( UpdaterFlag.LAUNCH ) ) {
					List<String> values = parameters.getValues( UpdaterFlag.LAUNCH );
					String workFolder = parameters.get( UpdaterFlag.LAUNCH_HOME );
					boolean launchElevated = parameters.isSet( UpdaterFlag.LAUNCH_ELEVATED );
					launchTasks.add( new ProcessLaunchTask( this, values, workFolder, launchElevated ) );
				}
			}

			process();
		} catch( Throwable throwable ) {
			Log.write( throwable );
		}
	}

	private void process() {
		if( needsElevation ) {
			updateElevated();
		} else {
			update();
		}
		launch();
	}

	private void updateElevated() {
		//Log.write( Log.DEVEL, "Launch updates with elevated privileges." );
		// Use current command parameters to start an elevated process.
		ProcessBuilder builder = new ProcessBuilder( OperatingSystem.isWindows() ? "javaw" : "java" );
		builder.directory( new File( System.getProperty( "user.dir" ) ) );

		// Add the VM parameters to the commands.
		RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
		for( String command : runtimeBean.getInputArguments() ) {
			if( !builder.command().contains( command ) ) builder.command().add( command );
		}

		// Add the classpath information.
		List<URI> classpath = JavaUtil.getClasspath();
		boolean jar = classpath.size() == 1 && classpath.get( 0 ).getPath().endsWith( ".jar" );
		if( jar ) {
			builder.command().add( "-jar" );
		} else {
			builder.command().add( "-cp" );
		}
		builder.command().add( runtimeBean.getClassPath() );
		if( !jar ) builder.command().add( getClass().getName() );

		// Add the STDIN flag to pass the parameters.
		builder.command().add( UpdaterFlag.STDIN );

		try {
			OperatingSystem.elevateProcessBuilder( getCard().getName(), builder );
			Process process = builder.start();
			PrintStream output = new PrintStream( process.getOutputStream() );
			output.println( UpdaterFlag.ELEVATED );
			output.println( UpdaterFlag.UPDATE );
			for( String value : parameters.getValues( UpdaterFlag.UPDATE ) ) {
				output.println( value );
			}
			output.close();
		} catch( IOException exception ) {
			Log.write( exception );
		}
	}

	private void update() {
		// Pause if an update delay is set.
		if( parameters.isSet( UpdaterFlag.UPDATE ) && parameters.isSet( UpdaterFlag.UPDATE_DELAY ) ) {
			String delayValue = parameters.get( UpdaterFlag.UPDATE_DELAY );
			Log.write( "Update delay: ", delayValue, "ms" );
			try {
				ThreadUtil.pause( Long.parseLong( delayValue ) );
			} catch( NumberFormatException exception ) {
				Log.write( exception );
			}
		}

		// Execute the update tasks.
		for( UpdateTask task : updateTasks ) {
			try {
				task.execute();
			} catch( Throwable throwable ) {
				Log.write( throwable );
			}
		}
	}

	private void launch() {
		// Pause if a launch delay is set.
		if( parameters.isSet( UpdaterFlag.LAUNCH_DELAY ) ) {
			String delayValue = parameters.get( UpdaterFlag.LAUNCH_DELAY );
			Log.write( "Launch delay: ", delayValue, "ms" );
			try {
				ThreadUtil.pause( Long.parseLong( delayValue ) );
			} catch( NumberFormatException exception ) {
				Log.write( exception );
			}
		}

		// Execute the launch tasks.
		for( LaunchTask task : launchTasks ) {
			try {
				task.execute();
			} catch( Throwable throwable ) {
				Log.write( throwable );
			}
		}
	}

	private void describe() {
		try {
			URI uri = getClass().getResource( "/META-INF/product.xml" ).toURI();
			card = new ProductCard( uri, new Descriptor( uri ) );
		} catch( Exception exception ) {
			throw new RuntimeException( exception );
		}
	}

	private void printHeader() {
		String summary = card.getLicenseSummary();

		Log.write( Log.HELP, TextUtil.pad( 75, '-' ) );
		Log.write( Log.HELP, card.getName() + " " + card.getRelease().getVersion().toHumanString() );
		Log.write( Log.HELP, card.getCopyright(), " ", card.getCopyrightNotice() );
		if( summary != null ) {
			Log.write( Log.HELP );
			Log.write( Log.HELP, TextUtil.reline( summary, 75 ) );
		}
		Log.write( Log.HELP, TextUtil.pad( 75, '-' ) );
		Log.write( Log.HELP );

		Log.write( Log.TRACE, "Java: " + System.getProperty( "java.runtime.version" ) );
		Log.write( Log.TRACE, "Log : ", logFilePattern );
	}

	private void printVersion() {
		Log.write( Log.HELP, "Version: " + card.getRelease().toString() );
		Log.write( Log.HELP, "Java version: " + System.getProperty( "java.version" ) );
		Log.write( Log.HELP, "Java home: " + System.getProperty( "java.home" ) );
		Log.write( Log.HELP, "Default locale: " + Locale.getDefault() + "  encoding: " + Charset.defaultCharset() );
		Log.write( Log.HELP, "OS name: " + OperatingSystem.getName() + "  version: " + OperatingSystem.getVersion() + "  arch: " + OperatingSystem.getSystemArchitecture() + "  family: " + OperatingSystem.getFamily() );
	}

	private void printHelp() {
		// ---------0--------1---------2---------3---------4---------5---------6---------7---------8
		// ---------12345678901234567890123456789012345678901234567890123456789012345678901234567890
		Log.write( Log.HELP, "Usage: java -jar <jar file name> [<option>...]" );
		Log.write( Log.HELP );
		Log.write( Log.HELP, "Commands:" );
		Log.write( Log.HELP, "  --update <file file>..." );
		Log.write( Log.HELP, "    Update files in pairs of two using the first as the source and the second" );
		Log.write( Log.HELP, "    as the target. If the launch parameter is specified then the launch" );
		Log.write( Log.HELP, "    commands are executed after the updates have been processed." );
		Log.write( Log.HELP, "  --launch command... [-launch.home folder]" );
		Log.write( Log.HELP );
		Log.write( Log.HELP, "Options:" );
		Log.write( Log.HELP, "  -help            Show help information." );
		Log.write( Log.HELP, "  -version         Show version and copyright information only." );
		Log.write( Log.HELP );
		Log.write( Log.HELP, "  -log.level <level>   Change the output log level. Levels are:" );
		Log.write( Log.HELP, "                       none, error, warn, info, trace, debug, all" );
		Log.write( Log.HELP, "  -log.tag             Use level tags in the console output." );
		Log.write( Log.HELP, "  -log.color           Use level colors in the console output." );
		Log.write( Log.HELP, "  -log.prefix          Use level prefixes in the console output." );
		Log.write( Log.HELP, "  -log.file <file>     Output log messages to the specified file." );
		Log.write( Log.HELP, "  -log.file.level      Same as log.level except in regardsd to the file." );
		Log.write( Log.HELP, "  -log.file.append     Append to the log file if file is used." );
	}

}
