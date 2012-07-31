/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.multidimensional.wms.map;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.DimensionPresentation;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.util.ReaderDimensionsAccessor;
import org.geoserver.ows.LocalLayer;
import org.geoserver.ows.LocalWorkspace;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.GetMapOutputFormat;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.MapLayerInfo;
import org.geoserver.wms.MapProducerCapabilities;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.map.OpenLayersMapOutputFormat;
import org.geoserver.wms.map.RawMap;
import org.geoserver.wms.map.RawMapResponse;
import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.Layer;
import org.geotools.map.WMSLayer;
import org.geotools.referencing.CRS;
import org.geotools.referencing.CRS.AxisOrder;
import org.geotools.temporal.object.DefaultPeriodDuration;
import org.geotools.util.logging.Logging;
import org.opengis.feature.type.FeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.ProjectedCRS;

import freemarker.ext.beans.BeansWrapper;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

/**
 * 
 * @see RawMapResponse
 */
public class CustomOpenLayersMapOutputFormat implements GetMapOutputFormat {
    /** A logger for this class. */
    private static final Logger LOGGER = Logging.getLogger(CustomOpenLayersMapOutputFormat.class);

    private static final String UTC_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    /**
     * The mime type for the response header
     */
    private static final String MIME_TYPE = "text/html";

    /**
     * The formats accepted in a GetMap request for this producer and stated in getcaps
     */
    private static final Set<String> OUTPUT_FORMATS = new HashSet<String>(Arrays.asList(
            "application/openlayers", "openlayers"));

    /**
     * Set of parameters that we can ignore, since they are not part of the OpenLayers WMS request
     */
    private static final Set<String> ignoredParameters;

    static {
        ignoredParameters = new HashSet<String>();
        ignoredParameters.add("REQUEST");
        ignoredParameters.add("TILED");
        ignoredParameters.add("BBOX");
        ignoredParameters.add("SERVICE");
        ignoredParameters.add("VERSION");
        ignoredParameters.add("FORMAT");
        ignoredParameters.add("WIDTH");
        ignoredParameters.add("HEIGHT");
        ignoredParameters.add("SRS");
    }

    /**
     * static freemaker configuration
     */
    private static Configuration cfgnd;
    private static Configuration cfg;
    

    static {
        cfgnd = new Configuration();
        cfgnd.setClassForTemplateLoading(CustomOpenLayersMapOutputFormat.class, "");
        BeansWrapper bw = new BeansWrapper();
        bw.setExposureLevel(BeansWrapper.EXPOSE_PROPERTIES_ONLY);
        cfgnd.setObjectWrapper(bw);
        
        cfg = new Configuration();
        cfg.setClassForTemplateLoading(OpenLayersMapOutputFormat.class, "");
        cfg.setObjectWrapper(bw);
    }

    /**
     * wms configuration
     */
    private WMS wms;

    public CustomOpenLayersMapOutputFormat(WMS wms) {
        this.wms = wms;
    }

    /**
     * @see org.geoserver.wms.GetMapOutputFormat#getOutputFormatNames()
     */
    public Set<String> getOutputFormatNames() {
        return OUTPUT_FORMATS;
    }

    /**
     * @see org.geoserver.wms.GetMapOutputFormat#getMimeType()
     */
    public String getMimeType() {
        return MIME_TYPE;
    }

