/* Copyright (c) 2007 - 2012 - www.geo-solutions.it.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.catalog.rest;

import java.util.logging.Logger;

import org.geoserver.config.GeoServer;
import org.geotools.util.logging.Logging;
import org.restlet.Finder;
import org.restlet.data.Method;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Resource;

public class CatalogRestore extends Finder {

	static Logger LOGGER = Logging.getLogger("org.geoserver.catalog.rest");

	private final GeoServer geoServer;

	/**
	 * Perform a restore operation
	 * 
	 * @param geoServer
	 */
	public CatalogRestore(GeoServer geoServer) {
		this.geoServer = geoServer;
	}

	@Override
	public Resource findTarget(Request request, Response response) {
		final Method m = request.getMethod();
		if (!(m == Method.GET || m == Method.PUT || m == Method.DELETE)) {
			response.setStatus(Status.CLIENT_ERROR_METHOD_NOT_ALLOWED);
			return null;
		}
		return new RestoreResource(geoServer);
	}
}
