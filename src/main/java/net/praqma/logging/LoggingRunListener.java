package net.praqma.logging;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.logging.Handler;
import java.util.logging.Logger;

import hudson.Extension;
import hudson.matrix.MatrixRun;
import hudson.model.TaskListener;
import hudson.model.Job;
import hudson.model.Run;
import hudson.model.listeners.RunListener;

@Extension
public class LoggingRunListener extends RunListener<Run> {

	public LoggingRunListener() {
		super( Run.class );
	}

	@Override
	public void onStarted( Run r, TaskListener listener ) {

		if( r == null ) {
			throw new IllegalArgumentException( "A build object must be set." );
		}

		Job job;
		if( r instanceof MatrixRun ) {
			job = ( (MatrixRun) r ).getParentBuild().getParent();
		} else {
			job = r.getParent();
		}

		LoggingJobProperty prop = (LoggingJobProperty) job.getProperty( LoggingJobProperty.class );
		if( prop != null ) {

			FileOutputStream fos;
			try {
				File file = new File( r.getRootDir(), "debug" );
				fos = new FileOutputStream( file );
				LoggingAction action = new LoggingAction( fos, prop.getTargets() );

				r.addAction( action );

				/* Get handler */
				LoggingHandler handler = LoggingUtils.createHandler( fos );
				action.setHandler( handler );
				
				handler.addTargets( prop.getTargets() );
				
			} catch( FileNotFoundException e ) {
				e.printStackTrace();
			}

		}

	}

	@Override
	public void onCompleted( Run r, TaskListener listener ) {
		
		LoggingAction action = r.getAction( LoggingAction.class );
		
		if( action != null ) {
			LoggingHandler handler = action.getHandler();
			handler.removeTargets();
			
			handler.flush();
			handler.close();

			try {
				action.getOut().flush();
				action.getOut().close();
			} catch( IOException e ) {
				listener.getLogger().println( "Failed to tear down logger: " + e.getMessage() );
				e.printStackTrace();
			}
			
		}
	}

}
