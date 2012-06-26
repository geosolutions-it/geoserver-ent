/*
 *  GeoBatch - Open Source geospatial batch processing system
 *  http://geobatch.geo-solutions.it/
 *  Copyright (C) 2007-2012 GeoSolutions S.A.S.
 *  http://www.geo-solutions.it
 *
 *  GPLv3 + Classpath exception
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package it.geosolutions.activemq;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.management.JMX;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

/**
 * JobScheduler job implementation to call GeoBatch Action via JMX
 * 
 * @author Carlo Cancellieri - carlo.cancellieri@geo-solutions.it
 */
public class JMXClientUtils<T> {

	// JMX bean
	public final static String SERVICE_JMXBEAN_KEY = "ServiceBean";
	public final static String GB_JMXBEAN_NAME = "AsyncStarter";

	// JMX
	public final static String SERVICE_URL_KEY = "service_jmx_url";
	public final static String SERVICE_URL = "localhost";
	public final static String SERVICE_PORT_KEY = "service_jmx_port";
	public final static String SERVICE_PORT = "1099";


	/**
	 * initialize environment using the incoming properties file (or defaults).
	 * The properties file used to configure connection parameters may contains
	 * at least: <br>
	 * <ul>
	 * <li><b>gb_jmx_url=localhost</b> - remote JMX server url</li>
	 * <li><b>gb_jmx_port=1099</b> - remote JMX server port</li>
	 * <li><b>JMXActionManager=JMXActionManager</b> - bean name which implements
	 * ActionManager interface</li>
	 * </ul>
	 * NOTE: above keywords are reserved keys<br>
	 * 
	 * @param configFilePath
	 *            the String representing the absolute file path of the
	 *            jmx.properties or null (in this case defaults values are used)
	 * @return a map containing the initialized environment
	 */
	public static Map<String, String> loadEnv(final String configFilePath) {
		// load from file
		final Properties props = new Properties();
		// if no external file configuration is found using defaults
		if (configFilePath != null) {
			FileInputStream fis = null;
			try {
				fis = new FileInputStream(new File(configFilePath));
				props.load(fis);

			} catch (IOException e) {

				// if (LOGGER.isEnabledFor(Level.ERROR)){
				// LOGGER.error("Unable to run without a property file, check the path: "+path);
				// }

			} finally {
				if (fis != null) {
					try {
						fis.close();
					} catch (Exception e) {
					}
				}
			}
		}

		// return env
		final Map<String, String> env = new HashMap<String, String>();

		// init defaults
		// url
		if (!props.containsKey(SERVICE_URL_KEY))
			env.put(SERVICE_URL_KEY, SERVICE_URL);
		else
			env.put(SERVICE_URL_KEY, props.getProperty(SERVICE_URL_KEY));

		// port
		if (!props.containsKey(SERVICE_PORT_KEY))
			env.put(SERVICE_PORT_KEY, SERVICE_PORT);
		else
			env.put(SERVICE_PORT_KEY, props.getProperty(SERVICE_PORT_KEY));

		if (!props.containsKey(SERVICE_JMXBEAN_KEY))
			env.put(SERVICE_JMXBEAN_KEY, GB_JMXBEAN_NAME);
		else
			env.put(SERVICE_JMXBEAN_KEY, props.getProperty(SERVICE_JMXBEAN_KEY));

		// form properties env to map env
		final Set keySet = props.keySet();
		final Iterator it = keySet.iterator();
		while (it.hasNext()) {
			String key = (String) it.next();
			env.put(key, props.getProperty(key));
		}
		return env;
	}

	/**
	 * 
	 * @param env
	 * @return JMXConnector representing the newly-made connection. Each
	 *         successful call to this method produces a different object
	 * @throws IllegalArgumentException
	 *             if the passed env is null, or it doesn't contains valid
	 *             values for the {@link #SERVICE_PORT_KEY} of {@link #SERVICE_URL_KEY}
	 * @throws IOException
	 *             if the connector client or the connection cannot be made
	 *             because of a communication problem.
	 */
	public static JMXConnector getConnector(Map<String, String> env)
			throws IllegalArgumentException, IOException {
		if (env == null)
			throw new IllegalArgumentException(
					"Unable to connect with a null environment, please");

		final String gbUrl = env.get(SERVICE_URL_KEY);
		if (gbUrl == null)
			throw new IllegalArgumentException(
					"Unable to connect with a null port, please set a port value for the key: "
							+ SERVICE_URL_KEY);

		final String gbPort = env.get(SERVICE_PORT_KEY);
		if (gbPort == null)
			throw new IllegalArgumentException(
					"Unable to connect with a null port, please set a url value for the key: "
							+ SERVICE_PORT_KEY);

		StringBuilder urlString = new StringBuilder(
				"service:jmx:rmi:///jndi/rmi://");

		final JMXServiceURL url = new JMXServiceURL(urlString.append(gbUrl)
				.append(":").append(gbPort).append("/jmxrmi").toString());

		return JMXConnectorFactory.connect(url, env);
	}

	/**
	 * Using the initialized environment (see {@link #loadEnv(String)}) to
	 * create a dedicated proxy of the desired MBean (instead of going directly
	 * through the MBean server connection).
	 * 
	 * @param environment
	 *            see {@link #loadEnv(String)}
	 * @return the ActionManager proxy instance.
	 * @throws IOException
	 *             if a valid MBeanServerConnection cannot be created, for
	 *             instance because the connection to the remote MBean server
	 *             has not yet been established (with the connect method), or it
	 *             has been closed, or it has broken.
	 * @throws MalformedObjectNameException
	 *             The string passed as a parameter does not have the right
	 *             format.
	 * @throws NullPointerException
	 *             if the name of the bean is not found into the environment
	 *             (see {@link #SERVICE_JMXBEAN_KEY})
	 */
	public static AsyncStarter getProxy(Map<String, ?> environment,
			JMXConnector jmxc) throws IOException,
			MalformedObjectNameException, NullPointerException {

		if (jmxc == null || environment == null) {
			throw new IllegalArgumentException("Some arguments are null");
		}

		// get bean name from the environment
		final ObjectName mbeanName = new ObjectName("bean:name="
				+ environment.get(SERVICE_JMXBEAN_KEY));

		// create the proxy
		final AsyncStarter mbeanProxy = JMX.newMBeanProxy(
				jmxc.getMBeanServerConnection(), mbeanName,
				AsyncStarter.class, true);

		return mbeanProxy;
	}

}
