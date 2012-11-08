package org.geoserver.wps.gs;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.wps.ppio.FeatureAttribute;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.gce.arcgrid.ArcGridFormat;
import org.geotools.geometry.jts.GeometryBuilder;
import org.geotools.process.ProcessException;
import org.geotools.process.factory.DescribeParameter;
import org.geotools.process.factory.DescribeProcess;
import org.geotools.process.factory.DescribeResult;
import org.geotools.process.gs.GSProcess;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.ProgressListener;

import com.vividsolutions.jts.geom.Geometry;

@DescribeProcess(title = "helloWPS", description = "Hello WPS Sample")
public class IDASoundPropagationModelProcess implements GSProcess {

	protected static final Logger LOGGER = Logging.getLogger(IDASoundPropagationModelProcess.class);
	
	protected GeoServer geoServer;
	
	protected Catalog catalog;
	
	protected FilterFactory ff = CommonFactoryFinder.getFilterFactory(null);
	
	protected GeometryBuilder geomBuilder = new GeometryBuilder();

	public static final String DEFAULT_TYPE_NAME = "IDASoundPropModel";

	public IDASoundPropagationModelProcess(GeoServer geoServer) {
		this.geoServer = geoServer;
		this.catalog = geoServer.getCatalog();
	}
	
	@DescribeResult(name = "result", description = "List of attributes to be converted to a FeatureType")
	public SimpleFeatureCollection execute(
			@DescribeParameter(name = "octaveConfigFilePath", description = "Octave Config file full path") String octaveConfigFilePath,
			@DescribeParameter(name = "userId", description = "SPM attribute user_id") String userId,
			@DescribeParameter(name = "name", description = "SPM attribute name") String name,
			@DescribeParameter(name = "outputUrl", description = "SPM attribute output_url") URL outputUrl,
			@DescribeParameter(name = "runBegin", description = "SPM attribute run_begin") Date runBegin,
			@DescribeParameter(name = "runEnd", description = "SPM attribute run_end") Date runEnd,
			@DescribeParameter(name = "itemStatus", description = "SPM attribute item_status") String itemStatus,
			@DescribeParameter(name = "itemStatusMessage", description = "SPM attribute item_status_message") String itemStatusMessage,
			@DescribeParameter(name = "wsName", description = "SPM attribute workspace_name") String wsName,
			@DescribeParameter(name = "storeName", description = "SPM attribute store_name") String storeName,
			@DescribeParameter(name = "layerName", description = "SPM attribute layer_name") String layerName,
			@DescribeParameter(name = "securityLevel", description = "SPM attribute security_level") String securityLevel,
			@DescribeParameter(name = "srcPath", description = "SPM attribute src_path") String srcPath,
			@DescribeParameter(name = "season", description = "SPM attribute season") String season,
			@DescribeParameter(name = "srcFrequency", description = "SPM attribute src_frequency") Double srcFrequency,
			@DescribeParameter(name = "srcPressureLevel", description = "SPM attribute src_pressure_level") Double srcPressureLevel,
			@DescribeParameter(name = "footprint", description = "SPM attribute footprint") Geometry footprint,
			ProgressListener progressListener) throws ProcessException {

		// first off, decide what is the target store
		WorkspaceInfo ws;
		if (wsName != null) {
			ws = catalog.getWorkspaceByName(wsName);
			if (ws == null) {
				throw new ProcessException("Could not find workspace "
						+ wsName);
			}
		} else {
			ws = catalog.getDefaultWorkspace();
			if (ws == null) {
				throw new ProcessException(
						"The catalog is empty, could not find a default workspace");
			}
		}
		
		/**
		 * Convert the spread attributes into a FeatureType
		 */
		List<FeatureAttribute> attributes = new ArrayList<FeatureAttribute>();

		ToFeature toFeatureProcess = new ToFeature();

		CoordinateReferenceSystem crs;
		try {
			if (footprint.getSRID() <= 0) {
				crs = CRS.decode("EPSG:4326");
				footprint.setSRID(4326);
			} else {
				crs = CRS.decode("EPSG:" + footprint.getSRID());
			}
		} catch (NoSuchAuthorityCodeException e) {
			if (progressListener != null)
			{
				progressListener.exceptionOccurred(e);
			}
			throw new ProcessException("Could not DECODE the footprint CRS due to an Exception.", e);
		} catch (FactoryException e) {
			if (progressListener != null)
			{
				progressListener.exceptionOccurred(e);
			}
			throw new ProcessException("Could not DECODE the footprint CRS due to an Exception.", e);
		}

		UUID uuid = UUID.randomUUID();
		attributes.add(new FeatureAttribute("ftUUID", uuid.toString()));
		attributes.add(new FeatureAttribute("octaveConfigFilePath", octaveConfigFilePath));
		attributes.add(new FeatureAttribute("userId", userId));
		attributes.add(new FeatureAttribute("name", name));
		attributes.add(new FeatureAttribute("outputUrl", outputUrl));
		attributes.add(new FeatureAttribute("runBegin", runBegin));
		attributes.add(new FeatureAttribute("runEnd", runEnd));
		attributes.add(new FeatureAttribute("itemStatus", (itemStatus != null ? itemStatus : "CREATED")));
		attributes.add(new FeatureAttribute("itemStatusMessage", itemStatusMessage));
		attributes.add(new FeatureAttribute("wsName", wsName));
		attributes.add(new FeatureAttribute("storeName", storeName));
		attributes.add(new FeatureAttribute("layerName", layerName));
		attributes.add(new FeatureAttribute("securityLevel", securityLevel));
		attributes.add(new FeatureAttribute("srcPath", srcPath));
		attributes.add(new FeatureAttribute("season", season));
		attributes.add(new FeatureAttribute("srcFrequency", srcFrequency));
		attributes.add(new FeatureAttribute("srcPressureLevel", srcPressureLevel));

		SimpleFeatureCollection features = toFeatureProcess.execute(footprint, crs, DEFAULT_TYPE_NAME, attributes, null);
		
		if (progressListener != null)
		{
			progressListener.progress(20);
		}
		
		if (features == null || features.isEmpty())
		{
			throw new ProcessException("There was an error while converting attributes into FeatureType.");
		}

		/**
		 * LOG into the DB
		 */
        Filter filter = ff.equals(ff.property("ftUUID"), ff.literal(uuid.toString()));

        WFSLog wfsLogProcess = new WFSLog(geoServer);
         
        features = wfsLogProcess.execute(features, DEFAULT_TYPE_NAME, wsName, storeName, filter, null);
        
        if (features == null || features.isEmpty())
		{
    		if (progressListener != null)
    		{
    			progressListener.exceptionOccurred(new Exception("There was an error while logging FeatureType into the storage."));
    		}
			throw new ProcessException("There was an error while logging FeatureType into the storage.");
		}
        
		if (progressListener != null)
		{
			progressListener.progress(30);
		}

		/**
		 * RUN Octave SPM process
		 */
		GridCoverage2D coverage = null;
		File f = null;
		
		try
		{
			// in order to read a grid coverage we need to first store it on disk
			File root = new File(System.getProperty("java.io.tmpdir", "."));
			f = File.createTempFile("wps", "asc", root); // TODO: File back from OctaveProcess...
			FileOutputStream os = null;
			try {
				os = new FileOutputStream(f);
			} finally {
				IOUtils.closeQuietly(os);
			}

	        // and then we try to read it as a asc
	        coverage = new ArcGridFormat().getReader(f).read(null);
		} 
		catch (Exception e)
		{
			if (progressListener != null)
    		{
    			progressListener.exceptionOccurred(e);
    		}
			throw new ProcessException("There was an error while while processing Input parameters.", e);
		}

		final String spmRasterStoreName = FilenameUtils.getBaseName(f.getAbsolutePath());
		final String spmRasterLayerName = FilenameUtils.getBaseName(f.getAbsolutePath());
		final String styleName = null;

		/**
		 * Import output Octave ASC layer into GeoServer
		 */
		ImportProcess importProcess = new ImportProcess(catalog);
		String result = importProcess.execute(
				null,
				coverage,
				wsName,
				spmRasterStoreName,
				spmRasterLayerName,
				crs,
				null,
				styleName);
		
		if (result == null || !result.contains(spmRasterLayerName))
		{
    		if (progressListener != null)
    		{
    			progressListener.exceptionOccurred(new Exception("There was an error while importing ASC layer into GeoServer."));
    		}
			throw new ProcessException("There was an error while importing ASC layer into GeoServer.");
		}
        
		if (progressListener != null)
		{
			progressListener.progress(90);
		}
		
		/**
		 * Update Feature Attributes and LOG into the DB
		 */
        filter = ff.equals(ff.property("ftUUID"), ff.literal(uuid.toString()));

        SimpleFeature feature = SimpleFeatureBuilder.copy(features.subCollection(filter).toArray(new SimpleFeature[1])[0]);
        
        // build the feature
        feature.setAttribute("storeName", spmRasterStoreName);
        feature.setAttribute("layerName", spmRasterLayerName);
        feature.setAttribute("runEnd", new Date());
        
        wfsLogProcess = new WFSLog(geoServer);
        
        ListFeatureCollection output = new ListFeatureCollection(features.getSchema());
        output.add(feature);
		
		features = wfsLogProcess.execute(output, DEFAULT_TYPE_NAME, wsName, storeName, filter, null);
        
        if (features == null || features.isEmpty())
		{
    		if (progressListener != null)
    		{
    			progressListener.exceptionOccurred(new Exception("There was an error while logging FeatureType into the storage."));
    		}
			throw new ProcessException("There was an error while logging FeatureType into the storage.");
		}
        
		if (progressListener != null)
		{
			progressListener.progress(100);
			progressListener.complete();
		}

		
		return features;
	}

}