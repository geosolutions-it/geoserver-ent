/* Copyright (c) 2011 GeoSolutions - http://www.geo-solutions.it/.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package it.geosolutions.geoserver.jms.impl;

import it.geosolutions.geoserver.jms.JMSProperties;
import it.geosolutions.geoserver.jms.JMSPublisher;
import it.geosolutions.geoserver.jms.events.ToggleProducer.ToggleEvent;
import it.geosolutions.geoserver.jms.impl.events.RestDispatcherCallback;
import it.geosolutions.geoserver.jms.impl.events.configuration.JMSGlobalModifyEvent;
import it.geosolutions.geoserver.jms.impl.events.configuration.JMSServiceModifyEvent;
import it.geosolutions.geoserver.jms.impl.handlers.DocumentFile;
import it.geosolutions.geoserver.jms.impl.utils.BeanUtils;

import java.io.File;
import java.util.List;

import javax.jms.JMSException;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogException;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.event.CatalogAddEvent;
import org.geoserver.catalog.event.CatalogListener;
import org.geoserver.catalog.event.CatalogModifyEvent;
import org.geoserver.catalog.event.CatalogPostModifyEvent;
import org.geoserver.catalog.event.CatalogRemoveEvent;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.config.ConfigurationListener;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.LoggingInfo;
import org.geoserver.config.ServiceInfo;
import org.geoserver.platform.ContextLoadedEvent;
import org.restlet.data.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.jms.core.JmsTemplate;
import org.vfny.geoserver.global.GeoserverDataDirectory;
/**
 * JMS MASTER (Producer)
 * Listener used to send GeoServer Catalog events over the JMS channel.
 * @see {@link JMSListener} 
 * 
 * @author Carlo Cancellieri - carlo.cancellieri@geo-solutions.it
 * @author Simone Giannecchini, GeoSolutions SAS
 * 
 */
public class ClusteringProducer implements CatalogListener, ConfigurationListener,ApplicationListener{

	private final static Logger LOGGER = LoggerFactory
	.getLogger(ClusteringProducer.class);
	private final JmsTemplate jmsTemplate;
	/**
	 * This will be set to false: - until the GeoServer context is initialized -
	 * if this instance of geoserver act as pure slave
	 */
	private Boolean producerEnabled = false;
	private final JMSProperties properties;

	public ClusteringProducer(JMSProperties properties, JmsTemplate jmsTemplate,
			final Catalog catalog,
			final GeoServer geoserver) {

		this.properties = properties;
		this.jmsTemplate = jmsTemplate;

		
		// listen to catalog changes
		catalog.addListener(this);
		
		// add this as geoserver listener
		geoserver.addListener(this);


	}