    /**
     * @see org.geoserver.wms.GetMapOutputFormat#produceMap(org.geoserver.wms.WMSMapContext)
     */
    public RawMap produceMap(WMSMapContent mapContent) throws ServiceException, IOException {
        try {
            HashMap<String, Object> map = new HashMap<String, Object>();

            map.put("context", mapContent);
            map.put("pureCoverage", hasOnlyCoverages(mapContent));
            map.put("styles", styleNames(mapContent));
            map.put("request", mapContent.getRequest());
            map.put("yx", String.valueOf(isWms13FlippedCRS(mapContent.getRequest().getCrs())));
            map.put("maxResolution", new Double(getMaxResolution(mapContent.getRenderingArea())));

            String baseUrl = mapContent.getRequest().getBaseUrl();
            map.put("baseUrl", canonicUrl(baseUrl));

            // TODO: replace service path with call to buildURL since it does this
            // same dance
            String servicePath = "wms";
            if (LocalLayer.get() != null) {
                servicePath = LocalLayer.get().getName() + "/" + servicePath;
            }
            if (LocalWorkspace.get() != null) {
                servicePath = LocalWorkspace.get().getName() + "/" + servicePath;
            }
            map.put("servicePath", servicePath);

            map.put("parameters", getLayerParameter(mapContent.getRequest().getRawKvp()));
            map.put("units", getOLUnits(mapContent.getRequest()));

            if (mapContent.layers().size()== 1) {
                map.put("layerName", mapContent.layers().get(0).getTitle());
            } else {
                map.put("layerName", "Geoserver layers");
            }

            DimensionInfo timeInfo = getTimeInfo(mapContent);
            TreeSet<Date> times = getTimeList(mapContent, timeInfo);

            // create the template
            Template template;
            if (times == null || times.isEmpty()) {
                template = cfg.getTemplate("OpenLayersMapTemplate.ftl");
            } else {
                template = cfgnd.getTemplate("CustomOpenLayersMapTemplate.ftl");

                SimpleDateFormat df = new SimpleDateFormat(UTC_PATTERN);
                df.setTimeZone(TimeZone.getTimeZone("UTC"));
                String[] temporalList;
                if (timeInfo.getPresentation() == DimensionPresentation.LIST) {
                    temporalList = new String[times.size()];
                    Iterator<Date> it = times.iterator();
                    for (int i = 0; i < temporalList.length; i++) {
                        temporalList[i] = df.format(it.next());
                    }
                    map.put("timePresentationMode", "values");
                    map.put("timeIntervalStart", temporalList[0]);
                    map.put("timeIntervalEnd", temporalList[temporalList.length - 1]);
                    map.put("timeResolution", "");
                } else {
                    temporalList = new String[3];
                    temporalList[0] = df.format(times.first());
                    temporalList[1] = df.format(times.last());
                    final BigDecimal resolution = timeInfo.getResolution();
                    if (resolution == null) {
                        long durationInMilliSeconds = times.last().getTime()
                                - times.first().getTime();
                    }
                    temporalList[2] = new DefaultPeriodDuration(resolution.longValue()).toString();

                    map.put("timePresentationMode", "interval");
                    map.put("timeIntervalStart", temporalList[0]);
                    map.put("timeIntervalEnd", temporalList[1]);
                    map.put("timeResolution", temporalList[2]);
                }

                map.put("timeDimension", Arrays.asList(temporalList));
            }

            template.setOutputEncoding("UTF-8");
            ByteArrayOutputStream buff = new ByteArrayOutputStream();
            template.process(map, new OutputStreamWriter(buff, Charset.forName("UTF-8")));
            RawMap result = new RawMap(mapContent, buff, MIME_TYPE);
            return result;
        } catch (TemplateException e) {
            throw new ServiceException(e);
        }
    }

    /**
     * Guesses if the map context is made only of coverage layers by looking at the wrapping feature
     * type. Ugly, if you come up with better means of doing so, fix it.
     * 
     * @param mapContext
     * @return
     */
    private boolean hasOnlyCoverages(WMSMapContent mapContext) {
        for (Layer layer : mapContext.layers()) {
            FeatureType schema = layer.getFeatureSource().getSchema();
            boolean grid = schema.getName().getLocalPart().equals("GridCoverage")
                    && schema.getDescriptor("geom") != null && schema.getDescriptor("grid") != null
                    && !(layer instanceof WMSLayer);
            if (!grid)
                return false;
        }
        return true;
    }
    
