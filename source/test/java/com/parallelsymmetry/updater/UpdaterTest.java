package com.parallelsymmetry.updater;

import com.parallelsymmetry.utility.*;
import com.parallelsymmetry.utility.log.DefaultHandler;
import com.parallelsymmetry.utility.log.Log;
import com.parallelsymmetry.utility.Version;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.charset.Charset;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.logging.Level;

import static org.junit.jupiter.api.Assertions.*;

public class UpdaterTest extends BaseTestCase {

	@Test
	public void testCommandLineOutput() throws Exception {
		LineParser parser = new LineParser( getCommandLineOutput( updater, Log.INFO ) );
		assertCommandLineHeader( parser );
		assertCommandLineHelp( parser );
		assertNull( parser.next() );
	}

	@Test
	public void testVersionOutput() throws Exception {
		LineParser parser = new LineParser( getCommandLineOutput( updater, Log.INFO, "-version" ) );
		assertCommandLineHeader( parser );
		assertCommandLineVersion( parser );
		assertNull( parser.next() );
	}

	@Test
	public void testQuestionOutput() throws Exception {
		LineParser parser = new LineParser( getCommandLineOutput( updater, Log.INFO, "-?" ) );
		assertCommandLineHeader( parser );
		assertCommandLineHelp( parser );
		assertNull( parser.next() );
	}

	@Test
	public void testHelpOutput() throws Exception {
		LineParser parser = new LineParser( getCommandLineOutput( updater, Log.INFO, "-help" ) );
		assertCommandLineHeader( parser );
		assertCommandLineHelp( parser );
		assertNull( parser.next() );
	}

	@Test
	public void testVersionOutputUsingStdin() throws Exception {
		InputStream stdin = System.in;
		InputStream commandInput = new ByteArrayInputStream( "-version".getBytes( TextUtil.DEFAULT_CHARSET ) );
		System.setIn( commandInput );
		try {
			LineParser parser = new LineParser( getCommandLineOutput( updater, Log.INFO, "-stdin" ) );
			assertCommandLineHeader( parser );
			assertCommandLineVersion( parser );
			assertNull( parser.next() );
		} finally {
			System.setIn( stdin );
		}

	}

	@Test
	public void testHelpOutputUsingStdin() throws Exception {
		InputStream stdin = System.in;
		InputStream commandInput = new ByteArrayInputStream( "-help".getBytes( TextUtil.DEFAULT_CHARSET ) );
		System.setIn( commandInput );
		try {
			LineParser parser = new LineParser( getCommandLineOutput( updater, Log.INFO, "-stdin" ) );
			assertCommandLineHeader( parser );
			assertCommandLineHelp( parser );
			assertNull( parser.next() );
		} finally {
			System.setIn( stdin );
		}

	}

	@Test
	public void testUpdateOutputWithNoSource() throws Exception {
		LineParser parser = new LineParser( getCommandLineOutput( updater, Log.INFO, "--update" ) );
		assertCommandLineHeader( parser );
		assertEquals( "[E] java.lang.IllegalArgumentException: No update files specified.", parser.next() );
	}

	@Test
	public void testUpdateOutputWithNoTarget() throws Exception {
		LineParser parser = new LineParser( getCommandLineOutput( updater, Log.INFO, "--update", "test.zip" ) );
		assertCommandLineHeader( parser );
		assertEquals( "[E] java.lang.IllegalArgumentException: Target parameter not specified.", parser.next() );
	}

	@Test
	public void testUpdateOutputWithInvalidSource() throws Exception {
		LineParser parser = new LineParser( getCommandLineOutput( updater, Log.INFO, "--update", "source/test/resources/invalid.zip", "target/test/update" ) );
		assertCommandLineHeader( parser );

		String line = parser.next();
		assertTrue( line.startsWith( "[E] java.io.IOException: Source not a valid zip file: " ) );
		assertTrue( line.endsWith( "source" + File.separator + "test" + File.separator + "resources" + File.separator + "invalid.zip" ) );
	}

	@Test
	public void testUpdateOutputWithMissingTarget() throws Exception {
		LineParser parser = new LineParser( getCommandLineOutput( updater, Log.INFO, "--update", "source/test/resources/invalid.zip", "target/invalid" ) );
		assertCommandLineHeader( parser );

		String line = parser.next();
		assertTrue( line.startsWith( "[E] java.lang.IllegalArgumentException: Target parameter not found: " ) );
		assertTrue( line.endsWith( "target" + File.separator + "invalid" ) );
	}

