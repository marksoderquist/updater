package com.parallelsymmetry.escape.updater;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.security.InvalidParameterException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
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
import com.parallelsymmetry.escape.utility.Version;
import com.parallelsymmetry.escape.utility.log.Log;

public final class Program {

	private static final String COPYRIGHT = "(C)";

	private static final String WHAT = "?";

	private static final String HELP = "help";

	private static final String VERSION = "version";

	private static final String UPDATE = "update";

	private static final String LAUNCH = "launch";

	private static final String LAUNCH_HOME = "launch.home";

	private static final String DEL_SUFFIX = ".del";

	private static final String ADD_SUFFIX = ".add";

	private Parameters parameters;

	private String name;

	private Release release;

	private int inceptionYear;

	private String copyright;

	private String copyrightNotice;

	private String copyrightHolder;

	private String licenseSummary;

	public Program() {
		describe();
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

			Log.init( parameters );

			describe();

			printHeader();

			if( parameters.isTrue( WHAT ) || parameters.isTrue( HELP ) ) {
				printHelp();
				return;
			} else if( parameters.isTrue( VERSION ) ) {
				printVersion();
				return;
			}

			if( parameters.isSet( UPDATE ) ) {
				List<String> files = parameters.getValues( UPDATE );

				try {
					int index = 0;
					int count = files.size();

					if( count == 0 || "true".equals( parameters.get( UPDATE ) ) ) throw new IllegalArgumentException( "No update files specified." );

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

			if( parameters.isSet( LAUNCH ) ) {
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

		try {
			stage( source, target );
		} catch( ZipException exception ) {
			throw new IOException( "Source not a valid zip file: " + source );
		} catch( Throwable throwable ) {
			Log.write( Log.WARN, "Reverting: " + target );
			revert( target, target );
			throw throwable;
		}

		Log.write( Log.TRACE, "Committing: " + target );
		commit( target, target );

		//source.renameTo( new File( source.getAbsolutePath() + ".old" ) );

		Log.write( "Success: " + source );
	}

	public static final void main( String[] commands ) {
		new Program().call( commands );
	}

	private void describe() {
		SimpleDateFormat releaseDateFormat = new SimpleDateFormat( Release.DATE_FORMAT );
		releaseDateFormat.setTimeZone( TimeZone.getTimeZone( "UTC" ) );

		try {
			Descriptor descriptor = new Descriptor( getClass().getResourceAsStream( "/META-INF/program.xml" ) );
			name = descriptor.getValue( "/program/information/name" );

			Version version = new Version( descriptor.getValue( "/program/information/version" ) );
			Date date = releaseDateFormat.parse( descriptor.getValue( "/program/information/timestamp" ) );
			int currentYear = Calendar.getInstance( TimeZone.getTimeZone( "UTC" ) ).get( Calendar.YEAR );
			release = new Release( version, date );

			inceptionYear = Integer.parseInt( descriptor.getValue( "/program/information/inception" ) );
			copyrightHolder = descriptor.getValue( "/program/information/organization" );
			copyrightNotice = descriptor.getValue( "/program/information/copyright/notice" );
			copyright = COPYRIGHT + " " + ( currentYear == inceptionYear ? currentYear : inceptionYear + "-" + currentYear ) + " " + copyrightHolder;
			licenseSummary = TextUtil.reline( descriptor.getValue( "/program/information/license/summary" ), 79 );
		} catch( Exception exception ) {
			throw new RuntimeException( exception );
		}
	}

	private void stage( File source, File target ) throws IOException {
		Log.write( Log.DEBUG, "Staging: " + source.getName() + " to " + target + "..." );

		ZipFile zip = new ZipFile( source );

		Enumeration<? extends ZipEntry> entries = zip.entries();
		while( entries.hasMoreElements() ) {
			ZipEntry entry = entries.nextElement();
			if( !stage( zip.getInputStream( entry ), target, entry.getName() ) ) throw new RuntimeException( "Could not stage: " + new File( target, entry.getName() ) );
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
		List<String> values = parameters.getValues( LAUNCH );
		String workFolder = parameters.get( LAUNCH_HOME );

		ProcessBuilder builder = new ProcessBuilder( values );
		if( workFolder != null ) builder.directory( new File( workFolder ) );

		Log.write( Log.DEBUG, "Launching: " + TextUtil.toString( builder.command(), " " ) );

		builder.start();
		Log.write( Log.TRACE, "Program process started." );
	}

	private void printHeader() {
		Log.write( Log.NONE, name + " " + release.getVersion().toHumanString() );
		Log.write( Log.NONE, copyright + " " + copyrightNotice );
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
		Log.write( Log.NONE, "  -log.append          Append to the log file if file is used." );
	}

}
