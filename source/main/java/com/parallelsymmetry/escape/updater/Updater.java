package com.parallelsymmetry.escape.updater;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.security.InvalidParameterException;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.logging.FileHandler;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import com.parallelsymmetry.escape.utility.Descriptor;
import com.parallelsymmetry.escape.utility.FileUtil;
import com.parallelsymmetry.escape.utility.IoUtil;
import com.parallelsymmetry.escape.utility.OperatingSystem;
import com.parallelsymmetry.escape.utility.Parameters;
import com.parallelsymmetry.escape.utility.Release;
import com.parallelsymmetry.escape.utility.TextUtil;
import com.parallelsymmetry.escape.utility.ThreadUtil;
import com.parallelsymmetry.escape.utility.Version;
import com.parallelsymmetry.escape.utility.log.DefaultFormatter;
import com.parallelsymmetry.escape.utility.log.Log;
import com.parallelsymmetry.escape.utility.log.LogFlag;

public final class Updater {

	private static final String COPYRIGHT = "(C)";

	private static final String DEL_SUFFIX = ".del";

	private static final String ADD_SUFFIX = ".add";

	private Parameters parameters;

	private String name;

	private String group;

	private String artifact;

	private Release release;

	private int inceptionYear;

	private String copyright;

	private String copyrightNotice;

	private String provider;

	private String licenseSummary;

	public Updater() {
		describe();
	}

	public static final void main( String[] commands ) {
		new Updater().call( commands );
	}

	public String getName() {
		return name;
	}

	public String getGroup() {
		return group;
	}

	public String getArtifact() {
		return artifact;
	}

