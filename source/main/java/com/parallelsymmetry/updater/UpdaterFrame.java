package com.parallelsymmetry.updater;

import java.awt.BorderLayout;

import javax.swing.JFrame;

import com.parallelsymmetry.utility.ui.SwingUtil;

public class UpdaterFrame extends JFrame {

	private static final long serialVersionUID = -4240269349153296803L;

	private UpdaterPanel updaterPanel;

	public UpdaterFrame() {
		updaterPanel = new UpdaterPanel();
		add( updaterPanel, BorderLayout.CENTER );
	}

	public void setMessage( String message ) {
		updaterPanel.setMessage( message );
	}

	public int getProgress() {
		return updaterPanel.getProgress();
	}

	public void setProgress( int value ) {
		updaterPanel.setProgress( value );
	}

	public void setProgressMin( int min ) {
		updaterPanel.setProgressMin( min );
	}

	public void setProgressMax( int max ) {
		updaterPanel.setProgressMax( max );
	}

	public static final void main( String[] commands ) {
		UpdaterFrame frame = new UpdaterFrame();
		frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );

		frame.setMessage( "Updating Program..." );
		frame.setProgress( 25 );

		frame.pack();
		SwingUtil.center( frame );
		frame.setVisible( true );
	}

}
