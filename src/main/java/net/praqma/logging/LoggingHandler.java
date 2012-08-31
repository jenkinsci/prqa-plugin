package net.praqma.logging;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.StreamHandler;

public class LoggingHandler extends StreamHandler {

	private int threadId;
	private Set<LoggingTarget> targets = new HashSet<LoggingTarget>();
	private OutputStream out;

	public LoggingHandler( OutputStream fos, Formatter formatter ) {
		super( fos, formatter );
		this.out = fos;
		
		this.threadId = (int) Thread.currentThread().getId();
		out = new PrintStream( fos, true );
	}
	
	public OutputStream getOut() { 
		return out;
	}
	
	/*
	public void addTarget( String name, String level ) {
		targets.add( new LoggingTarget( name, level ) );
	}
	
	public void addTarget( LoggingTarget target ) {
		targets.add( target );
	}
	
	public void addTarget( Set<LoggingTarget> targets ) {
		targets.addAll( targets );
	}
	*/
	
	public void addTarget( LoggingTarget target ) {
		targets.add( target );
		
		/* Creating or updating existing loggers */
		Logger logger = LogManager.getLogManager().getLogger( target.getName() );
		if( logger != null ) {
			logger.setLevel( Level.ALL );
			logger.setUseParentHandlers( false );
			logger.addHandler( this );
		} else {
			Logger nlogger = Logger.getLogger( target.getName() );
			nlogger.setLevel( Level.ALL );
			nlogger.setUseParentHandlers( false );
			nlogger.addHandler( this );
		}
	}
	
	public void addTargets( List<LoggingTarget> targets ) {
		for( LoggingTarget t : targets ) {
			addTarget( t );
		}
	}
	
	public void removeTargets() {
		for( LoggingTarget t : targets ) {
			Logger logger = LogManager.getLogManager().getLogger( t.getName() );
			if( logger != null ) {
				logger.removeHandler( this );
			} else {
				Logger nlogger = Logger.getLogger( t.getName() );
				nlogger.removeHandler( this );
			}
		}
	}

	@Override
	public void publish( LogRecord logRecord ) {
		if( threadId == Thread.currentThread().getId() && checkTargets( logRecord ) ) {
			super.publish( logRecord );
		} else {
			/* No op, not same thread */
		}
	}
	
	private boolean checkTargets( LogRecord lr ) {
		for( LoggingTarget target : targets ) {
			if( checkTarget( target, lr ) ) {
				return true;
			}
		}
		
		return false;
	}
	
	private boolean checkTarget( LoggingTarget target, LogRecord lr ) {

		if( lr.getLevel().intValue() < target.getLogLevel() ) {
			return false;
		}
		
		if( target.getName() == null || !lr.getLoggerName().startsWith( target.getName() ) ) {
			return false;
		}

		String rest = lr.getLoggerName().substring( target.getName().length() );
		if( rest.length() == 0 || rest.startsWith( "." ) ) {
			return true;
		}
		
		return false;
	}
}
