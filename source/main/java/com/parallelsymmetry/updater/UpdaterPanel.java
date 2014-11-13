package com.parallelsymmetry.updater;

import java.awt.BorderLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.border.EmptyBorder;

public class UpdaterPanel extends JPanel {

	private static final long serialVersionUID = -733636367053486845L;

	private static final int PAD = 5;

	private JLabel message;

	private JProgressBar progress;

	public UpdaterPanel() {
		message = new JLabel();
		progress = new JProgressBar();

		message.setHorizontalAlignment( JLabel.CENTER );

		setLayout( new BorderLayout( PAD, PAD ) );
		setBorder( new EmptyBorder( PAD, PAD, PAD, PAD ) );

		add( message, BorderLayout.NORTH );
		add( progress, BorderLayout.SOUTH );
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

}