    private boolean isWms13FlippedCRS(CoordinateReferenceSystem crs) {
        try {
            String code = "EPSG:" + CRS.lookupIdentifier(crs, false);
            code = WMS.toInternalSRS(code, WMS.version("1.3.0"));
            CoordinateReferenceSystem crs13 = CRS.decode(code);
            return CRS.getAxisOrder(crs13) == AxisOrder.NORTH_EAST;
        } catch(Exception e) {
            LOGGER.log(Level.WARNING, "Failed to determine CRS axis order, assuming is EN", e);
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private TreeSet<Date> getTimeList(WMSMapContent mapContext, DimensionInfo timeInfo)
            throws IOException {
        if (timeInfo == null) {
            return null;
        }

        Layer ml = mapContext.layers().get(0);
        LayerInfo layer = wms.getGeoServer().getCatalog().getLayerByName(ml.getTitle());

        // handle dimensions
        ResourceInfo resource = layer.getResource();
        if (resource instanceof FeatureTypeInfo) {
            return wms.getFeatureTypeTimes((FeatureTypeInfo) resource);
        } else if (resource instanceof CoverageInfo) {
            AbstractGridCoverage2DReader reader = (AbstractGridCoverage2DReader) ((CoverageInfo) resource)
                    .getGridCoverageReader(null, null);
            ReaderDimensionsAccessor accessor = new ReaderDimensionsAccessor(reader);
            return accessor.getTimeDomain();
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    private DimensionInfo getTimeInfo(WMSMapContent mapContext) throws IOException {
        if (mapContext.layers().size()!= 1 || mapContext.getRequest() == null) {
            return null;
        }

        Layer ml = mapContext.layers().get(0);
        LayerInfo layer = wms.getGeoServer().getCatalog().getLayerByName(ml.getTitle());

        if (layer == null) {
            return null;
        }

        // handle dimensions
        ResourceInfo resource = layer.getResource();
        if (resource != null && resource.getMetadata().get(ResourceInfo.TIME) != null) {
            DimensionInfo timeInfo = resource.getMetadata().get(ResourceInfo.TIME,
                    DimensionInfo.class);
            if (timeInfo.isEnabled()) {
                return timeInfo;
            }
        }

        return null;
    }

    private List<String> styleNames(WMSMapContent mapContext) {
        if (mapContext.layers().size()!= 1 || mapContext.getRequest() == null)
            return Collections.emptyList();

        MapLayerInfo info = mapContext.getRequest().getLayers().get(0);
        return info.getOtherStyleNames();
    }

    /**
     * OL does support only a limited number of unit types, we have to try and return one of those,
     * otherwise the scale won't be shown. From the OL guide: possible values are "degrees" (or
     * "dd"), "m", "ft", "km", "mi", "inches".
     * 
     * @param request
     * @return
     */
    private String getOLUnits(GetMapRequest request) {
        CoordinateReferenceSystem crs = request.getCrs();
        // first rough approximation, meters for projected CRS, degrees for the
        // others
        String result = crs instanceof ProjectedCRS ? "m" : "degrees";
        try {
            String unit = crs.getCoordinateSystem().getAxis(0).getUnit().toString();
            // use the unicode escape sequence for the degree sign so its not
            // screwed up by different local encodings
            final String degreeSign = "\u00B0";
            if (degreeSign.equals(unit) || "degrees".equals(unit) || "dd".equals(unit))
                result = "degrees";
            else if ("m".equals(unit) || "meters".equals(unit))
                result = "m";
            else if ("km".equals(unit) || "kilometers".equals(unit))
                result = "mi";
            else if ("in".equals(unit) || "inches".equals(unit))
                result = "inches";
            else if ("ft".equals(unit) || "feets".equals(unit))
                result = "ft";
            else if ("mi".equals(unit) || "miles".equals(unit))
                result = "mi";
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error trying to determine unit of measure", e);
        }
        return result;
    }

    /**
     * Returns a list of maps with the name and value of each parameter that we have to forward to
     * OpenLayers. Forwarded parameters are all the provided ones, besides a short set contained in
     * {@link #ignoredParameters}.
     * 
     * 
     * 
     * @param rawKvp
     * @return
     */
    private List<Map<String, String>> getLayerParameter(Map<String, String> rawKvp) {
        List<Map<String, String>> result = new ArrayList<Map<String, String>>(rawKvp.size());

        for (Map.Entry<String, String> en : rawKvp.entrySet()) {
            String paramName = en.getKey();

            if (ignoredParameters.contains(paramName.toUpperCase())) {
                continue;
            }

            // this won't work for multi-valued parameters, but we have none so
            // far (they are common just in HTML forms...)
            Map<String, String> map = new HashMap<String, String>();
            map.put("name", paramName);
            map.put("value", en.getValue());
            result.add(map);
        }

        return result;
    }

    /**
     * Makes sure the url does not end with "/", otherwise we would have URL lik
     * "http://localhost:8080/geoserver//wms?LAYERS=..." and Jetty 6.1 won't digest them...
     * 
     * @param baseUrl
     * @return
     */
    private String canonicUrl(String baseUrl) {
        if (baseUrl.endsWith("/")) {
            return baseUrl.substring(0, baseUrl.length() - 1);
        } else {
            return baseUrl;
        }
    }

    private double getMaxResolution(ReferencedEnvelope areaOfInterest) {
        double w = areaOfInterest.getWidth();
        double h = areaOfInterest.getHeight();

        return ((w > h) ? w : h) / 256;
    }

    public MapProducerCapabilities getCapabilities(String format) {
        return null;
    }

}
