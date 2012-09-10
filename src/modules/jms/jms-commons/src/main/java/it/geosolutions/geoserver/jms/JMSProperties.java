/* Copyright (c) 2011 GeoSolutions - http://www.geo-solutions.it/.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package it.geosolutions.geoserver.jms;

import java.lang.reflect.InvocationTargetException;
import java.util.Properties;

import org.apache.commons.beanutils.BeanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Purpose of this class is to provide an easy way to share the same set of
 * properties between jms-client and jms-server instances.
 * 
 * @author Carlo Cancellieri - carlo.cancellieri@geo-solutions.it
 * 
 */
public class JMSProperties {
	final static Logger LOGGER = LoggerFactory.getLogger(JMSProperties.class);
	/**
	 * 
	 */
	private static final long serialVersionUID = -8795336081993259009L;
	
	private Properties properties;
	
	private String name;
	
	public final static String SERVER_NAME_KEY = "SERVER";
	
	@Override
	public Object clone(){
		JMSProperties props=null;
		try {
			props = new JMSProperties();
			BeanUtils.copyProperties(props, this);
			return props;
		} catch (IllegalAccessException e) {
			if (LOGGER.isErrorEnabled()){
				LOGGER.error(e.getLocalizedMessage(),e);
			}
		} catch (InvocationTargetException e) {
			if (LOGGER.isErrorEnabled()){
				LOGGER.error(e.getLocalizedMessage(),e);
			}
		}
		return null;
	}
	

	public JMSProperties() {
		properties=new Properties();
		setName(SERVER_NAME_KEY);
	}

	/**
	 * @param name
	 *            the name to set
	 */
	public final void setName(final String name) {
		this.name=name;
	}

	/**
	 * @return the name
	 */
	public final String getName() {
		return name;
	}
	
	public void put(Object key, Object value) {
		properties.put(key, value);
	}
	
	public Object get(Object key) {
		return properties.get(key);
	}
	
	public Properties getProperties(){
		return properties;
	}
	
	public void setProperties(final Properties props){
		properties=props;
	}

}
