/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.wps.gs.IDARasterAlgebraProcess;
import org.geoserver.wps.gs.IDASoundPropagationModelProcess;
import org.geotools.data.DataUtilities;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;

public class IDADeleteTransactionListener implements TransactionListener {

	protected static final Logger LOGGER = Logging.getLogger(IDADeleteTransactionListener.class);

	protected GeoServer geoServer;

	protected Catalog catalog;

	List events = new ArrayList();
	List features = new ArrayList();

	public IDADeleteTransactionListener(GeoServer geoServer) {
		this.geoServer = geoServer;
		this.catalog = geoServer.getCatalog();
	}

	public void clear() {
		events.clear();
		features.clear();
	}

	public void dataStoreChange(TransactionEvent event) throws WFSException {
		events.add(event);
		String typeName = event.getAffectedFeatures().getSchema().getTypeName();
		// check the correct event type and the correct FeatureType Name
		if ((typeName.contains(IDARasterAlgebraProcess.DEFAULT_TYPE_NAME) || typeName.contains(IDASoundPropagationModelProcess.DEFAULT_TYPE_NAME)) && TransactionEventType.PRE_DELETE == event.getType()) {
			features.addAll(DataUtilities.list(event.getAffectedFeatures()));
			for (Object ft : features) {
				if (ft instanceof SimpleFeature) {
					WorkspaceInfo ws = null;
					DataStoreInfo storeInfo = null;
					try {
						// retrieve workspace and store as inserted in the feature attributes
						SimpleFeature next = (SimpleFeature) ft;
						String wsName = (String) next.getAttribute("wsName");
						String storeName = (String) next.getAttribute("storeName");
						String layerName = (String) next.getAttribute("layerName");
						if (wsName != null && storeName != null
								&& wsName.length() > 0
								&& storeName.length() > 0) {
							// being sure the workspace exists in the catalog
							ws = catalog.getWorkspaceByName(wsName);
							if (ws == null) {
								LOGGER.severe("Could not retrive WorkSpace "
										+ wsName + " into the Catalog");
							}

							if (ws != null) {
								// being sure the store exists in the catalog
								storeInfo = catalog.getDataStoreByName(wsName,
										storeName);
								
								// drill down into layers (into resources since we cannot scan layers)
								int layersInStore = 0;
						        List<ResourceInfo> resources = catalog.getResourcesByStore(storeInfo, ResourceInfo.class);
						        for (ResourceInfo ri : resources) {
						            List<LayerInfo> layers = catalog.getLayers(ri);
						            if (!layers.isEmpty()){ 
						                for (LayerInfo li : layers) {
						                	// counting the store layers, if 0 we can remove the whole store too ...
						                	layersInStore++;
						                	// we need to check the layer name start, since for the coverages a timestamp is attached to the name
						                	if (layerName != null && li.getName().startsWith(layerName))
						                	{
						                		catalog.remove(li);
						                		layersInStore--;
						                	}
						                }
						            }
						        }
						        
						        // the store does not contain layers anymore, lets remove it from the catalog then
						        if (layersInStore == 0)
						        	catalog.remove(storeInfo);
								
								if (storeInfo == null) {
									LOGGER.severe("Could not retrive DataStore "
											+ storeName + " into the Catalog");
								}
							}
						}

						String srcPath = (String) next.getAttribute("srcPath");
						if (ws != null && storeInfo != null && srcPath != null && srcPath.length() > 0) {
							File file = new File(srcPath);

							// try to delete the file several times since it may be locked by catalog for some reason
							if (file.exists() && file.isFile() && file.canWrite()) {
								final int retries = 10;
								int tryDelete = 0;
								for (; tryDelete < 5; tryDelete++)
								{
									if (file.delete())
									{
										break;
									}
									else
									{
										// wait 5 seconds and try again...
										Thread.sleep(5000);
									}
								}
								
								if (tryDelete > retries)
								{
									LOGGER.severe("Could not delete file "+srcPath+" from the FileSystem");
								}
							}
						}
					} catch (Exception e) {
						LOGGER.severe("Exception occurred during Deletion: "+e.getLocalizedMessage());
						throw new WFSException(e);
					} finally {
					}
				}
			}
		}
	}
}