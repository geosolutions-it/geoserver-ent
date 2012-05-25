package org.geoserver.catalog.rest;

import org.geoserver.config.GeoServer;
import org.restlet.resource.Resource;

public class RestoreResource extends Resource {

	private final GeoServer geoServer;
	
	public RestoreResource(GeoServer geoServer){
		this.geoServer=geoServer;
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
