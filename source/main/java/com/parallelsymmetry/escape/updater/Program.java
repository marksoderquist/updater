package com.parallelsymmetry.escape.updater;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.security.InvalidParameterException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import com.parallelsymmetry.escape.utility.Descriptor;
import com.parallelsymmetry.escape.utility.OperatingSystem;
import com.parallelsymmetry.escape.utility.Parameters;
import com.parallelsymmetry.escape.utility.Release;
import com.parallelsymmetry.escape.utility.TextUtil;
import com.parallelsymmetry.escape.utility.Version;
import com.parallelsymmetry.escape.utility.log.Log;

public final class Program {

	private static final String WHAT = "?";

	private static final String HELP = "help";

	private static final String VERSION = "version";

	private static final String UPDATE = "update";

	private static final String SOURCE = "source";

	private static final String TARGET = "target";

	private static final String LAUNCH = "launch";

	private static final String COPYRIGHT = "(C)";

	private Parameters parameters;

	private String name;

	private Release release;

	private int inceptionYear;

	private String copyright;

	private String copyrightNotice;

	private String copyrightHolder;

	private String licenseSummary;

	public static final void main( String[] commands ) {
		new Program().call( commands );
	}

	public void call( String[] commands ) {
		try {
			parameters = Parameters.parse( commands, getValidCommandLineFlags() );
		} catch( InvalidParameterException exception ) {
			Log.write( Log.ERROR, exception.getMessage() );
			printHelp();
			return;
		}

		configureLog( parameters );

		describe();

		printHeader();

		if( parameters.isSet( WHAT ) || parameters.isSet( HELP ) ) {
			printHelp();
			return;
		} else if( parameters.isSet( VERSION ) ) {
			printVersion();
			return;
		}

		try {
			if( parameters.isSet( UPDATE ) ) {
				String source = parameters.get( SOURCE );
				String target = parameters.get( TARGET );
				if( source == null ) throw new IllegalArgumentException( "Source parameter not specified." );
				if( target == null ) throw new IllegalArgumentException( "Target parameter not specified." );
				update( new File( source ), new File( target ) );
			} else {
				printHelp();
				return;
			}
		} catch( Throwable throwable ) {
			Log.write( throwable );
		}
	}

	public Release getRelease() {
		return release;
	}

	public void update( File source, File target ) throws IOException {
		if( !source.exists() ) throw new IllegalArgumentException( "Source parameter not found: " + source );
		if( !target.exists() ) throw new IllegalArgumentException( "Target parameter not found: " + target );

		ZipFile zip;
		try {
			zip = new ZipFile( source );
		} catch( ZipException exception ) {
			throw new RuntimeException( "Source not a valid zip file: " + source );
		}

		try {
			stage( zip, target );
		} catch( Throwable throwable ) {
			revert( target );
		}

		commit( target );
	}

	private void stage( ZipFile source, File target ) {
		Log.write( Log.DEBUG, "Staging: " + source + " to " + target + "..." );

		// Go through each resource in the update file and move any existing resource to a stage name.
		Enumeration<? extends ZipEntry> entries = source.entries();
		while( entries.hasMoreElements() ) {
			ZipEntry entry = entries.nextElement();

		}

		Log.write( Log.TRACE, "Staged: " + source + " to " + target );
	}

	private void commit( File target ) {
		Log.write( Log.DEBUG, "Committing: " + target + "..." );

		// Commit staged changes to their original state.

		Log.write( Log.TRACE, "Committed: " + target );
	}

	private void revert( File target ) {
		Log.write( "Reverting: " + target + "..." );

		// Revert staged changes to their original state.

		Log.write( "Reverted: " + target );
	}

	private void describe() {
		SimpleDateFormat releaseDateFormat = new SimpleDateFormat( Release.DATE_FORMAT );
		releaseDateFormat.setTimeZone( TimeZone.getTimeZone( "UTC" ) );

		try {
			Descriptor descriptor = new Descriptor( getClass().getResourceAsStream( "/META-INF/program.xml" ) );
			Version version = Version.parse( descriptor.getValue( "/program/information/version" ) );
			Date date = releaseDateFormat.parse( descriptor.getValue( "/program/information/timestamp" ) );
			int currentYear = Calendar.getInstance( TimeZone.getTimeZone( "UTC" ) ).get( Calendar.YEAR );

			name = descriptor.getValue( "/program/information/name" );
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
		Log.write( "Version: " + getRelease().getRelease() );
		Log.write( "Java version: " + System.getProperty( "java.version" ) );
		Log.write( "Java home: " + System.getProperty( "java.home" ) );
		Log.write( "Default locale: " + Locale.getDefault() + "  encoding: " + Charset.defaultCharset() );
		Log.write( "OS name: " + OperatingSystem.getName() + "  version: " + OperatingSystem.getVersion() + "  arch: " + OperatingSystem.getSystemArchitecture() + "  family: " + OperatingSystem.getFamily() );
	}

	private void printHelp() {
		// ---------0--------1---------2---------3---------4---------5---------6---------7---------8
		// ---------12345678901234567890123456789012345678901234567890123456789012345678901234567890
		Log.write( "Usage: java -jar <jar file name> [<option>...]" );
		Log.write();
		Log.write( "Commands:" );
		Log.write( "  -update -source <file> -target <path> [-launch command...]" );
		Log.write( "    Use the specified source file to update the specified target path." );
		Log.write( "    If the launch parameter is specified then the launch commands are executed." );
		Log.write();
		Log.write( "Options:" );
		Log.write( "  -help            Show help information." );
		Log.write( "  -version         Show version and copyright information only." );
		Log.write();
		Log.write( "  -log.color           Use ANSI color in the console output." );
		Log.write( "  -log.level <level>   Change the output log level. Levels are:" );
		Log.write( "                       none, error, warn, info, trace, debug, all" );
	}

	private Set<String> getValidCommandLineFlags() {
		Set<String> flags = new HashSet<String>();

		flags.add( WHAT );
		flags.add( HELP );
		flags.add( VERSION );
		flags.add( "log.level" );
		flags.add( "log.color" );

		flags.add( UPDATE );
		flags.add( SOURCE );
		flags.add( TARGET );
		flags.add( LAUNCH );

		return flags;
	}

	private void configureLog( Parameters parameters ) {
		Log.setShowColor( parameters.isSet( "log.color" ) );
		Log.setLevel( Log.parseLevel( parameters.get( "log.level" ) ) );
	}

}
