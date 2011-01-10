package com.parallelsymmetry.escape.updater;

import java.io.File;
import java.nio.charset.Charset;
import java.security.InvalidParameterException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

import com.parallelsymmetry.escape.utility.Descriptor;
import com.parallelsymmetry.escape.utility.OperatingSystem;
import com.parallelsymmetry.escape.utility.Parameters;
import com.parallelsymmetry.escape.utility.Release;
import com.parallelsymmetry.escape.utility.TextUtil;
import com.parallelsymmetry.escape.utility.Version;
import com.parallelsymmetry.escape.utility.log.Log;

public final class Program {

	private static final String COPYRIGHT = "(C)";

	private Parameters parameters;

	private String name;

	private Release release;

	private int inceptionYear;

	private String copyrightNotice;

	private String copyrightHolder;

	private String licenseSummary;

	public static final void main( String[] commands ) {
		new Program().call( commands );
	}

	public void call( String[] commands ) {
		describe();

		printHeader();

		try {
			parameters = Parameters.parse( commands, getValidCommandLineFlags() );
		} catch( InvalidParameterException exception ) {
			Log.write( Log.ERROR, exception.getMessage() );
			printHelp();
			return;
		}

		setLogConfiguration( parameters );

		if( parameters.isSet( "?" ) || parameters.isSet( "help" ) ) {
			printHelp();
			return;
		} else if( parameters.isSet( "version" ) ) {
			printVersion();
			return;
		}

		try {
			if( parameters.isSet( "update" ) ) {
				String file = parameters.get( "file" );
				String path = parameters.get( "path" );
				if( file == null ) throw new IllegalArgumentException( "File parameter not specified." );
				if( path == null ) throw new IllegalArgumentException( "Path parameter not specified." );
				update( new File( file ), new File( path ) );
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

	private void update( File file, File path ) {

	}

	private void describe() {
		SimpleDateFormat releaseDateFormat = new SimpleDateFormat( Release.DATE_FORMAT );
		releaseDateFormat.setTimeZone( TimeZone.getTimeZone( "UTC" ) );

		try {
			Descriptor descriptor = new Descriptor( getClass().getResourceAsStream( "/META-INF/program.xml" ) );
			Version version = Version.parse( descriptor.getValue( "/program/information/version" ) );
			Date date = releaseDateFormat.parse( descriptor.getValue( "/program/information/timestamp" ) );

			name = descriptor.getValue( "/program/information/name" );
			release = new Release( version, date );

			inceptionYear = Integer.parseInt( descriptor.getValue( "/program/information/inception" ) );
			copyrightNotice = descriptor.getValue( "/program/information/copyright/notice" );
			copyrightHolder = descriptor.getValue( "/program/information/organization" );
			licenseSummary = TextUtil.reline( descriptor.getValue( "/program/information/license/summary" ), 79 );
		} catch( Exception exception ) {
			throw new RuntimeException( exception );
		}
	}

	private void printHeader() {
		Log.write( Log.NONE, name + " " + release.getVersion().toHumanString() );
		Log.write( Log.NONE, getCopyright() + " " + copyrightNotice );
		Log.write( Log.NONE );
		if( licenseSummary != null ) {
			Log.write( Log.NONE, licenseSummary );
			Log.write( Log.NONE );
		}

		Log.write( Log.TRACE, "Java: " + System.getProperty( "java.runtime.version" ) );
	}

	private String getCopyright() {
		int currentYear = Calendar.getInstance( TimeZone.getTimeZone( "UTC" ) ).get( Calendar.YEAR );
		return COPYRIGHT + " " + ( currentYear == inceptionYear ? currentYear : inceptionYear + "-" + currentYear ) + " " + copyrightHolder;
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
		Log.write( "  -update -file <file> -path <path> [-launch command...]" );
		Log.write( "    Use the specified file to update the specified path. If the launch" );
		Log.write( "    parameter is specified then the launch commands are executed." );
		Log.write();
		Log.write( "Options:" );
		Log.write( "  -help            Show help information." );
		Log.write( "  -version         Show version and copyright information only." );
		Log.write();
		Log.write( "  -log.color           Use ANSI color in the console output." );
		Log.write( "  -log.level <level>   Change the output log level. Levels are:" );
		Log.write( "                       none, error, warn, info, trace, debug, all" );
		Log.write();
		Log.write( "  -update <file>   The path to the update file." );
		Log.write( "  -path <path>     The path to apply the update." );
		Log.write();
		Log.write( "  --launch <parameter>... The command line parameters to launch an application" );
		Log.write( "                          after the update has been applied." );
	}

	private Set<String> getValidCommandLineFlags() {
		Set<String> flags = new HashSet<String>();

		flags.add( "?" );
		flags.add( "help" );
		flags.add( "version" );
		flags.add( "log.level" );
		flags.add( "log.color" );

		flags.add( "update" );
		flags.add( "file" );
		flags.add( "path" );
		flags.add( "launch" );

		return flags;
	}

	private void setLogConfiguration( Parameters parameters ) {
		Log.setShowColor( parameters.isSet( "log.color" ) );
		Log.setLevel( Log.parseLevel( parameters.get( "log.level" ) ) );
	}

}
