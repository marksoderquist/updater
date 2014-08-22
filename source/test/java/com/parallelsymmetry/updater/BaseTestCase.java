package com.parallelsymmetry.updater;

import java.io.File;

import com.parallelsymmetry.utility.FileUtil;
import com.parallelsymmetry.utility.log.Log;

import junit.framework.TestCase;

public abstract class BaseTestCase extends TestCase {

	protected Updater updater;

	protected File source = new File( "source/test/resources" );

	protected File target = new File( "target/test/update" );

	protected File sample1 = new File( target, "sample.1.txt" );

	protected File sample2 = new File( target, "sample.2.txt" );

	protected File folder1 = new File( target, "folder1" );

	protected File file1_1 = new File( folder1, "file.1.1.txt" );

	protected File file1_2 = new File( folder1, "file.1.2.txt" );

	protected File folder2 = new File( target, "folder2" );

	protected File file2_1 = new File( folder2, "file.2.1.txt" );

	protected File file2_2 = new File( folder2, "file.2.2.txt" );

	protected File update0 = new File( source, "update0.zip" );

	protected File update1 = new File( source, "update1.zip" );

	protected File update2 = new File( source, "update2.zip" );

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

		updater = new Updater();
	}

}
