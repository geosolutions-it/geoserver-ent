package it.geosolutions.geoserver.map.turbojpeg;

import it.geosolutions.imageio.plugins.turbojpeg.TurboJpegUtilities;

import java.awt.image.RenderedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geoserver.wms.MapProducerCapabilities;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.map.JPEGMapResponse;
import org.geoserver.wms.map.RenderedImageMapResponse;
/**
 * 
 * @author Simone Giannecchini, GeoSolutions SAS
 *
 */
public class TurboJPEGMapResponse  extends RenderedImageMapResponse {

    /** Logger. */
    private final static Logger LOGGER = org.geotools.util.logging.Logging.getLogger(TurboJPEGMapResponse.class.toString());

    private static final boolean TURBO_JPEG_LIB_AVAILABLE = TurboJpegUtilities.isTurboJpegAvailable();
    
    private final static boolean DISABLE_TURBO= Boolean.getBoolean("disable.turbojpeg");
    
    /** 
     * Default capabilities for JPEG .
     * 
     * <p>
     * <ol>
     *         <li>tiled = supported</li>
     *         <li>multipleValues = unsupported</li>
     *         <li>paletteSupported = false</li>
     *         <li>transparency = false</li>
     * </ol>
     * 
     * <p>
     * We should soon support multipage tiff.
     */
    private static MapProducerCapabilities CAPABILITIES= new MapProducerCapabilities(true, false, false, false, null);

    /** the only MIME type this map producer supports */
    private static final String MIME_TYPE = "image/jpeg";

	private final JPEGMapResponse fallback;

    public TurboJPEGMapResponse(WMS wms) {
        super(MIME_TYPE, wms);
        fallback= new JPEGMapResponse(wms);
    }

    @Override
    public void formatImageOutputStream(RenderedImage image, OutputStream outStream, WMSMapContent mapContent) throws IOException {
    	
    	// FALLBACK
    	if(!TURBO_JPEG_LIB_AVAILABLE||DISABLE_TURBO){
    		if(LOGGER.isLoggable(Level.INFO)){
    			LOGGER.info("About to fallback on standard lib as libjpeg-turbi is not available");
    		}
    		fallback.formatImageOutputStream(image, outStream, mapContent);
    		return;
    	}
    	
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("About to write a JPEG image using libjpeg-turbo");
        }
        float quality = (100 - wms.getJpegCompression()) / 100.0f;
        final TurboJpegImageWorker iw = new TurboJpegImageWorker(image);
        iw.writeTurboJPEG(outStream, quality);

        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("Writing a JPEG done!!!");
        }
    }

    @Override
    public MapProducerCapabilities getCapabilities(String outputFormat) {
        return CAPABILITIES;
    }
}

