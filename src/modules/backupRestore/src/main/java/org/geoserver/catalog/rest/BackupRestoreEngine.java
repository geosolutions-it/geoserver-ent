package org.geoserver.catalog.rest;

import it.geosolutions.tools.commons.listener.DefaultProgress;
import it.geosolutions.tools.commons.listener.Progress;
import it.geosolutions.tools.io.file.CopyTree;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.geoserver.catalog.rest.beans.EngineTask;
import org.geoserver.config.GeoServer;
import org.springframework.beans.factory.DisposableBean;

public final class BackupRestoreEngine implements DisposableBean {
	private final ExecutorService copyExecutor;
	private final ExecutorService engineExecutor;
	private final FileFilter filter;
	private final EngineTask status;
	
	private final Progress<String> copyProgress;

	public BackupRestoreEngine(final ExecutorService es, FileFilter filter)
			throws IllegalArgumentException {
		if (es == null || es.isShutdown())
			throw new IllegalArgumentException("Invalid executor service");
		
		this.copyExecutor = es;

		this.engineExecutor = Executors.newSingleThreadExecutor();
		
		this.filter=filter;
		
		copyProgress=new DefaultProgress("BackupRestore"){
			private float progress;
			private boolean running=false;
			@Override
			public void onStart(){
				running=true;
			}
			@Override
			public void onCompleted(){
				running=false;
			}
			
			public boolean isStarted(){
				return running;
			}
			public float getProgress(){
				return progress;
			}
			
			@Override
			public void onUpdateProgress(float percent) {
				this.progress=percent;
				super.onUpdateProgress(percent);
			}
		};
		
		status=new EngineTask();
	}
	
	
	private void restore(final GeoServer geoServer) throws Exception{
		/**
		 * cp source -> d1
		 * mv dcorrent -> dold
		 * [5:35:17 PM] Andrea Aime: mv d1 -> dcorrent
		 * [5:35:20 PM] Andrea Aime: rm dold
		 */
		geoServer.reload();
	}
	
	private int backup(final File sourceDir, final File destDir, final FileFilter filter) throws IOException {
		
		final CompletionService<File> cs=new ExecutorCompletionService<File>(copyExecutor);
		CopyTree ct= null;
		
		if (filter!=null)
			ct=new CopyTree(filter, cs, sourceDir, destDir);
		else
			ct=new CopyTree(this.filter, cs, sourceDir, destDir);
		
		// TODO needed?
		// ct.addCollectingListener(listener);
		
		ct.addCopyListener(this.copyProgress);
		
		return ct.copy();
	}

	@Override
	public void destroy() throws Exception {

		if (copyExecutor != null) {
			try {
				copyExecutor.shutdown();
			} catch (Exception e) {
			}
		}
		if (engineExecutor != null) {
			try {
				engineExecutor.shutdown();
			} catch (Exception e) {
			}
		}
	}

}
