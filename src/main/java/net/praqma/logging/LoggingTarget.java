package net.praqma.logging;

import java.io.Serializable;
import java.util.logging.Level;

import org.kohsuke.stapler.DataBoundConstructor;


public class LoggingTarget implements Serializable {
	
	private String level;
	private String name;
	private int logLevel;
	
	@DataBoundConstructor
	public LoggingTarget( String name, String level ) {
		this.name = name;
		this.level = level;
		
		this.logLevel = Level.parse( level ).intValue();
	}

	public String getLevel() {
		return level;
	}

	public void setLevel( String level ) {
		this.level = level;
		this.logLevel = Level.parse( level ).intValue();
	}

	public String getName() {
		return name;
	}

	public void setName( String name ) {
		this.name = name;
	}
	
	public String toString() {
		return name + ", " + level;
	}
	
	public int getLogLevel() {
		return logLevel;
	}
}
