package com.parallelsymmetry.updater;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.nio.charset.Charset;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.FileHandler;

import com.parallelsymmetry.utility.Descriptor;
import com.parallelsymmetry.utility.IoUtil;
import com.parallelsymmetry.utility.OperatingSystem;
import com.parallelsymmetry.utility.Parameters;
import com.parallelsymmetry.utility.TextUtil;
import com.parallelsymmetry.utility.ThreadUtil;
import com.parallelsymmetry.utility.log.DefaultFormatter;
import com.parallelsymmetry.utility.log.Log;
import com.parallelsymmetry.utility.log.LogFlag;
import com.parallelsymmetry.utility.product.Product;
import com.parallelsymmetry.utility.product.ProductCard;
import com.parallelsymmetry.utility.ui.SwingUtil;

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

	private static final String LOG_EXTENSION = ".log";

	private static final String ELEV_EXTENSION = ".elev";

	private Parameters parameters;

	private ProductCard card;

	private String logFilePattern;

	private List<UpdateTask> updateTasks;

	private List<LaunchTask> launchTasks;

	private boolean needsElevation;

	private ServerSocket server;

	private UpdaterWindow window;

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

			Log.config( parameters );
			if( parameters.isSet( LogFlag.LOG_FILE ) ) {
				logFilePattern = parameters.get( LogFlag.LOG_FILE );
			} else {
				try {
					File folder = getDataFolder();
					folder.mkdirs();

					StringBuilder pattern = new StringBuilder( folder.getCanonicalPath() );
					pattern.append( File.separatorChar );
					pattern.append( "updater.log" );

					logFilePattern = pattern.toString();

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

			describe();

			printHeader();

			boolean isElevated = parameters.isTrue( UpdaterFlag.ELEVATED );

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
						FileUpdateTask task = new FileUpdateTask( this, new File( source ).getCanonicalFile(), new File( target ).getCanonicalFile() );
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
					launchTasks.add( new ProcessLaunchTask( values, workFolder ) );
				}
			}

			if( parameters.isSet( UpdaterFlag.UI ) ) {
				window = new UpdaterWindow();
				if( parameters.isSet( UpdaterFlag.UI_MESSAGE ) ) window.setMessage( parameters.get( UpdaterFlag.UI_MESSAGE ) );
			}

			process();
		} catch( Throwable throwable ) {
			Log.write( throwable );
		}
	}

	public void incrementProgress() {
		if( window != null ) window.setProgress( window.getProgress() + 1 );
	}

	private void process() {
		if( window != null ) {
			window.setProgressMax( updateTasks.size() );
			window.setSize( 400, 50 );

			if( parameters.isSet( UpdaterFlag.UPDATE_DELAY ) ) window.setMessage( "Waiting for program to stop..." );

			SwingUtil.center( window );
			window.setVisible( true );
		}

		if( parameters.isSet( UpdaterFlag.ECHOPORT ) ) {
			String portString = parameters.get( UpdaterFlag.ECHOPORT );
			int port = -1;
			try {
				port = Integer.parseInt( portString );
			} catch( NumberFormatException exception ) {
				Log.write( exception );
			}
			if( port > 0 && port < 65536 ) waitForEchoStop( port );
		}

		try {
			if( needsElevation ) {
				int port = setupForCallback();
				updateElevated( port );
				waitForCallback( port );
				window.setProgress( updateTasks.size() );
			} else {
				update();
			}
			launch();
		} finally {
			if( window != null ) window.dispose();
		}
	}

	private void updateElevated( int port ) {
		// Use current command parameters to start an elevated process.
		ProcessBuilder builder = new ProcessBuilder( OperatingSystem.getJavaExecutableName() );
		builder.directory( new File( System.getProperty( "user.dir" ) ) );

		// Add the VM parameters to the commands.
		RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
		for( String command : runtimeBean.getInputArguments() ) {
			if( !builder.command().contains( command ) ) builder.command().add( command );
		}

		// Add the classpath information.
		builder.command().add( "-jar" );
		builder.command().add( runtimeBean.getClassPath() );

		// Set the log file.
		builder.command().add( LogFlag.LOG_FILE );
		builder.command().add( getElevatedLogFile() );

		// Add the callback port flag.
		builder.command().add( UpdaterFlag.CALLBACK );
		builder.command().add( String.valueOf( port ) );

		// Add the update delay flag.
		if( parameters.isSet( UpdaterFlag.UPDATE_DELAY ) ) {
			builder.command().add( UpdaterFlag.UPDATE_DELAY );
			builder.command().add( parameters.get( UpdaterFlag.UPDATE_DELAY ) );
		}

		// Add the updates.
		builder.command().add( UpdaterFlag.UPDATE );
		for( String value : parameters.getValues( UpdaterFlag.UPDATE ) ) {
			builder.command().add( value );
		}

		try {
			OperatingSystem.elevateProcessBuilder( getCard().getName(), builder );
			Log.write( Log.INFO, "Elevated update: " + TextUtil.toString( builder.command(), " " ) );
			Process process = builder.start();
			process.waitFor();
			Log.write( Log.INFO, "Elevated update complete." );
		} catch( InterruptedException exception ) {
			Log.write( exception );
		} catch( IOException exception ) {
			Log.write( exception );
		}
	}

	private String getElevatedLogFile() {
		File logFile = new File( logFilePattern );
		File folder = logFile.getParentFile();
		String name = logFile.getName();

		if( name.endsWith( LOG_EXTENSION ) ) {
			name = name.substring( 0, name.length() - LOG_EXTENSION.length() ) + ELEV_EXTENSION + LOG_EXTENSION;
		} else {
			name += ELEV_EXTENSION;
		}

		return new File( folder, name ).getAbsolutePath();
	}

	private int setupForCallback() {
		try {
			server = new ServerSocket( 0, 1, InetAddress.getLoopbackAddress() );
		} catch( IOException exception ) {
			Log.write( exception );
		}

		int port = server.getLocalPort();
		Log.write( Log.TRACE, "Set up for callback on port: ", port );
		return port;
	}

	private void waitForCallback( int port ) {
		try {
			server.accept();
		} catch( IOException exception ) {
			Log.write( exception );
		}
	}

	private void waitForEchoStop( int port ) {
		int timeout = 100;
		boolean found = true;
		while( found ) {
			Socket socket = null;
			String data = String.valueOf( System.currentTimeMillis() );
			try {
				// Open the socket.
				socket = new Socket();
				socket.setSoTimeout( timeout );
				socket.connect( new InetSocketAddress( InetAddress.getLoopbackAddress(), port ), timeout );

				// Write the current time.
				socket.getOutputStream().write( data.getBytes( TextUtil.DEFAULT_CHARSET ) );
				socket.getOutputStream().write( '\n' );
				socket.getOutputStream().flush();
				
				// Read the response.
				BufferedReader reader = new BufferedReader( new InputStreamReader( socket.getInputStream(), TextUtil.DEFAULT_CHARSET ) );
				String echo = reader.readLine();
				Log.write( Log.DEBUG, "Echo: ", echo );

				// Program is still running so pause.
				ThreadUtil.pause( timeout );
			} catch( SocketTimeoutException exception ) {
				return;
			} catch( IOException exception ) {
				Log.write( exception );
			} finally {
				try {
					if( socket != null ) socket.close();
				} catch( IOException exception ) {
					Log.write( exception );
				}
			}
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

		// Callback to the parent process.
		if( parameters.isSet( UpdaterFlag.CALLBACK ) ) {
			String portString = parameters.get( UpdaterFlag.CALLBACK );
			int port = -1;
			try {
				port = Integer.parseInt( portString );
			} catch( NumberFormatException exception ) {
				Log.write( exception );
			}
			if( port > 0 && port < 65536 ) {
				Log.write( Log.TRACE, "Callback to parent updater on port: ", port );
				Socket socket = null;
				try {
					socket = new Socket( InetAddress.getLoopbackAddress(), port );
				} catch( IOException exception ) {
					Log.write( exception );
				} finally {
					try {
						if( socket != null ) socket.close();
					} catch( IOException exception ) {
						Log.write( exception );
					}
				}
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
