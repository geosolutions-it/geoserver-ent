/* Copyright (c) 2011 GeoSolutions - http://www.geo-solutions.it/.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package it.geosolutions.geoserver.map.turbojpeg;

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

    private final JPEGMapResponse fallback;

    private final static boolean DISABLE_TURBO = Boolean.getBoolean("disable.turbojpeg");

    /** Let us check if we have to fallback on the standard {@link JPEGMapResponse} in case
     * we do not have the right native libs for libjpeg turbo at hand.
     */
    private final static boolean TURBO_JPEG_LIB_AVAILABLE = TurboJpegUtilities
            .isTurboJpegAvailable();

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
    private final static MapProducerCapabilities CAPABILITIES = new MapProducerCapabilities(true,
            false, false, false, null);

    /** the only MIME type this map producer supports */
    private static final String MIME_TYPE = "image/jpeg";

    public TurboJPEGMapResponse(WMS wms) {
        super(MIME_TYPE, wms);
        fallback = new JPEGMapResponse(wms);
        if(!TURBO_JPEG_LIB_AVAILABLE){
            if(LOGGER.isLoggable(Level.WARNING)){
                LOGGER.warning("The turbo jpeg encoder is not available, check the native libs installation");
            }
        } else {
            if(LOGGER.isLoggable(Level.WARNING)){
                LOGGER.warning("The turbo jpeg encoder is available for usage");
            } 
        }        
        if(DISABLE_TURBO){
            if(LOGGER.isLoggable(Level.WARNING)){
                LOGGER.warning("The turbo jpeg encoder has been explicitly disabled");
            }
        }
    }

    @Override
    public MapProducerCapabilities getCapabilities(String outputFormat) {
        return CAPABILITIES;
    }

    @Override
    public void formatImageOutputStream(RenderedImage image, OutputStream outStream,
            WMSMapContext mapContext) throws ServiceException, IOException {

        // FALLBACK
        if (!TURBO_JPEG_LIB_AVAILABLE || DISABLE_TURBO) {
            if (LOGGER.isLoggable(Level.INFO)) {
                LOGGER.info("About to fallback on standard lib as libjpeg-turbi is not available");
            }
            fallback.formatImageOutputStream(image, outStream, mapContext);
            return;
        }

        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("About to write a JPEG image using libjpeg-turbo with compression set at "+wms.getJpegCompression());
        }
        float quality = (100 - wms.getJpegCompression()) / 100.0f;
        TurboJpegImageWorker iw=null;
        try{
            iw = new TurboJpegImageWorker(image);
            iw.writeTurboJPEG(outStream, quality);
        } finally{
            if(iw!=null){
                try{
                    iw.dispose();
                } catch (Exception e) {
                    if(LOGGER.isLoggable(Level.FINEST)){
                        LOGGER.log(Level.FINEST,e.getLocalizedMessage(),e);
                    }
                }
            }
        }

        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("Writing a JPEG using libjpeg-turbo done!!!");
        }
    }

}
