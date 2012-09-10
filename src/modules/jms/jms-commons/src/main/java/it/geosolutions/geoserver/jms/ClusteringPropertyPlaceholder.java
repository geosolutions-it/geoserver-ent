/* Copyright (c) 2011 GeoSolutions - http://www.geo-solutions.it/.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package it.geosolutions.geoserver.jms;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.platform.GeoServerExtensions;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;

/**
 * Custom {@link PropertyPlaceholderConfigurer} that tries to get access to the
 * geoserver data dir for reading the proerties file for configuring JMS.
 * 
 * <p> 
 * The default name for the directory where to place the clustering configuration 
 * inside the GeoServer data directory is <b>clustering</b> and the file
 * is <b>clustering.properties</b>.
 * 
 * <p> The name for the properties file can be change via the <b>-Dorg.geoserver.clustering.filename</b>
 * switch. 
 * 
 * @author Simone Giannecchini, GeoSolutions SAS
 * 
 */
public class ClusteringPropertyPlaceholder extends
		GeoServerDataDirAwarePropertyPlaceHolder {
	
	/** The name of the directory where we can find the clustering the data directory.**/
	public final static String CLUSTERING_CONFIG_DIRECTORY= "clustering";
	
	/** The name of the directory where we can find the clustering config.**/
	public final static String DEFAULT_CLUSTERING_CONFIG_FILE= "clustering";
	
	/** The name of the properties file where we can find the clustering config.**/
	public final static String CLUSTERING_CONFIG_FILE_ENV_KEY= "org.geoserver.clustering.filename";
	
	/**The name for the properties file to load.**/
	public final static String CLUSTERING_CONFIG_FILE;
	static{
		String prop=GeoServerExtensions.getProperty(CLUSTERING_CONFIG_FILE_ENV_KEY);
		if(prop!=null&&prop.length()>0){
			CLUSTERING_CONFIG_FILE=prop;
		} else {
			CLUSTERING_CONFIG_FILE=DEFAULT_CLUSTERING_CONFIG_FILE+".properties";
		}
	}
	
	/**
	 * Constructor.
	 * 
	 * <p> Builds up a {@link ClusteringPropertyPlaceholder} that first of all reads properties from a specific 
	 * directory inside the GeoServer data directory.
	 * 
	 * @param dataDirectory the {@link GeoServerDataDirectory} class that holds up info about the GeoServer data directory.
	 */
	public ClusteringPropertyPlaceholder(GeoServerDataDirectory dataDirectory) {
		super(dataDirectory,CLUSTERING_CONFIG_DIRECTORY,CLUSTERING_CONFIG_FILE);
	}
	


}
