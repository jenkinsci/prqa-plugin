package net.praqma.logging;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import hudson.Extension;
import hudson.model.JobProperty;
import hudson.model.JobPropertyDescriptor;
import hudson.model.Job;

public class LoggingJobProperty extends JobProperty<Job<?, ?>> {

	public static final String[] levels = { "all", "finest", "finer", "fine", "config", "info", "warning", "severe" };

	private List<LoggingTarget> targets;

	private boolean pollLogging = false;
	
	private Map<Long, LoggingHandler> pollhandler = new HashMap<Long, LoggingHandler>();

	@DataBoundConstructor
	public LoggingJobProperty( boolean pollLogging ) {
		this.pollLogging = pollLogging;
	}
	
	public LoggingHandler getPollhandler( long id ) throws IOException {
		LoggingHandler pollhandler = this.pollhandler.get( id );
		
		if( pollhandler == null ) {
			File path = new File( owner.getRootDir(), "poll-logging" );
			if( !path.exists() ) {
				path.mkdir();
			}
			File file = new File( path, "logging" );
			FileOutputStream fos = new FileOutputStream( file, true );
			pollhandler = LoggingUtils.createHandler( fos );
			
			pollhandler.addTargets( getTargets() );
			
			this.pollhandler.put( id, pollhandler );
		}
		
		return pollhandler;
	}
	
	public void resetPollhandler( long id ) {
		pollhandler.put( id, null );
	}
	
	public LoggingAction getLoggingAction( long id ) throws IOException {
		LoggingHandler handler = getPollhandler( id );
		return new LoggingAction( handler, getTargets() );
	}


	private void setTargets( List<LoggingTarget> targets ) {
		this.targets = targets;
	}

	public List<LoggingTarget> getTargets() {
		return targets;
	}

	public boolean isPollLogging() {
		return pollLogging;
	}

	@Extension
	public static class DescriptorImpl extends JobPropertyDescriptor {

		public JobProperty<?> newInstance( StaplerRequest req, JSONObject formData ) throws FormException {
			Object debugObject = formData.get( "debugLog" );

			System.out.println( formData.toString( 2 ) );

			if( debugObject != null ) {
				JSONObject debugJSON = (JSONObject) debugObject;

				boolean pollLogging = debugJSON.getBoolean( "pollLogging" );

				LoggingJobProperty instance = new LoggingJobProperty( pollLogging );

				List<LoggingTarget> targets = req.bindParametersToList( LoggingTarget.class, "logging.logger." );
				instance.setTargets( targets );

				return instance;
			}

			return null;
		}

		@Override
		public String getDisplayName() {
			return "Logging";
		}

		@Override
		public boolean isApplicable( Class<? extends Job> jobType ) {
			return true;
		}

		public String[] getLogLevels() {
			return levels;
		}

		public List<LoggingTarget> getAcceptableLoggerNames( LoggingJobProperty instance ) {
			if( instance == null ) {
				return new ArrayList<LoggingTarget>();
			} else {
				return instance.getTargets();
			}
		}

	}

	public String toString() {
		return "Logging job property, " + targets;
	}

}
