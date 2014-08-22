package com.parallelsymmetry.updater;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.security.InvalidParameterException;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.logging.FileHandler;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import com.parallelsymmetry.utility.Descriptor;
import com.parallelsymmetry.utility.FileUtil;
import com.parallelsymmetry.utility.HashUtil;
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

public final class Updater implements Product {

	private static final String DEL_SUFFIX = ".del";

	private static final String ADD_SUFFIX = ".add";

	private Parameters parameters;

	private ProductCard card;

	private String logFilePattern;

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

	public void call( String[] commands ) {
		try {
			try {
				parameters = Parameters.parse( commands );
			} catch( InvalidParameterException exception ) {
				Log.write( Log.ERROR, exception.getMessage() );
				printHelp();
				return;
			}

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

			describe();

			printHeader();

			if( parameters.isTrue( UpdaterFlag.WHAT ) || parameters.isTrue( UpdaterFlag.HELP ) ) {
				printHelp();
				return;
			} else if( parameters.isTrue( UpdaterFlag.VERSION ) ) {
				printVersion();
				return;
			}

			if( parameters.isSet( UpdaterFlag.UPDATE ) ) {
				List<String> files = parameters.getValues( UpdaterFlag.UPDATE );

				try {
					int index = 0;
					int count = files.size();

					if( count == 0 || "true".equals( parameters.get( UpdaterFlag.UPDATE ) ) ) throw new IllegalArgumentException( "No update files specified." );

					if( parameters.isSet( UpdaterFlag.UPDATE_DELAY ) ) {
						String delayValue = parameters.get( UpdaterFlag.UPDATE_DELAY );
						Log.write( "Update delay: ", delayValue, "ms" );
						try {
							ThreadUtil.pause( Long.parseLong( delayValue ) );
						} catch( NumberFormatException exception ) {
							Log.write( exception );
						}
					}

					while( index < count ) {
						String source = null;
						String target = null;
						if( index < count ) source = files.get( index );
						if( index + 1 < count ) target = files.get( index + 1 );
						if( source == null ) throw new IllegalArgumentException( "Source parameter not specified." );
						if( target == null ) throw new IllegalArgumentException( "Target parameter not specified." );
						update( new File( source ).getCanonicalFile(), new File( target ).getCanonicalFile() );
						index += 2;
					}
				} catch( RuntimeException exception ) {
					Log.write( exception );
				}
			}

			if( parameters.isSet( UpdaterFlag.LAUNCH ) ) {
				if( parameters.isSet( UpdaterFlag.LAUNCH_DELAY ) ) {
					String delayValue = parameters.get( UpdaterFlag.LAUNCH_DELAY );
					Log.write( "Launch delay: ", delayValue, "ms" );
					try {
						ThreadUtil.pause( Long.parseLong( delayValue ) );
					} catch( NumberFormatException exception ) {
						Log.write( exception );
					}
				}

				try {
					launch( parameters );
				} catch( IOException exception ) {
					Log.write( exception );
				}
			} else {
				printHelp();
				return;
			}
		} catch( Throwable throwable ) {
			Log.write( throwable );
		}
	}

	public void update( File source, File target ) throws Throwable {
		if( !source.exists() ) throw new IllegalArgumentException( "Source parameter not found: " + source );
		if( !target.exists() ) throw new IllegalArgumentException( "Target parameter not found: " + target );

		if( !target.isDirectory() ) throw new IOException( "Target must be a folder: " + target );

		Log.write( Log.TRACE, "Staging: " + target );

		try {
			stage( source, target );
		} catch( ZipException exception ) {
			throw new IOException( "Source not a valid zip file: " + source );
		} catch( Throwable throwable ) {
			Log.write( Log.WARN, throwable.getMessage() );
			Log.write( Log.WARN, "Reverting: " + target );
			revert( target, target );
			throw throwable;
		}

		Log.write( Log.TRACE, "Committing: " + target );
		commit( target, target );

		//source.renameTo( new File( source.getAbsolutePath() + ".old" ) );

		Log.write( "Successful update: " + source );
	}

	@Override
	public File getDataFolder() {
		return OperatingSystem.getUserProgramDataFolder( card.getArtifact(), card.getName() );
	}

