package com.parallelsymmetry.escape.updater;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.nio.charset.Charset;
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
import com.parallelsymmetry.escape.utility.TextUtil;
import com.parallelsymmetry.escape.utility.Version;
import com.parallelsymmetry.escape.utility.log.DefaultHandler;
import com.parallelsymmetry.escape.utility.log.Log;

public class UpdaterTest extends TestCase {

	private Updater program;

	private File source = new File( "source/test/resources" );

	private File target = new File( "target/test/update" );

	private File sample1 = new File( target, "sample.1.txt" );

	private File sample2 = new File( target, "sample.2.txt" );

	private File folder1 = new File( target, "folder1" );

	private File file1_1 = new File( folder1, "file.1.1.txt" );

	private File file1_2 = new File( folder1, "file.1.2.txt" );

	private File folder2 = new File( target, "folder2" );

	private File file2_1 = new File( folder2, "file.2.1.txt" );

	private File file2_2 = new File( folder2, "file.2.2.txt" );

	private File update0 = new File( source, "update0.zip" );

	private File update1 = new File( source, "update1.zip" );

	private File update2 = new File( source, "update2.zip" );

	@Override
	public void setUp() throws Exception {
		Log.setLevel( Log.NONE );

		FileUtil.delete( target );
		target.mkdirs();
		assertTrue( target.exists() );

		FileUtil.unzip( update0, target );

		assertEquals( "Sample 1 Version 0", FileUtil.load( sample1 ).trim() );
		assertFalse( sample2.exists() );
		assertFalse( folder1.exists() );
		assertFalse( file1_1.exists() );
		assertFalse( file1_2.exists() );
		assertTrue( folder2.exists() );
		assertFalse( file2_1.exists() );
		assertEquals( "File 2.2 Version 0", FileUtil.load( file2_2 ).trim() );
	}

	public void testCommandLineOutput() throws Exception {
		Log.setLevel( Log.INFO );
		program = new Updater();
		LineParser parser = new LineParser( getCommandLineOutput( program, Log.INFO ) );
		assertCommandLineHeader( parser );
		assertCommandLineHelp( parser );
		assertNull( parser.next() );
	}

	public void testVersionOutput() throws Exception {
		program = new Updater();
		LineParser parser = new LineParser( getCommandLineOutput( program, Log.INFO, "-version" ) );
		assertCommandLineHeader( parser );
		assertCommandLineVersion( parser );
		assertNull( parser.next() );
	}

	public void testQuestionOutput() throws Exception {
		program = new Updater();
		LineParser parser = new LineParser( getCommandLineOutput( program, Log.INFO, "-?" ) );
		assertCommandLineHeader( parser );
		assertCommandLineHelp( parser );
		assertNull( parser.next() );
	}

	public void testHelpOutput() throws Exception {
		program = new Updater();
		LineParser parser = new LineParser( getCommandLineOutput( program, Log.INFO, "-help" ) );
		assertCommandLineHeader( parser );
		assertCommandLineHelp( parser );
		assertNull( parser.next() );
	}

	public void testUpdateOutputWithNoSource() throws Exception {
		program = new Updater();
		LineParser parser = new LineParser( getCommandLineOutput( program, Log.INFO, "--update" ) );
		assertCommandLineHeader( parser );
		assertEquals( "[E] java.lang.IllegalArgumentException: No update files specified.", parser.next() );
	}

	public void testUpdateOutputWithNoTarget() throws Exception {
		program = new Updater();
		LineParser parser = new LineParser( getCommandLineOutput( program, Log.INFO, "--update", "test.zip" ) );
		assertCommandLineHeader( parser );
		assertEquals( "[E] java.lang.IllegalArgumentException: Target parameter not specified.", parser.next() );
	}

	public void testUpdateOutputWithInvalidSource() throws Exception {
		program = new Updater();
		LineParser parser = new LineParser( getCommandLineOutput( program, Log.INFO, "--update", "source/test/resources/invalid.zip", "target/test/update" ) );
		assertCommandLineHeader( parser );

		String line = parser.next();
		assertTrue( line.startsWith( "[E] java.io.IOException: Source not a valid zip file: " ) );
		assertTrue( line.endsWith( "source" + File.separator + "test" + File.separator + "resources" + File.separator + "invalid.zip" ) );
	}

	public void testUpdateOutputWithMissingTarget() throws Exception {
		program = new Updater();
		LineParser parser = new LineParser( getCommandLineOutput( program, Log.INFO, "--update", "source/test/resources/invalid.zip", "target/invalid" ) );
		assertCommandLineHeader( parser );

		String line = parser.next();
		assertTrue( line.startsWith( "[E] java.lang.IllegalArgumentException: Target parameter not found: " ) );
		assertTrue( line.endsWith( "target" + File.separator + "invalid" ) );
	}

	public void testUpdate() throws Throwable {
		program = new Updater();

		program.update( update1, target );
		assertEquals( "Sample 1 Version 1", FileUtil.load( sample1 ).trim() );
		assertEquals( "Sample 2 Version 1", FileUtil.load( sample2 ).trim() );
		assertTrue( folder1.exists() );
		assertEquals( "File 1.1 Version 1", FileUtil.load( file1_1 ).trim() );
		assertEquals( "File 1.2 Version 1", FileUtil.load( file1_2 ).trim() );
		assertTrue( folder2.exists() );
		assertEquals( "File 2.1 Version 1", FileUtil.load( file2_1 ).trim() );
		assertEquals( "File 2.2 Version 1", FileUtil.load( file2_2 ).trim() );

		program.update( update2, target );
		assertEquals( "Sample 1 Version 2", FileUtil.load( sample1 ).trim() );
		assertEquals( "Sample 2 Version 2", FileUtil.load( sample2 ).trim() );
		assertTrue( folder1.exists() );
		assertEquals( "File 1.1 Version 2", FileUtil.load( file1_1 ).trim() );
		assertEquals( "File 1.2 Version 2", FileUtil.load( file1_2 ).trim() );
		assertTrue( folder2.exists() );
		assertEquals( "File 2.1 Version 2", FileUtil.load( file2_1 ).trim() );
		assertEquals( "File 2.2 Version 2", FileUtil.load( file2_2 ).trim() );
	}

	public void testSimultaneousUpdate() throws Exception {
		program = new Updater();

		LineParser parser = new LineParser( getCommandLineOutput( program, Log.INFO, "--update", "source/test/resources/update1.zip", "target/test/update", "source/test/resources/update2.zip", "target/test/update" ) );
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

		return buffer.toString( "UTF-8" );
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

		assertEquals( TextUtil.pad( 60, '-' ), parser.next() );
		assertEquals( "Escape Updater " + release.getVersion().toHumanString(), parser.next() );
		assertEquals( "(C) 2010-" + currentYear + " Parallel Symmetry All rights reserved.", parser.next() );
		assertEquals( "", parser.next() );
		assertEquals( "Escape Updater comes with ABSOLUTELY NO WARRANTY. This is open software, and", parser.next() );
		assertEquals( "you are welcome to redistribute it under certain conditions.", parser.next() );
		assertEquals( "", parser.next() );
	}

	private void assertCommandLineVersion( LineParser parser ) throws Exception {
		assertEquals( "Version: " + program.getRelease().toString(), parser.next() );
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
