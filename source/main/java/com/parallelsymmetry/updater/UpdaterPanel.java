package com.parallelsymmetry.updater;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class UpdaterPanel extends Box {

	private static final long serialVersionUID = -733636367053486845L;

	private static final int PAD = 5;

	private JLabel step;

	private JProgressBar progress;

	private JLabel task;

	public UpdaterPanel() {
		super( BoxLayout.Y_AXIS );

		step = new JLabel();
		step.setAlignmentX( 0f );

		progress = new JProgressBar();
		progress.setAlignmentX( 0f );

		task = new JLabel();
		task.setFont( task.getFont().deriveFont( Font.PLAIN ) );

		setBorder( new EmptyBorder( PAD, PAD, PAD, PAD ) );

		add( Box.createVerticalGlue() );
		add( step );
		add( Box.createVerticalStrut( PAD ) );
		add( progress );
		add( Box.createVerticalStrut( PAD ) );
		add( task );
		add( Box.createVerticalGlue() );
	}

	public void setStep( String step ) {
		SwingUtilities.invokeLater( new SetStep( step ) );
	}

	public void setTask( String task ) {
		SwingUtilities.invokeLater( new SetTask( task ) );
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
		Dimension messageSize = step.getPreferredSize();
		Dimension progressSize = progress.getPreferredSize();
		Dimension taskSize = task.getPreferredSize();
		return new Dimension( Math.max( 300, Math.max( messageSize.width, progressSize.width ) ), 4 * PAD + messageSize.height + progressSize.height + taskSize.height );
	}

	private class SetStep implements Runnable {

		private String text;

		public SetStep( String text ) {
			this.text = text;
		}

		public void run() {
			step.setText( text );
		}

	}

	private class SetTask implements Runnable {

		private String text;

		public SetTask( String text ) {
			this.text = text;
		}

		public void run() {
			task.setText( text );
		}

	}

}
