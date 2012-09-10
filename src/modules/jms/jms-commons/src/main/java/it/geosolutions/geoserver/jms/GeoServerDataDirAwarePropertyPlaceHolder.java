/* Copyright (c) 2011 GeoSolutions - http://www.geo-solutions.it/.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package it.geosolutions.geoserver.jms;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geoserver.config.GeoServerDataDirectory;
import org.geotools.util.logging.Logging;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
/**
 * 
 * GeoServer's specific subclass for {@link PropertyPlaceholderConfigurer} Spring bean to load properties file.
 * 
 * <p> This class is a special {@link PropertyPlaceholderConfigurer} that:
 * <ol>
 *   <li> By default load properties from a specific file inside a specific dir of the GeoServer data dir. Hence it is aware of the GeoServer data dir. This id done via setting internally {@link #setLocalOverride(boolean)} to <code>true</code> </li>
 *   <li> It still allows users to specify locations from where loading the properties. In this case we must set localovveride property to false though.
 * </ol>
 * @author Simone Giannecchini, GeoSolutions SAS.
 *
 */
public class GeoServerDataDirAwarePropertyPlaceHolder extends
		PropertyPlaceholderConfigurer {
	
	/**Logger.*/
	static final private Logger LOGGER= Logging.getLogger(GeoServerDataDirAwarePropertyPlaceHolder.class);

	/**This object mediates access to the GeoServer data directory.**/
	protected final GeoServerDataDirectory dataDirectory;
	
	/** 
	 * Constructor.
	 * 
	 * <p> Allows subclasses or users to specify from which directory which properties files to load.
	 * 
	 * @param dataDirectory a valid {@link GeoServerDataDirectory}. Cannot be <code>null</code>.
	 * @param parentDirectory An absolute path to the directory where to look for properties files.
	 * @param propertiesFiles List of names (with extension) of properties files to load.
	 */
	public GeoServerDataDirAwarePropertyPlaceHolder(final GeoServerDataDirectory dataDirectory, final String parentDirectory, final String... propertiesFiles) {
		//checks
		if(dataDirectory==null){
			throw new IllegalArgumentException("Unable to proceed with the placeholder initialization as the provided GeoServerDataDirectory is null!");
		}
		this.dataDirectory = dataDirectory;
		
		// localOverride to true, so that what we do here takes precedence over external properties
		setLocalOverride(true);
		
		// initialize
		try {
			for(String propertiesFile:propertiesFiles){
				initialize(dataDirectory.findDataDir(parentDirectory),propertiesFile);
			}
		} catch (IOException e) {
			if(LOGGER.isLoggable(Level.SEVERE)){
				LOGGER.log(Level.SEVERE,"Unable to load clustering configuration:\n"+e.getLocalizedMessage(),e);
			}
		}
	}
	

	/**
	 * Initialize the properties for this {@link PropertyPlaceholderConfigurer} with what we find inside the 
	 * data dir for GeoServer.
	 * 
	 * @param parentDirectory the directory where to search for and then load the clustering configuration.
	 * @throws IOException in case something bad happens when trying to find and load the properties.
	 */
	private void initialize(final File parentDirectory, final String propertiesFilePath) throws IOException {
		// checks on the provided directory
		if(!(parentDirectory!=null&&parentDirectory.exists()&&parentDirectory.isDirectory()&&parentDirectory.canRead()&&parentDirectory.canExecute())){
			throw new IOException("Unable to find clustering configuration. Please make sure the clustering directory exists and contains a proper jms.properties file");
		}
		
		// tries to find the properties files for JMS
		final File propertiesFile= new File(parentDirectory,propertiesFilePath);							
		Properties properties=loadClusteringProperties(propertiesFile);
		if(properties==null){
			throw new IOException("Unable to find clustering configuration. Please make sure the clustering directory exists and contains a proper jms.properties file");
		} else {
			if(LOGGER.isLoggable(Level.WARNING)){
				LOGGER.warning("Loaded properties from file "+propertiesFile.getAbsolutePath()+":\n"+properties.toString());
			}
		}
			
		// set the properties we loaded as the default one to use
		setProperties(properties);
		
	}

	/**
	 * Tries to find the provided properties file and then returns its properties.
	 * 
	 * @param propertiesFile the {@link File} to load from. Must exists and be readable.
	 * @throws IOException in case something bad happens trying to read the proeprties from the file.
	 * 
	 */
	private static Properties loadClusteringProperties(final File propertiesFile)
			throws IOException {
		// checks on provided file
		if(!(propertiesFile!=null&&propertiesFile.exists()&&propertiesFile.isFile()&&propertiesFile.canRead())){
			return null;
		}
		// load the file
		InputStream is=null;
		try{
			is= new BufferedInputStream(new FileInputStream(propertiesFile));
			final Properties properties= new Properties();
			properties.load(is);
			return properties;
		} finally {
			// close
			if(is!=null){
				try{
					is.close();
				}catch (Exception e) {
					if(LOGGER.isLoggable(Level.FINE)){
						LOGGER.log(Level.FINE,e.getLocalizedMessage(),e);
					}
				}
			}
		}
	}
}