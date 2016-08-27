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
		pack();
	}

	public static final void main( String[] commands ) {
		UpdaterWindow window = new UpdaterWindow();
		window.setMessage( "Updating Program..." );
		window.setAlwaysOnTop( true );
		window.setProgress( 33 );
		SwingUtil.center( window );
		window.setVisible( true );
		window.requestFocus();
	}

	public void setMessage( String message ) {
		updaterPanel.setMessage( message );
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
