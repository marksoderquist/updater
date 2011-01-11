package com.parallelsymmetry.escape.updater;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.logging.Level;

import junit.framework.TestCase;

import com.parallelsymmetry.escape.utility.Descriptor;
import com.parallelsymmetry.escape.utility.FileUtil;
import com.parallelsymmetry.escape.utility.LineParser;
import com.parallelsymmetry.escape.utility.OperatingSystem;
import com.parallelsymmetry.escape.utility.Release;
import com.parallelsymmetry.escape.utility.Version;
import com.parallelsymmetry.escape.utility.log.DefaultHandler;
import com.parallelsymmetry.escape.utility.log.Log;

public class ProgramTest extends TestCase {

	private static final String RELEASE_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

	private Program program;

	private File target = new File( "target/test/update" );

	public void setUp() {
		Log.setLevel( Log.NONE );

		FileUtil.delete( target );
		new File( "target/test/update" ).mkdirs();
		assertTrue( target.exists() );
	}

	public void testCommandLineOutput() throws Exception {
		program = new Program();
		LineParser parser = new LineParser( getCommandLineOutput( program, Log.INFO ) );
		assertCommandLineHeader( parser );
		assertCommandLineHelp( parser );
		assertNull( parser.next() );
	}

	public void testVersionOutput() throws Exception {
		program = new Program();
		LineParser parser = new LineParser( getCommandLineOutput( program, Log.INFO, "-version" ) );
		assertCommandLineHeader( parser );
		assertCommandLineVersion( parser );
		assertNull( parser.next() );
	}

	public void testQuestionOutput() throws Exception {
		program = new Program();
		LineParser parser = new LineParser( getCommandLineOutput( program, Log.INFO, "-?" ) );
		assertCommandLineHeader( parser );
		assertCommandLineHelp( parser );
		assertNull( parser.next() );
	}

	public void testHelpOutput() throws Exception {
		program = new Program();
		LineParser parser = new LineParser( getCommandLineOutput( program, Log.INFO, "-help" ) );
		assertCommandLineHeader( parser );
		assertCommandLineHelp( parser );
		assertNull( parser.next() );
	}

	public void testUpdateOutputWithNoSource() throws Exception {
		program = new Program();
		LineParser parser = new LineParser( getCommandLineOutput( program, Log.INFO, "-update" ) );
		assertCommandLineHeader( parser );
		assertEquals( "java.lang.IllegalArgumentException: Source parameter not specified.", parser.next() );
	}

	public void testUpdateOutputWithMissingSource() throws Exception {
		program = new Program();
		LineParser parser = new LineParser( getCommandLineOutput( program, Log.INFO, "-update", "-source", "invalid.zip", "-target", "target/test/update" ) );
		assertCommandLineHeader( parser );
		assertEquals( "java.lang.IllegalArgumentException: Source parameter not found: invalid.zip", parser.next() );
	}

	public void testUpdateOutputWithInvalidSource() throws Exception {
		program = new Program();
		LineParser parser = new LineParser( getCommandLineOutput( program, Log.INFO, "-update", "-source", "source/test/resources/invalid.zip", "-target", "target/test/update" ) );
		assertCommandLineHeader( parser );
		assertEquals( "java.lang.RuntimeException: Source not a valid zip file: source" + File.separator + "test" + File.separator + "resources" + File.separator + "invalid.zip", parser.next() );
	}

	public void testUpdateOutputWithNoTarget() throws Exception {
		program = new Program();
		LineParser parser = new LineParser( getCommandLineOutput( program, Log.INFO, "-update", "-source", "test.zip" ) );
		assertCommandLineHeader( parser );
		assertEquals( "java.lang.IllegalArgumentException: Target parameter not specified.", parser.next() );
	}

	public void testUpdateOutputWithMissingTarget() throws Exception {
		program = new Program();
		LineParser parser = new LineParser( getCommandLineOutput( program, Log.INFO, "-update", "-source", "source/test/resources/update.zip", "-target", "target/invalid" ) );
		assertCommandLineHeader( parser );
		assertEquals( "java.lang.IllegalArgumentException: Target parameter not found: target" + File.separator + "invalid", parser.next() );
	}

	public void testUpdate() throws Exception {
		Log.setLevel( Log.DEBUG );
		program = new Program();

		File source = new File( "source/test/resources/update.zip" );

		// FIXME Finish implementing update().
		//program.update( source, target );
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

		assertEquals( "Escape Updater " + release.getVersion().toHumanString(), parser.next() );
		assertEquals( "(C) 2010-" + currentYear + " Parallel Symmetry All rights reserved.", parser.next() );
		assertEquals( "", parser.next() );
		assertEquals( "Escape Updater comes with ABSOLUTELY NO WARRANTY. This is open software, and", parser.next() );
		assertEquals( "you are welcome to redistribute it under certain conditions.", parser.next() );
		assertEquals( "", parser.next() );
	}

	private void assertCommandLineVersion( LineParser parser ) throws Exception {
		assertEquals( "Version: " + program.getRelease().getRelease(), parser.next() );
		assertEquals( "Java version: " + System.getProperty( "java.version" ), parser.next() );
		assertEquals( "Java home: " + System.getProperty( "java.home" ), parser.next() );
		assertEquals( "Default locale: " + Locale.getDefault() + "  encoding: " + Charset.defaultCharset(), parser.next() );
		assertEquals( "OS name: " + OperatingSystem.getName() + "  version: " + OperatingSystem.getVersion() + "  arch: " + OperatingSystem.getSystemArchitecture() + "  family: " + OperatingSystem.getFamily(), parser.next() );
		assertEquals( "", parser.next() );
	}

	private void assertCommandLineHelp( LineParser parser ) throws Exception {
		assertEquals( "Usage: java -jar <jar file name> [<option>...]", parser.next() );
		assertEquals( "", parser.next() );
		assertEquals( "Commands:", parser.next() );
		assertEquals( "  -update -source <file> -target <path> [-launch command...]", parser.next() );
		assertEquals( "    Use the specified source file to update the specified target path.", parser.next() );
		assertEquals( "    If the launch parameter is specified then the launch commands are executed.", parser.next() );
		assertEquals( "", parser.next() );
		assertEquals( "Options:", parser.next() );
		assertEquals( "  -help            Show help information.", parser.next() );
		assertEquals( "  -version         Show version and copyright information only.", parser.next() );
		assertEquals( "", parser.next() );
		assertEquals( "  -log.color           Use ANSI color in the console output.", parser.next() );
		assertEquals( "  -log.level <level>   Change the output log level. Levels are:", parser.next() );
		assertEquals( "                       none, error, warn, info, trace, debug, all", parser.next() );
		assertEquals( "", parser.next() );
	}

}
