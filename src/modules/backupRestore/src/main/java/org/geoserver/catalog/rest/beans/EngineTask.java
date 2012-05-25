package org.geoserver.catalog.rest.beans;

import java.io.Serializable;

import com.thoughtworks.xstream.annotations.XStreamAlias;

public class EngineTask implements Serializable {
	@XStreamAlias("Path")
	private String path;
	
	@XStreamAlias("Status")
	private Status status;
	
	@XStreamAlias("Progress")
	private float progress;
	
	public enum Status {
		FAILED,
		WAITING,
		RUNNING,
		COMPLETE
	}

	/**
	 * @return the path
	 */
	public synchronized final String getPath() {
		return path;
	}

	/**
	 * @param path the path to set
	 */
	public synchronized final void setPath(String path) {
		this.path = path;
	}

	/**
	 * @return the status
	 */
	public synchronized final Status getStatus() {
		return status;
	}

	/**
	 * @param status the status to set
	 */
	public synchronized final void setStatus(Status status) {
		this.status = status;
	}

	/**
	 * @return the progress
	 */
	public synchronized final float getProgress() {
		return progress;
	}

	/**
	 * @param progress the progress to set
	 */
	public synchronized final void setProgress(float progress) {
		this.progress = progress;
	}

	public EngineTask() {
		
	}
	
	public EngineTask(String path, Status status, float progress) {
		super();
		this.path = path;
		this.status = status;
		this.progress = progress;
	}
	
}