	private void describe() {
		try {
			URI uri = getClass().getResource( "/META-INF/product.xml" ).toURI();
			card = new ProductCard( uri, new Descriptor( uri ) );
		} catch( Exception exception ) {
			throw new RuntimeException( exception );
		}
	}

	private void stage( File source, File target ) throws IOException {
		Log.write( Log.DEBUG, "Staging: " + source.getName() + " to " + target + "..." );

		ZipFile zip = new ZipFile( source );

		try {
			Enumeration<? extends ZipEntry> entries = zip.entries();
			while( entries.hasMoreElements() ) {
				ZipEntry entry = entries.nextElement();
				if( !stage( zip.getInputStream( entry ), target, entry.getName() ) ) throw new RuntimeException( "Could not stage: " + new File( target, entry.getName() ) );
			}
		} finally {
			if( zip != null ) zip.close();
		}

		Log.write( Log.TRACE, "Staged: " + source.getName() + " to " + target );
	}

	private boolean stage( InputStream input, File target, String entry ) throws IOException {
		File file = new File( target, entry );
		boolean folder = entry.endsWith( "/" );

		if( folder ) {
			if( !file.exists() && !file.mkdirs() ) throw new IOException( "Could not create folder: " + file );
		} else {
			if( file.exists() ) {
				File delFile = new File( file.getAbsolutePath() + DEL_SUFFIX );
				if( !file.renameTo( delFile ) ) throw new IOException( "Could not rename file: " + file );
			}
			File addFile = new File( file.getAbsolutePath() + ADD_SUFFIX );
			addFile.getParentFile().mkdirs();
			FileOutputStream output = null;
			try {
				output = new FileOutputStream( addFile );
				IoUtil.copy( input, output );
			} finally {
				if( output != null ) output.close();
			}
		}

		Log.write( Log.DEBUG, "Staging: " + entry );

		return true;
	}

	private void commit( File root, File target ) {
		// Commit staged changes.
		if( target.isDirectory() ) {
			File[] files = target.listFiles();
			for( File file : files ) {
				commit( root, file );
			}
		} else {
			if( target.getName().endsWith( ADD_SUFFIX ) ) {
				String sourceHash = HashUtil.hash( target );
				File file = FileUtil.removeExtension( target );
				target.renameTo( file );
				String targetHash = HashUtil.hash( file );
				if( !targetHash.equals( sourceHash ) ) throw new RuntimeException( "Hash code mismatch commiting file: " + file );
				Log.write( Log.TRACE, "Commit: " + relativize( root, file ) );
			} else if( target.getName().endsWith( DEL_SUFFIX ) ) {
				File file = FileUtil.removeExtension( target );
				target.delete();
				if( !file.exists() ) Log.write( Log.TRACE, "Remove: " + relativize( root, FileUtil.removeExtension( target ) ) );
			}
		}
	}

	private void revert( File root, File target ) {
		// Revert staged changes.
		if( target.isDirectory() ) {
			File[] files = target.listFiles();
			for( File file : files ) {
				revert( root, file );
			}
		} else {
			if( target.getName().endsWith( DEL_SUFFIX ) ) {
				target.renameTo( FileUtil.removeExtension( target ) );
			} else if( target.getName().endsWith( ADD_SUFFIX ) ) {
				target.delete();
			}
		}
	}

	private String relativize( File root, File file ) {
		return root.toURI().relativize( file.toURI() ).toString();
	}

	private void launch( Parameters parameters ) throws IOException {
		List<String> values = parameters.getValues( UpdaterFlag.LAUNCH );
		String workFolder = parameters.get( UpdaterFlag.LAUNCH_HOME );
		boolean launchElevated = parameters.isSet( UpdaterFlag.LAUNCH_ELEVATED );
		boolean processElevated = OperatingSystem.isProcessElevated();

		ProcessBuilder builder = null;

		if( launchElevated ) {
			if( processElevated ) {
				builder = new ProcessBuilder( values );
			} else {
				builder = OperatingSystem.elevateProcessBuilder( getCard().getName(), new ProcessBuilder( values ) );
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
