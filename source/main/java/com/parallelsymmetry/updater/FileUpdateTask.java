package com.parallelsymmetry.updater;

import com.parallelsymmetry.utility.FileUtil;
import com.parallelsymmetry.utility.HashUtil;
import com.parallelsymmetry.utility.IoUtil;
import com.parallelsymmetry.utility.log.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

public class FileUpdateTask implements UpdateTask {

	private static final String DEL_SUFFIX = ".del";

	private static final String ADD_SUFFIX = ".add";

	private File source;

	private File target;

	public FileUpdateTask( File source, File target ) {
		this.source = source;
		this.target = target;
	}

	public boolean needsElevation() {
		return target.exists() && !FileUtil.isWritable( target );
	}

	public void execute() throws Throwable {
		if( !source.exists() ) throw new IllegalArgumentException( "Source parameter not found: " + source );
		if( !target.exists() ) throw new IllegalArgumentException( "Target parameter not found: " + target );
		if( !target.isDirectory() ) throw new IOException( "Target must be a folder: " + target );

		Log.write( Log.TRACE, "Staging: " + target );

		try {
			stage( source, target );
		} catch( ZipException exception ) {
			throw new IOException( "Source not a valid zip file: " + source );
		} catch( Throwable throwable ) {
			Log.write( Log.WARN, throwable.getMessage() );
			Log.write( Log.WARN, "Reverting: " + target );
			revert( target, target );
			throw throwable;
		}

		Log.write( Log.TRACE, "Committing: " + target );
		commit( target, target );

		//source.renameTo( new File( source.getAbsolutePath() + ".old" ) );

		Log.write( "Successful update: " + source );
	}

	private void stage( File source, File target ) throws IOException {
		Log.write( Log.DEBUG, "Staging: " + source.getName() + " to " + target + "..." );

		ZipFile zip = new ZipFile( source );

		try {
			Enumeration<? extends ZipEntry> entries = zip.entries();
			while( entries.hasMoreElements() ) {
				ZipEntry entry = entries.nextElement();
				if( !stage( zip.getInputStream( entry ), target, entry.getName() ) )
					throw new RuntimeException( "Could not stage: " + new File( target, entry.getName() ) );
			}
		} finally {
			if( zip != null ) zip.close();
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
		// Commit staged changes.
		if( target.isDirectory() ) {
			File[] files = target.listFiles();
			for( File file : files ) {
				commit( root, file );
			}
		} else {
			if( target.getName().endsWith( ADD_SUFFIX ) ) {
				String sourceHash = HashUtil.hash( target );
				File file = FileUtil.removeExtension( target );
				target.renameTo( file );
				String targetHash = HashUtil.hash( file );
				if( !targetHash.equals( sourceHash ) )
					throw new RuntimeException( "Hash code mismatch commiting file: " + file );
				Log.write( Log.TRACE, "Commit: " + relativize( root, file ) );
			} else if( target.getName().endsWith( DEL_SUFFIX ) ) {
				File file = FileUtil.removeExtension( target );
				target.delete();
				if( !file.exists() )
					Log.write( Log.TRACE, "Remove: " + relativize( root, FileUtil.removeExtension( target ) ) );
			}
		}
	}

	private void revert( File root, File target ) {
		// Revert staged changes.
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

}
