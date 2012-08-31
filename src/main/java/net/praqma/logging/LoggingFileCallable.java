package net.praqma.logging;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

import hudson.FilePath.FileCallable;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Actionable;
import hudson.remoting.RemoteOutputStream;
import hudson.remoting.VirtualChannel;

public abstract class LoggingFileCallable<T> implements FileCallable<T> {

	protected LoggingStream lstream;
	private List<LoggingTarget> targets;
	private long threadId;

	public LoggingFileCallable( Actionable a ) {
		threadId = Thread.currentThread().getId();
		if( a instanceof AbstractBuild ) {
			initialize( (AbstractBuild)a );
		} else if( a instanceof AbstractProject ) {
			initialize( (AbstractProject)a );
		}
	}
	
	private void initialize( AbstractBuild<?, ?> build ) {
		LoggingAction action = build.getAction( LoggingAction.class );
		if( action != null ) {
			lstream = action.getLoggingStream();
			targets = action.getTargets();
			
			action.getHandler().flush();
		}
	}
	
	private void initialize( AbstractProject<?, ?> project ) {
		LoggingJobProperty prop = (LoggingJobProperty) project.getProperty( LoggingJobProperty.class );
		if( prop != null && prop.isPollLogging() ) {
			try {
				LoggingAction action = prop.getLoggingAction( Thread.currentThread().getId() );
				if( action != null ) {
					lstream = action.getLoggingStream();
					targets = action.getTargets();
				}
			} catch( Exception e ) {
				
			}
		}
	}

	public abstract T perform( File workspace, VirtualChannel channel ) throws IOException, InterruptedException;

	@Override
	public T invoke( File workspace, VirtualChannel channel ) throws IOException, InterruptedException {

		long currentThreadId = Thread.currentThread().getId();
        
        if( lstream != null ) {
            new PrintStream( lstream.getOutputStream() ).println( "THREAD: " + Thread.currentThread().getName() + "::" + Thread.currentThread().getId() );
            new PrintStream( lstream.getOutputStream() ).println( "STREAM: " + lstream.getOutputStream() );
            new PrintStream( lstream.getOutputStream() ).println( "REMOTE: " + isRemote() );
            new PrintStream( lstream.getOutputStream() ).println( "WS: " + workspace.getAbsoluteFile() );
        }
		
		/* Setup logger */
		T result = null;
		/* Do this if remote or on another stream than caller */
		if( lstream != null && ( isRemote() || ( !isRemote() && threadId != currentThreadId ) ) ) {
			LoggingHandler handler = LoggingUtils.createHandler( lstream.getOutputStream() );
			handler.addTargets( targets );
	
			try {
				result = perform( workspace, channel );
			} finally {
				/* Tear down logger */
				//LoggingUtils.removeHandler( handler );
							
				handler.removeTargets();
				
				/* If remote flush and close handler */
				try {
					handler.flush();
					handler.close();
					handler.getOut().flush();
					handler.getOut().close();
				} catch( Exception e ) {
					/* Unable to close handler */
				}
			}
		} else {
			result = perform( workspace, channel );
		}

		return result;

	}
	
	private boolean isRemote() {
		return lstream != null && lstream.getOutputStream() instanceof RemoteOutputStream;
	}

}
