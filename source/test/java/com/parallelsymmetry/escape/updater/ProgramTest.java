package com.parallelsymmetry.escape.updater;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.logging.Level;

import junit.framework.TestCase;

import com.parallelsymmetry.escape.utility.Descriptor;
import com.parallelsymmetry.escape.utility.LineParser;
import com.parallelsymmetry.escape.utility.Release;
import com.parallelsymmetry.escape.utility.Version;
import com.parallelsymmetry.escape.utility.log.DefaultHandler;
import com.parallelsymmetry.escape.utility.log.Log;

public class ProgramTest extends TestCase {

	private static final String RELEASE_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

	public void testCommandLineOutput() throws Exception {
		Program program = new Program();
		LineParser parser = new LineParser( getCommandLineOutput( program, Log.INFO ) );
		assertCommandLineHeader( parser );
	}

	public void testHelpOutput() throws Exception {
		Program program = new Program();
		LineParser parser = new LineParser( getCommandLineOutput( program, Log.INFO, "-?" ) );
		assertCommandLineHeader( parser );

		assertEquals( "Usage: java -jar <jar file name> [<option>...]", parser.next() );
		assertEquals( "", parser.next() );
		assertEquals( "Options:", parser.next() );
		assertEquals( "  -help            Show help information.", parser.next() );
		assertEquals( "  -version         Show version and copyright information only.", parser.next() );
		assertEquals( "", parser.next() );
		assertEquals( "  -log.color           Use ANSI color in the console output.", parser.next() );
		assertEquals( "  -log.level <level>   Change the output log level. Levels are:", parser.next() );
		assertEquals( "                       none, error, warn, info, trace, debug, all", parser.next() );
		assertEquals( "", parser.next() );
		assertEquals( "  -update <file>   The path to the update file.", parser.next() );
		assertEquals( "  -path <path>     The path to apply the update.", parser.next() );
		assertEquals( "", parser.next() );
		assertEquals( "  --launch <parameter>... The command line parameters to launch an application", parser.next() );
		assertEquals( "                          affter the update has been applied.", parser.next() );
		assertEquals( "", parser.next() );
		assertNull( parser.next() );
	}

	private String getCommandLineOutput( Program service, Level level, String... commands ) throws Exception {
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		DefaultHandler handler = new DefaultHandler( new PrintStream( buffer ) );
		handler.setLevel( level );
		Log.addHandler( handler );

		try {
			service.call( commands );
		} finally {
			Log.removeHandler( handler );
		}

		return buffer.toString( "UTF-8" );
	}

	private void assertCommandLineHeader( LineParser parser ) throws Exception {
		SimpleDateFormat releaseDateFormat = new SimpleDateFormat( RELEASE_DATE_FORMAT );
		releaseDateFormat.setTimeZone( TimeZone.getTimeZone( "UTC" ) );

		Descriptor descriptor = new Descriptor( getClass().getResourceAsStream( "/META-INF/program.xml" ) );
		Version version = Version.parse( descriptor.getValue( "/program/information/version" ) );
		Date date = releaseDateFormat.parse( descriptor.getValue( "/program/information/timestamp" ) );

		Release release = new Release( version, date );
		int currentYear = Calendar.getInstance( TimeZone.getTimeZone( "UTC" ) ).get( Calendar.YEAR );

		assertEquals( "Escape Updater " + release.toHumanString(), parser.next() );
		assertEquals( "(C) 2010-" + currentYear + " Parallel Symmetry All rights reserved.", parser.next() );
		assertEquals( "", parser.next() );
		assertEquals( "Escape Updater comes with ABSOLUTELY NO WARRANTY. This is open software, and", parser.next() );
		assertEquals( "you are welcome to redistribute it under certain conditions.", parser.next() );
		assertEquals( "", parser.next() );
	}

}