	@Override
	public void handleAddEvent(CatalogAddEvent event) throws CatalogException {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Incoming event of type "
					+ event.getClass().getSimpleName() + " from Catalog");
		}
	
		// skip incoming events if producer is not Enabled
		if (!producerEnabled) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("skipping incoming event: context is not initted");
			}
			return;
		}
	
		final JMSPublisher publisher = new JMSPublisher();
		
		// update properties
		JMSProperties options=updateProperties();
		
		try {
			// check if we may publish also the file
			final CatalogInfo info = event.getSource();
			if (info instanceof StyleInfo) {
				final String fileName = File.separator + "styles"
						+ File.separator + ((StyleInfo) info).getFilename();
				final File styleFile = new File(GeoserverDataDirectory
						.getGeoserverDataDirectory().getCanonicalPath(),
						fileName);				
				
				// transmit the file
				publisher.publish(jmsTemplate, options, new DocumentFile(
						styleFile));
			}
	
			// propagate the event
			publisher.publish(jmsTemplate, options, event);
		} catch (Exception e) {
			if (LOGGER.isErrorEnabled()) {
				LOGGER.error(e.getLocalizedMessage());
			}
			final CatalogException ex = new CatalogException(e);
			throw ex;
		}
	}

	@Override
	public void handleModifyEvent(CatalogModifyEvent event)
			throws CatalogException {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Incoming message event of type "
					+ event.getClass().getSimpleName() + " from Catalog");
		}
	
		// skip incoming events until context is loaded
		if (!producerEnabled) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("skipping incoming event: context is not initted");
			}
			return;
		}
		
		// update properties
		JMSProperties options=updateProperties();
	
		final JMSPublisher publisher = new JMSPublisher();
		try {
			// check if we may publish also the file
			final CatalogInfo info = event.getSource();
			if (info instanceof StyleInfo) {
				// build local datadir file style path
				final String fileName = File.separator + "styles"
						+ File.separator + ((StyleInfo) info).getFilename();
				final File styleFile = new File(GeoserverDataDirectory
						.getGeoserverDataDirectory().getCanonicalPath(),
						fileName);
				
				// publish the style xml document
				publisher.publish(jmsTemplate, options, new DocumentFile(
						styleFile));
			}
	
			// propagate the event
			publisher.publish(jmsTemplate, options, event);
	
		} catch (Exception e) {
			if (LOGGER.isErrorEnabled()) {
				LOGGER.error(e.getLocalizedMessage());
			}
			final CatalogException ex = new CatalogException(e);
			throw ex;
		}
	}

	@Override
	public void handlePostModifyEvent(CatalogPostModifyEvent event)
			throws CatalogException {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Incoming message event of type "
					+ event.getClass().getSimpleName() + " from Catalog");
		}
	
		// EAT EVENT
		// this event should be generated locally (to slaves) by the catalog
		// itself
	}

	@Override
	public void handleRemoveEvent(CatalogRemoveEvent event)
			throws CatalogException {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Incoming message event of type "
					+ event.getClass().getSimpleName() + " from Catalog");
		}
	
		// skip incoming events until context is loaded
		if (!producerEnabled) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("skipping incoming event: context is not initted");
			}
			return;
		}
		
		// update properties
		JMSProperties options=updateProperties();
		
		final JMSPublisher publisher = new JMSPublisher();
		try {
			publisher.publish(jmsTemplate, options, event);
		} catch (JMSException e) {
			if (LOGGER.isErrorEnabled()) {
				LOGGER.error(e.getLocalizedMessage());
			}
			throw new CatalogException(e);
		}
	}

	/**
	 * This should be called before each message send to 
	 * add options (coming form the dispatcher callback) to the message 
	 * @return a copy of the properties object updated with others
	 * options coming from the RestDispatcherCallback
	 * TODO use also options coming from the the GUI DispatcherCallback
	 */
	private JMSProperties updateProperties(){
		// append options
		JMSProperties options=(JMSProperties)properties.clone();
		// get options from rest callback
		List<Parameter> p=RestDispatcherCallback.get();
		if (p!=null){
			for (Parameter par:p) {
				options.put(par.getName(),par.getValue().toString());
			}
		}
		return options;
	}

	@Override
	public void handleGlobalChange(GeoServerInfo global,
			List<String> propertyNames, List<Object> oldValues,
			List<Object> newValues) {
	
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Incoming event");
		}
	
		// skip incoming events if producer is not Enabled
		if (!producerEnabled) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("skipping incoming event: context is not initted");
			}
			return;
		}
	
		final JMSPublisher publisher = new JMSPublisher();
		try {
	
			// propagate the event
			publisher.publish(jmsTemplate, properties,
					new JMSGlobalModifyEvent(ModificationProxy.unwrap(global),
							propertyNames, oldValues, newValues));
	
		} catch (JMSException e) {
			if (LOGGER.isErrorEnabled()) {
				LOGGER.error(e.getLocalizedMessage(), e);
			}
		}
	}

	@Override
	public void handleLoggingChange(LoggingInfo logging,
			List<String> propertyNames, List<Object> oldValues,
			List<Object> newValues) {
	
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Incoming event");
		}
	
		// skip incoming events if producer is not Enabled
		if (!producerEnabled) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("skipping incoming event: context is not initted");
			}
			return;
		}
	
		final JMSPublisher publisher = new JMSPublisher();
		try {
			// update the logging event with changes
			BeanUtils.smartUpdate(ModificationProxy.unwrap(logging), propertyNames, newValues);
	
			// propagate the event
			publisher.publish(jmsTemplate, properties, logging);
	
		} catch (Exception e) {
			if (LOGGER.isErrorEnabled()) {
				LOGGER.error(e.getLocalizedMessage(), e);
			}
		}
	}

	@Override
	public void handlePostGlobalChange(GeoServerInfo global) {
		// no op.s
	}

	@Override
	public void handlePostLoggingChange(LoggingInfo logging) {
		// send(xstream.toXML(logging), JMSConfigEventType.LOGGING_CHANGE);
	}

	@Override
	public void handlePostServiceChange(ServiceInfo service) {
		// send(xstream.toXML(service), JMSConfigEventType.POST_SERVICE_CHANGE);
	}

	@Override
	public void handleServiceChange(ServiceInfo service,
			List<String> propertyNames, List<Object> oldValues,
			List<Object> newValues) {
	
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Incoming event of type");
		}
	
		// skip incoming events if producer is not Enabled
		if (!producerEnabled) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("skipping incoming event: context is not initted");
			}
			return;
		}
	
		final JMSPublisher publisher = new JMSPublisher();
		try {
			// propagate the event
			publisher.publish(jmsTemplate, properties,
					new JMSServiceModifyEvent(
							ModificationProxy.unwrap(service), propertyNames,
							oldValues, newValues));
	
		} catch (Exception e) {
			if (LOGGER.isErrorEnabled()) {
				LOGGER.error(e.getLocalizedMessage(), e);
			}
		}
	}

	/**
	 * @return the properties
	 */
	public JMSProperties getProperties() {
		return properties;
	}

	/**
	 * @return the producerEnabled
	 */
	public boolean isProducerEnabled() {
		return producerEnabled;
	}

	public void onApplicationEvent(ApplicationEvent event) {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Incoming event of type "
					+ event.getClass().getSimpleName());
		}
	
		// event coming from the GeoServer application when the configuration
		// load process is complete
		if (event instanceof ContextLoadedEvent) {
			if (LOGGER.isWarnEnabled()) {
				LOGGER.warn("Activating JMS Catalog event publisher since the GeoServer context has been loaded. This GeoServer act as a master via the following class: "+this.getClass().getSimpleName());
				LOGGER.warn("Clustering Properties: "+this.properties.getProperties().toString());
			}
			setProducerEnabled(true);
	
		} else if (event instanceof ToggleEvent) {
	
			// enable/disable the producer
			final ToggleEvent tEv = (ToggleEvent) event;
			setProducerEnabled(tEv.toggleTo());
	
		} else {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Incoming application event of type "
						+ event.getClass().getSimpleName());
			}
		}
	}

	public void reloaded() {
	
		// skip incoming events until context is loaded
		if (!producerEnabled) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("skipping incoming event: context is not initted");
			}
			return;
		}
	
		// EAT EVENT
		// TODO check why reloaded here? check differences from CatalogListener
		// reloaded() method?
		// TODO disable and re-enable the producer!!!!!
		// this is potentially a problem since this listener should be the first
		// called by the GeoServer.
	
	}

	/**
	 * @param producerEnabled
	 *            enable or disable producer
	 * @note thread safe
	 */
	private void setProducerEnabled(final boolean producerEnabled) {
		if (producerEnabled) {
			// if produce is disable -> enable it
			if (!this.producerEnabled) {
				synchronized (this.producerEnabled) {
					if (!this.producerEnabled) {
						this.producerEnabled = true;
					}
				}
			}
		} else {
			// if produce is Enabled -> disable
			if (this.producerEnabled) {
				synchronized (this.producerEnabled) {
					if (this.producerEnabled) {
						this.producerEnabled = false;
					}
				}
			}
	
		}
	}

}
