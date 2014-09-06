package com.parallelsymmetry.updater;

import com.parallelsymmetry.utility.FileUtil;

public class FileUpdaterTaskTest extends BaseTestCase {

	public void testExecute() throws Throwable {
		new FileUpdateTask( update1, target ).execute();
		assertEquals( "Sample 1 Version 1", FileUtil.load( sample1 ).trim() );
		assertEquals( "Sample 2 Version 1", FileUtil.load( sample2 ).trim() );
		assertTrue( folder1.exists() );
		assertEquals( "File 1.1 Version 1", FileUtil.load( file1_1 ).trim() );
		assertEquals( "File 1.2 Version 1", FileUtil.load( file1_2 ).trim() );
		assertTrue( folder2.exists() );
		assertEquals( "File 2.1 Version 1", FileUtil.load( file2_1 ).trim() );
		assertEquals( "File 2.2 Version 1", FileUtil.load( file2_2 ).trim() );

		new FileUpdateTask( update2, target ).execute();
		assertEquals( "Sample 1 Version 2", FileUtil.load( sample1 ).trim() );
		assertEquals( "Sample 2 Version 2", FileUtil.load( sample2 ).trim() );
		assertTrue( folder1.exists() );
		assertEquals( "File 1.1 Version 2", FileUtil.load( file1_1 ).trim() );
		assertEquals( "File 1.2 Version 2", FileUtil.load( file1_2 ).trim() );
		assertTrue( folder2.exists() );
		assertEquals( "File 2.1 Version 2", FileUtil.load( file2_1 ).trim() );
		assertEquals( "File 2.2 Version 2", FileUtil.load( file2_2 ).trim() );
	}

}
