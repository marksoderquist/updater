package com.parallelsymmetry.updater;

import com.parallelsymmetry.utility.ui.SwingUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class UpdaterWindow extends JDialog implements KeyListener {

	private static final long serialVersionUID = -4240269349153296803L;

	private UpdaterPanel updaterPanel;

	public UpdaterWindow() {
		updaterPanel = new UpdaterPanel();
		add( updaterPanel, BorderLayout.CENTER );
		addKeyListener( this );
	}

	public static final void main( String[] commands ) {
		UpdaterWindow window = new UpdaterWindow();
		window.setDefaultCloseOperation( JDialog.DISPOSE_ON_CLOSE );
		window.setTitle( "Perform Updater" );
		window.setStep( "Updating program..." );
		window.setTask( "Copy foo.txt..." );
		window.setAlwaysOnTop( true );
		window.pack();

		SwingUtil.center( window );
		window.setVisible( true );
		window.requestFocus();

		// Test the window
		int steps = 5;
		int tasks = 20;
		window.setProgressMax( steps * tasks );
		try {
			for( int step = 0; step < steps; step++ ) {
				window.setStep( "Running step " + (step + 1) + "..." );
				for( int task = 0; task < tasks; task++ ) {
					window.setTask( "Performing task " + (task + 1) + "..." );
					Thread.sleep( 50 );
					window.setProgress( ((step * tasks) + task) + 1 );
				}
			}
		} catch( InterruptedException exception ) {
			// Intentionally ignore exception
		}
		window.setStep( "Complete!");
		window.setTask( " " );
	}

	public void setStep( String step ) {
		updaterPanel.setStep( step );
		pack();
	}

	public void setTask( String task ) {
		updaterPanel.setTask( task );
		pack();
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

	@Override
	public void keyPressed( KeyEvent event ) {
		if( event.getKeyCode() == KeyEvent.VK_ESCAPE ) dispose();
	}

	@Override
	public void keyReleased( KeyEvent event ) {}

	@Override
	public void keyTyped( KeyEvent event ) {}

}
