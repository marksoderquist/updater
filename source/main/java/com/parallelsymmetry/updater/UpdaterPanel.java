package com.parallelsymmetry.updater;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import java.awt.*;

public class UpdaterPanel extends Box {

	private static final long serialVersionUID = -733636367053486845L;

	private static final int PAD = 5;

	private JLabel message;

	private JProgressBar progress;

	private JLabel task;

	public UpdaterPanel() {
		super( BoxLayout.Y_AXIS );

		message = new JLabel();
		message.setAlignmentX( 0f );

		progress = new JProgressBar();
		progress.setAlignmentX( 0f );

		task = new JLabel();
		task.setBorder( new EtchedBorder(  ) );
		task.setFont( task.getFont().deriveFont( Font.PLAIN ) );

		setBorder( new EmptyBorder( PAD, PAD, PAD, PAD ) );

		add( Box.createVerticalGlue() );
		add( message );
		add( Box.createVerticalStrut( PAD ) );
		add( progress );
		add( Box.createVerticalStrut( PAD ) );
		add( task );
		add( Box.createVerticalGlue() );
	}

	public void setMessage( String message ) {
		this.message.setText( message );
	}

	public void setTask( String task ) {
		this.task.setText( task );
	}

	public void setProgressMin( int min ) {
		progress.setMinimum( min );
	}

	public void setProgressMax( int max ) {
		progress.setMaximum( max );
	}

	public int getProgress() {
		return progress.getValue();
	}

	public void setProgress( int value ) {
		progress.setValue( value );
	}

	public Dimension getMinimumSize() {
		return getPreferredSize();
	}

	public Dimension getPreferredSize() {
		Dimension messageSize = message.getPreferredSize();
		Dimension progressSize = progress.getPreferredSize();
		Dimension taskSize = task.getPreferredSize();
		return new Dimension( Math.max( messageSize.width, progressSize.width ) * 2, 4 * PAD + messageSize.height + progressSize.height + taskSize.height );
	}

}
