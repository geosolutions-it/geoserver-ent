/* Copyright (c) 2007 - 2012 - www.geo-solutions.it.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.catalog.rest;

import java.io.FileFilter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import org.apache.commons.io.filefilter.FileFilterUtils;
import org.geoserver.config.GeoServer;
import org.geotools.util.logging.Logging;
import org.restlet.Finder;
import org.restlet.data.Method;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Resource;

public class CatalogBackup extends Finder {

    static Logger LOGGER = Logging.getLogger("org.geoserver.catalog.rest");
    
    private final GeoServer geoServer;
    private final BackupRestoreEngine engine;
    private final ExecutorService es;
	private final FileFilter filter;
    
    /**
     * 
     * Handler GeoServer EngineTask operation
     * 
     * @param geoServer
     * @param engine
     * @param filter
     * @param es
     */
    public CatalogBackup(GeoServer geoServer, BackupRestoreEngine engine, FileFilter filter, ExecutorService es) {
        this.geoServer = geoServer;
        this.engine=engine;
        this.es=es;
        this.filter=filter;
    }
    
    public CatalogBackup(GeoServer geoServer, BackupRestoreEngine engine, final int maxThreads) {
        this.geoServer = geoServer;
        this.engine=engine;
        this.es=Executors.newFixedThreadPool(maxThreads);
        // all file and dirs
        this.filter=FileFilterUtils.or(FileFilterUtils.fileFileFilter(),FileFilterUtils.directoryFileFilter());
    }

    @Override
    public Resource findTarget(Request request, Response response) {
    	final Method m=request.getMethod();
    	if (!(m==Method.GET|| m==Method.PUT || m==Method.DELETE)) {
    		response.setStatus(Status.CLIENT_ERROR_METHOD_NOT_ALLOWED);
            return null;
    	}
    	return new BackupResource(geoServer,engine,filter,es);
    }
}
