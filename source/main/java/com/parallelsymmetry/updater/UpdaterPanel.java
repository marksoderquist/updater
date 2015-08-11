package com.parallelsymmetry.updater;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.border.EmptyBorder;

public class UpdaterPanel extends Box {

	private static final long serialVersionUID = -733636367053486845L;

	private static final int PAD = 5;

	private JLabel message;

	private JProgressBar progress;

	public UpdaterPanel() {
		super( BoxLayout.Y_AXIS );

		message = new JLabel();
		message.setAlignmentX( 0.5f );
		message.setHorizontalAlignment( JLabel.CENTER );

		progress = new JProgressBar();
		progress.setAlignmentX( 0.5f );

		setBorder( new EmptyBorder( PAD, PAD, PAD, PAD ) );

		add( Box.createVerticalGlue() );
		add( message, BorderLayout.CENTER );
		add( Box.createVerticalStrut( PAD ) );
		add( progress, BorderLayout.SOUTH );
		add( Box.createVerticalGlue() );
	}

	public void setMessage( String message ) {
		this.message.setText( message );
	}

	public void setProgressMin( int min ) {
		progress.setMinimum( min );
	}

	public void setProgressMax( int max ) {
		progress.setMaximum( max );
	}

	public void setProgress( int value ) {
		progress.setValue( value );
	}

	public int getProgress() {
		return progress.getValue();
	}
	
	public Dimension getMinimumSize() {
		Dimension messageSize = message.getPreferredSize();
		Dimension progressSize = progress.getPreferredSize();
		return new Dimension( Math.max( messageSize.width, progressSize.width ), 3 * PAD + messageSize.height + progressSize.height );
	}

	public Dimension getPreferredSize() {
		Dimension messageSize = message.getPreferredSize();
		Dimension progressSize = progress.getPreferredSize();
		return new Dimension( Math.max( messageSize.width, progressSize.width ) * 2, 3 * PAD + messageSize.height + progressSize.height );
	}

}
