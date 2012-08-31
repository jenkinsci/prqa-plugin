package net.praqma.logging;

import java.io.IOException;
import java.util.logging.Logger;

import net.sf.json.JSONObject;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;

public class LoggingPublisher extends Recorder {
	
	private static Logger logger = Logger.getLogger( LoggingPublisher.class.getName() );
	
	@DataBoundConstructor
	public LoggingPublisher() {
		
	}

	@Override
	public BuildStepMonitor getRequiredMonitorService() {
		return BuildStepMonitor.BUILD;
	}
	
	@Override
	public boolean perform( AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener ) throws InterruptedException, IOException {

		int tid = (int) Thread.currentThread().getId();
		System.out.println( "publisher thread " + tid );
		
		Logger logger = Logger.getLogger( "Wolles.logger" );
		logger.fine( "I'm fine" );
		logger.severe( "I'm severe" );
		
		Logger logger2 = Logger.getLogger( "snade.logger" );
		logger2.fine( "I'm fine2" );
		logger2.severe( "I'm severe2" );
		
		try {
			FilePath workspace = build.getWorkspace();
			workspace.act( new RemoteTest( build ) );
			//workspace.actAsync( new RemoteTest( build ) ).get();
		} catch( Exception e ) {
			ExceptionUtils.printRootCauseStackTrace( e, listener.getLogger() );
		}
		
		return true;
		
	}

	
	//@Extension
	public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

		public DescriptorImpl() {
			super( LoggingPublisher.class );
			load();
		}
		
		@Override
		public LoggingPublisher newInstance( StaplerRequest req, JSONObject data ) {
			return new LoggingPublisher();
		}

		@Override
		public String getDisplayName() {
			return "Logger test";
		}

		@Override
		public boolean isApplicable( Class<? extends AbstractProject> arg0 ) {
			// TODO Auto-generated method stub
			return true;
		}

	}


}
