package net.praqma.logging;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import hudson.model.Actionable;
import hudson.remoting.VirtualChannel;

public class RemoteTest extends LoggingFileCallable<Boolean> {

	public RemoteTest( Actionable a ) {
		super( a );
	}

	@Override
	public Boolean perform( File workspace, VirtualChannel channel ) throws IOException, InterruptedException {

		Logger logger = Logger.getLogger( "test.logger.remote" );
		
		System.out.println( "LEVEL: " + logger.getLevel() );
		
		logger.finest( "finest from LoggingFileCallable" );
		logger.finer( "finer from LoggingFileCallable" );
		logger.fine( "fine from LoggingFileCallable" );
		logger.config( "config from LoggingFileCallable" );
		logger.info( "info from LoggingFileCallable" );
		logger.warning( "warning from LoggingFileCallable" );
		logger.severe( "severe from LoggingFileCallable" );

		return true;
	}

}
