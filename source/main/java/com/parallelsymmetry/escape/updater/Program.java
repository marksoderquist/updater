package com.parallelsymmetry.escape.updater;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.ObjectInputStream;
import java.security.InvalidParameterException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

import com.parallelsymmetry.escape.utility.Descriptor;
import com.parallelsymmetry.escape.utility.Parameters;
import com.parallelsymmetry.escape.utility.Release;
import com.parallelsymmetry.escape.utility.TextUtil;
import com.parallelsymmetry.escape.utility.Version;
import com.parallelsymmetry.escape.utility.log.Log;

public final class Program {

	private static final String RELEASE_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

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
			help();
			return;
		}

		setLogConfiguration( parameters );

		if( parameters.isSet( "?" ) || parameters.isSet( "help" ) ) {
			help();
			return;
		}

		String path = parameters.get( "path" );
		List<File> files = parameters.getFiles();

		if( parameters.isSet( "restart" ) ) {
			// Read the restart parameters from stdin.
			try {
				String[] restartCommands = getRestartCommands();
			} catch( Exception exception ) {
				Log.write( exception );
			}
		}
	}

	private String[] getRestartCommands() throws Exception {
		ObjectInputStream objectInput = new ObjectInputStream( new BufferedInputStream( System.in ) );
		return (String[])objectInput.readObject();
	}

	private void describe() {
		SimpleDateFormat releaseDateFormat = new SimpleDateFormat( RELEASE_DATE_FORMAT );
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
		Log.write( Log.NONE, name + " " + release.toHumanString() );
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

	private void help() {
		// ---------0--------1---------2---------3---------4---------5---------6---------7---------8
		// ---------12345678901234567890123456789012345678901234567890123456789012345678901234567890
		Log.write( "Usage: java -jar <jar file name> [<option>...]" );
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

		flags.add( "?" );
		flags.add( "help" );
		flags.add( "version" );
		flags.add( "log.level" );
		flags.add( "log.color" );

		return flags;
	}

	private void setLogConfiguration( Parameters parameters ) {
		Log.setShowColor( parameters.isSet( "log.color" ) );
		Log.setLevel( Log.parseLevel( parameters.get( "log.level" ) ) );
	}

}
