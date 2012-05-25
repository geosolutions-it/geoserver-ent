/* Copyright (c) 2011 GeoSolutions - http://www.geo-solutions.it/.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package it.geosolutions.geoserver;

import it.geosolutions.imageio.plugins.turbojpeg.TurboJpegUtilities;

import java.awt.image.RenderedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geoserver.platform.ServiceException;
import org.geoserver.wms.MapProducerCapabilities;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSMapContext;
import org.geoserver.wms.map.JPEGMapResponse;
import org.geoserver.wms.map.RenderedImageMapResponse;
/**
 * Specific {@link RenderedImageMapResponse} for JPEG using LibJPEGTurbo.
 * 
 * @author Simone Giannecchini, GeoSolutions SAS
 * @author Daniele Romagnoli, GeoSolutions SAS
 *
 */
public class TurboJPEGMapResponse extends RenderedImageMapResponse {

    /** Logger. */
    private final static Logger LOGGER = org.geotools.util.logging.Logging
            .getLogger(TurboJPEGMapResponse.class);

    private JPEGMapResponse delegated = null;
    
    private final static boolean NATIVELIBS_AVAILABLE=TurboJpegUtilities.isTurboJpegAvailable();

    /**
     * Default capabilities for JPEG .
     * 
     * <p>
     * <ol>
     * <li>tiled = supported</li>
     * <li>multipleValues = unsupported</li>
     * <li>paletteSupported = false</li>
     * <li>transparency = false</li>
     * </ol>
     * 
     * <p>
     * We should soon support multipage tiff.
     */
    private static MapProducerCapabilities CAPABILITIES = new MapProducerCapabilities(true, false,
            false, false, null);

    /** the only MIME type this map producer supports */
    private static final String MIME_TYPE = "image/jpeg";

    public TurboJPEGMapResponse(WMS wms) {
        super(MIME_TYPE, wms);
        delegated = new JPEGMapResponse(wms);
    }

    @Override
    public MapProducerCapabilities getCapabilities(String outputFormat) {
        return CAPABILITIES;
    }

    @Override
    public void formatImageOutputStream(RenderedImage image, OutputStream outStream,
            WMSMapContext mapContext) throws ServiceException, IOException {
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("About to write a JPEG image.");
        }
        if (NATIVELIBS_AVAILABLE) {
            float quality = (100 - wms.getJpegCompression()) / 100.0f;
            TurboImageWorker iw = new TurboImageWorker(image);
            iw.writeTurboJPEG(outStream, "JPEG", quality);
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine("Writing a JPEG done!!!");
            }
        } else {
            if (delegated != null) 
                delegated.formatImageOutputStream(image, outStream, mapContext);
        }

        
    }

}
