package net.praqma.logging;

import java.text.MessageFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class PraqmaticFormatter extends Formatter {
	
	public String format = "";
	
	private static final int width = 8;
	private boolean enableLineNumbers = true;
	
	private MessageFormat messageFormat = new MessageFormat( "{3,date,HH:mm:ss} [{1}]{5} {6}.{7}, {99}: {4} \n" );
	
	public PraqmaticFormatter() {}
	
	public PraqmaticFormatter( String format ) {
		messageFormat = new MessageFormat( format );
		this.format = format;
	}

	@Override
	public String format( LogRecord record ) {
		
		Object[] args = new Object[100];
		args[0] = record.getLoggerName();
		args[1] = record.getLevel();
		args[2] = Thread.currentThread().getName();
		args[3] = new Date( record.getMillis() );
		args[4] = record.getMessage();
		int w = width - record.getLevel().getName().length();
		if( w > 0 ) {
			args[5] = new String( new char[w] ).replace( "\0", " " );
		} else {
			args[5] = "";
		}
		
		args[6] = record.getSourceClassName();
		args[7] = record.getSourceMethodName();
		
		
		if( enableLineNumbers ) {
			try {
				args[99] = Thread.currentThread().getStackTrace()[8].getLineNumber();
			} catch( Exception e ) {
				args[99] = -1;
			}
		} else {
			args[99] = "?";
		}
		
		return messageFormat.format( args );
	}

}