	@Test
	public void testSimultaneousUpdate() throws Exception {
		LineParser parser = new LineParser( getCommandLineOutput(
			updater,
			Log.INFO,
			"--update",
			"source/test/resources/update1.zip",
			"target/test/update",
			"source/test/resources/update2.zip",
			"target/test/update"
		) );
		assertCommandLineHeader( parser );

		assertEquals( "Sample 1 Version 2", FileUtil.load( sample1 ).trim() );
		assertEquals( "Sample 2 Version 2", FileUtil.load( sample2 ).trim() );
		assertTrue( folder1.exists() );
		assertEquals( "File 1.1 Version 2", FileUtil.load( file1_1 ).trim() );
		assertEquals( "File 1.2 Version 2", FileUtil.load( file1_2 ).trim() );
		assertTrue( folder2.exists() );
		assertEquals( "File 2.1 Version 2", FileUtil.load( file2_1 ).trim() );
		assertEquals( "File 2.2 Version 2", FileUtil.load( file2_2 ).trim() );
	}

	private String getCommandLineOutput( Updater service, Level level, String... commands ) throws Exception {
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		DefaultHandler handler = new DefaultHandler( new PrintStream( buffer ) );
		handler.setLevel( level );
		Log.addHandler( handler );

		try {
			service.call( commands );
		} finally {
			Log.removeHandler( handler );
		}

		return buffer.toString( TextUtil.DEFAULT_ENCODING );
	}

	private void assertCommandLineHeader( LineParser parser ) throws Exception {
		Descriptor descriptor = new Descriptor( getClass().getResourceAsStream( "/META-INF/product.xml" ) );
		Version version = new Version( descriptor.getValue( "/product/version" ) );
		Date date = null;
		try {
			date = new Date( Long.parseLong( descriptor.getValue( "/product/timestamp" ) ) );
		} catch( NumberFormatException exception ) {
			date = new Date();
		}

		Release release = new Release( version, date );
		int currentYear = Calendar.getInstance( TimeZone.getTimeZone( "UTC" ) ).get( Calendar.YEAR );

		assertEquals( TextUtil.pad( 75, '-' ), parser.next() );
		assertEquals( "Parallel Symmetry Updater " + release.getVersion().toHumanString(), parser.next() );
		assertEquals( "(C) 2010-" + currentYear + " Avereon All rights reserved.", parser.next() );
		assertEquals( "", parser.next() );
		assertEquals( "Parallel Symmetry Updater comes with ABSOLUTELY NO WARRANTY. This is open", parser.next() );
		assertEquals( "software, and you are welcome to redistribute it under certain conditions.", parser.next() );
		assertEquals( TextUtil.pad( 75, '-' ), parser.next() );
		assertEquals( "", parser.next() );
	}

	private void assertCommandLineVersion( LineParser parser ) throws Exception {
		assertEquals( "Version: " + updater.getCard().getRelease().toString(), parser.next() );
		assertEquals( "Java version: " + System.getProperty( "java.version" ), parser.next() );
		assertEquals( "Java home: " + System.getProperty( "java.home" ), parser.next() );
		assertEquals( "Default locale: " + Locale.getDefault() + "  encoding: " + Charset.defaultCharset(), parser.next() );
		assertEquals(
			"OS name: " + OperatingSystem.getName() + "  version: " + OperatingSystem.getVersion() + "  arch: " + OperatingSystem.getSystemArchitecture() + "  family: " + OperatingSystem.getFamily(),
			parser.next()
		);
		assertEquals( "", parser.next() );
	}

	private void assertCommandLineHelp( LineParser parser ) throws Exception {
		assertEquals( "Usage: java -jar <jar file name> [<option>...]", parser.next() );
		assertEquals( "", parser.next() );
		assertEquals( "Commands:", parser.next() );
		assertEquals( "  --update <file file>...", parser.next() );
		assertEquals( "    Update files in pairs of two using the first as the source and the second", parser.next() );
		assertEquals( "    as the target. If the launch parameter is specified then the launch", parser.next() );
		assertEquals( "    commands are executed after the updates have been processed.", parser.next() );
		assertEquals( "  --launch command... [-launch.home folder]", parser.next() );
		assertEquals( "", parser.next() );
		assertEquals( "Options:", parser.next() );
		assertEquals( "  -help            Show help information.", parser.next() );
		assertEquals( "  -version         Show version and copyright information only.", parser.next() );
		assertEquals( "", parser.next() );
		assertEquals( "  -log.level <level>   Change the output log level. Levels are:", parser.next() );
		assertEquals( "                       none, error, warn, info, trace, debug, all", parser.next() );
		assertEquals( "  -log.tag             Use level tags in the console output.", parser.next() );
		assertEquals( "  -log.color           Use level colors in the console output.", parser.next() );
		assertEquals( "  -log.prefix          Use level prefixes in the console output.", parser.next() );
		assertEquals( "  -log.file <file>     Output log messages to the specified file.", parser.next() );
		assertEquals( "  -log.file.level      Same as log.level except in regardsd to the file.", parser.next() );
		assertEquals( "  -log.file.append     Append to the log file if file is used.", parser.next() );
		assertEquals( "", parser.next() );
	}

}
