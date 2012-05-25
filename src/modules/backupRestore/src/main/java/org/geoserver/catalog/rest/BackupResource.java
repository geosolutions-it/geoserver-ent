package org.geoserver.catalog.rest;

import java.io.FileFilter;
import java.util.concurrent.ExecutorService;

import org.geoserver.config.GeoServer;
import org.geoserver.rest.util.RESTUtils;
import org.restlet.resource.Resource;

public class BackupResource extends Resource {
	
	private final GeoServer geoServer;
    private final BackupRestoreEngine engine;
    private final ExecutorService es;
	private final FileFilter filter;
	
	public BackupResource(GeoServer geoServer, BackupRestoreEngine engine, FileFilter filter, ExecutorService es) {
		this.geoServer = geoServer;
        this.engine=engine;
        this.es=es;
        this.filter=filter;
	}

	@Override
	public void handleGet(){
		
	}
	
	@Override
	public void handleDelete(){
		
	}
	
	@Override
	public void handlePut(){
		
	}
}
