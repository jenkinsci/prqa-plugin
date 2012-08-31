package net.praqma.logging;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.List;

import hudson.model.Action;

public class LoggingAction implements Action {

	protected transient LoggingStream loggingStream;
	protected transient List<LoggingTarget> targets;
	protected transient LoggingHandler handler;

	public LoggingAction( FileOutputStream out, List<LoggingTarget> targets ) {
		loggingStream = new LoggingStream( out );
		this.targets = targets;
	}
	
	public LoggingAction( LoggingHandler handler, List<LoggingTarget> targets ) {
		loggingStream = new LoggingStream( handler.getOut() );
		this.targets = targets;
		this.handler = handler;
	}

	public OutputStream getOut() {
		return loggingStream.getOutputStream();
	}

	public LoggingStream getLoggingStream() {
		return loggingStream;
	}
	
	public List<LoggingTarget> getTargets() {
		return targets;
	}
	
	public LoggingHandler getHandler() {
		return handler;
	}

	public void setHandler( LoggingHandler handler ) {
		this.handler = handler;
	}

	@Override
	public String getDisplayName() {
		return null;
	}

	@Override
	public String getIconFileName() {
		return null;
	}

	@Override
	public String getUrlName() {
		return null;
	}
	
	public String toString() {
		return "Targets: " + targets + ", " + handler;
	}
}
