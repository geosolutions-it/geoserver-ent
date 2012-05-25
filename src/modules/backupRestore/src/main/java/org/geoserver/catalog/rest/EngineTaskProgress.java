package org.geoserver.catalog.rest;

import org.geoserver.catalog.rest.beans.EngineTask;
import org.geoserver.catalog.rest.beans.EngineTask.Status;

import it.geosolutions.tools.commons.listener.Progress;

public class EngineTaskProgress implements Progress<EngineTask> {
	private EngineTask task;
	
	public EngineTask getTask(){
		return task;
	}

	@Override
	public void onNewTask(EngineTask task) {
		this.task=task;
	}

	@Override
	public void onStart() {
		task.setProgress(0);
		task.setStatus(Status.WAITING);
	}

	@Override
	public void onUpdateProgress(float percent) {
		task.setProgress(percent);
		if (task.getStatus()!=Status.RUNNING){
			task.setStatus(Status.RUNNING);
		}
	}

	@Override
	public void onCompleted() {
		task.setStatus(Status.COMPLETE);
	}

	@Override
	public void onDispose() {
		
	}

	@Override
	public void onCancel() {
		task.setStatus(Status.FAILED);
	}

	@Override
	public void onWarningOccurred(String source, String location, String warning) {
		
	}

	@Override
	public void onExceptionOccurred(Throwable exception) {
		
	}

}
