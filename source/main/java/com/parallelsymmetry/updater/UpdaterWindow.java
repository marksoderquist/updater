package com.parallelsymmetry.updater;

import java.awt.BorderLayout;

import javax.swing.JFrame;
import javax.swing.JWindow;

import com.parallelsymmetry.utility.ui.SwingUtil;

public class UpdaterWindow extends JWindow {

	private static final long serialVersionUID = -4240269349153296803L;

	private UpdaterPanel updaterPanel;

	public UpdaterWindow() {
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
		UpdaterWindow window = new UpdaterWindow();

		window.setMessage( "Updating Program..." );
		window.setProgress( 25 );

		window.setSize( 300, 50 );
		window.setAlwaysOnTop( true );

		SwingUtil.center( window );
		window.setVisible( true );
	}

}