	public Release getRelease() {
		return release;
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
					File folder = getProgramDataFolder();
					String pattern = new File( folder, "program.log" ).getCanonicalPath().replace( '\\', '/' );
					folder.mkdirs();

					FileHandler handler = new FileHandler( pattern, parameters.isTrue( LogFlag.LOG_FILE_APPEND ) );
					handler.setLevel( Log.INFO );
					if( parameters.isSet( LogFlag.LOG_FILE_LEVEL ) ) handler.setLevel( Log.parseLevel( parameters.get( LogFlag.LOG_FILE_LEVEL ) ) );
					handler.setFormatter( new DefaultFormatter() );
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
						Log.write( "Update delay..." );
						ThreadUtil.pause( Long.parseLong( parameters.get( UpdaterFlag.UPDATE_DELAY ) ) );
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
					Log.write( "Launch delay..." );
					ThreadUtil.pause( Long.parseLong( parameters.get( UpdaterFlag.LAUNCH_DELAY ) ) );
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

	public File getProgramDataFolder() {
		return OperatingSystem.getUserProgramDataFolder( getArtifact(), getName() );
	}

	private void describe() {
		try {
			Descriptor descriptor = new Descriptor( getClass().getResourceAsStream( "/META-INF/product.xml" ) );

			group = descriptor.getValue( "/product/group" );
			artifact = descriptor.getValue( "/product/artifact" );

			Version version = new Version( descriptor.getValue( "/product/version" ) );
			Date date = null;
			try {
				date = new Date( Long.parseLong( descriptor.getValue( "/product/timestamp" ) ) );
			} catch( NumberFormatException exception ) {
				date = new Date();
			}
			int currentYear = Calendar.getInstance( TimeZone.getTimeZone( "UTC" ) ).get( Calendar.YEAR );
			release = new Release( version, date );

			name = descriptor.getValue( "/product/name" );
			provider = descriptor.getValue( "/product/provider" );

			inceptionYear = Integer.parseInt( descriptor.getValue( "/product/inception" ) );
			copyrightNotice = descriptor.getValue( "/product/copyright/notice" );
			copyright = COPYRIGHT + " " + ( currentYear == inceptionYear ? currentYear : inceptionYear + "-" + currentYear ) + " " + provider;
			licenseSummary = TextUtil.reline( descriptor.getValue( "/product/license/summary" ), 79 );
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
		// Commit staged changes to their original state.
		if( target.isDirectory() ) {
			File[] files = target.listFiles();
			for( File file : files ) {
				commit( root, file );
			}
		} else {
			if( target.getName().endsWith( ADD_SUFFIX ) ) {
				File file = FileUtil.removeExtension( target );
				target.renameTo( file );
				Log.write( Log.TRACE, "Commit: " + relativize( root, file ) );
			} else if( target.getName().endsWith( DEL_SUFFIX ) ) {
				File file = FileUtil.removeExtension( target );
				target.delete();
				if( !file.exists() ) Log.write( Log.TRACE, "Remove: " + relativize( root, FileUtil.removeExtension( target ) ) );
			}
		}
	}

	private void revert( File root, File target ) {
		// Revert staged changes to their original state.
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
				builder = OperatingSystem.elevateProcessBuilder( new ProcessBuilder( values ) );
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
		Log.write( Log.NONE, TextUtil.pad( 60, '-' ) );
		Log.write( Log.NONE, getName() + " " + getRelease().getVersion().toHumanString() );
		Log.write( Log.NONE, copyright, " ", copyrightNotice );
		Log.write( Log.NONE );
		if( licenseSummary != null ) {
			Log.write( Log.NONE, licenseSummary );
			Log.write( Log.NONE );
		}

		Log.write( Log.TRACE, "Java: " + System.getProperty( "java.runtime.version" ) );
	}

	private void printVersion() {
		Log.write( Log.NONE, "Version: " + getRelease().toString() );
		Log.write( Log.NONE, "Java version: " + System.getProperty( "java.version" ) );
		Log.write( Log.NONE, "Java home: " + System.getProperty( "java.home" ) );
		Log.write( Log.NONE, "Default locale: " + Locale.getDefault() + "  encoding: " + Charset.defaultCharset() );
		Log.write( Log.NONE, "OS name: " + OperatingSystem.getName() + "  version: " + OperatingSystem.getVersion() + "  arch: " + OperatingSystem.getSystemArchitecture() + "  family: " + OperatingSystem.getFamily() );
	}

	private void printHelp() {
		// ---------0--------1---------2---------3---------4---------5---------6---------7---------8
		// ---------12345678901234567890123456789012345678901234567890123456789012345678901234567890
		Log.write( Log.NONE, "Usage: java -jar <jar file name> [<option>...]" );
		Log.write( Log.NONE );
		Log.write( Log.NONE, "Commands:" );
		Log.write( Log.NONE, "  --update <file file>..." );
		Log.write( Log.NONE, "    Update files in pairs of two using the first as the source and the second" );
		Log.write( Log.NONE, "    as the target. If the launch parameter is specified then the launch" );
		Log.write( Log.NONE, "    commands are executed after the updates have been processed." );
		Log.write( Log.NONE, "  --launch command... [-launch.home folder]" );
		Log.write( Log.NONE );
		Log.write( Log.NONE, "Options:" );
		Log.write( Log.NONE, "  -help            Show help information." );
		Log.write( Log.NONE, "  -version         Show version and copyright information only." );
		Log.write( Log.NONE );
		Log.write( Log.NONE, "  -log.level <level>   Change the output log level. Levels are:" );
		Log.write( Log.NONE, "                       none, error, warn, info, trace, debug, all" );
		Log.write( Log.NONE, "  -log.tag             Use level tags in the console output." );
		Log.write( Log.NONE, "  -log.color           Use level colors in the console output." );
		Log.write( Log.NONE, "  -log.prefix          Use level prefixes in the console output." );
		Log.write( Log.NONE, "  -log.file <file>     Output log messages to the specified file." );
		Log.write( Log.NONE, "  -log.file.level      Same as log.level except in regardsd to the file." );
		Log.write( Log.NONE, "  -log.file.append     Append to the log file if file is used." );
	}

}
