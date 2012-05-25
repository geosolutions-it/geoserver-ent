/* Copyright (c) 2011 GeoSolutions - http://www.geo-solutions.it/.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package it.geosolutions.geoserver;

import it.geosolutions.imageio.plugins.turbojpeg.TurboJpegImageWriterSpi;

import java.awt.image.ColorModel;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.IIOImage;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.spi.ImageOutputStreamSpi;
import javax.imageio.spi.ImageWriterSpi;
import javax.imageio.stream.ImageOutputStream;

import org.geotools.image.ImageWorker;
import org.geotools.util.logging.Logging;
/**
 * Specific extension to use LibJPEG-Turbo to write JPEG Images.
 * 
 * @author Simone Giannecchini, GeoSolutions
 * @author Daniele Romagnoli, GeoSolutions
 *
 */
public class TurboImageWorker extends ImageWorker {

    static final String ERROR_LIB_MESSAGE = "The TurboJpeg native library hasn't been loaded: Skipping";

    static final String ERROR_FILE_MESSAGE = "The specified input file can't be read: Skipping";

    /**
     * The logger to use for this class.
     */
    private final static Logger LOGGER = Logging.getLogger(TurboImageWorker.class);

    private final static ImageWriterSpi SPI = new TurboJpegImageWriterSpi();

    public TurboImageWorker(RenderedImage image) {
        super(image);
    }

    /**
     * Writes outs the image contained into this {@link ImageWorker} as a JPEG
     * using the provided destination , compression and compression rate.
     * <p>
     * The destination object can be anything providing that we have an
     * {@link ImageOutputStreamSpi} that recognizes it.
     * 
     * @param destination
     *            where to write the internal {@link #image} as a JPEG.
     * @param compression
     *            algorithm.
     * @param compressionRate
     *            percentage of compression.
     * @param nativeAcc
     *            should we use native acceleration.
     * @return this {@link ImageWorker}.
     * @throws IOException
     *             In case an error occurs during the search for an
     *             {@link ImageOutputStream} or during the eoncding process.
     */
    public final void writeTurboJPEG(final Object destination, final String compression,
            final float compressionRate) throws IOException {
        // Reformatting this image for jpeg.
        if (LOGGER.isLoggable(Level.FINE))
            LOGGER.fine("Encoding input image to write out as JPEG.");

        // go to component color model if needed
        ColorModel cm = image.getColorModel();
        final boolean hasAlpha = cm.hasAlpha();
        forceComponentColorModel();
        cm = image.getColorModel();

        // rescale to 8 bit
        rescaleToBytes();
        cm = image.getColorModel();

        // remove transparent band
        final int numBands = image.getSampleModel().getNumBands();
        if (hasAlpha) {
            retainBands(numBands - 1);
        }

        ImageWriter writer = SPI.createWriterInstance();

        // Compression is available on both lib
        final ImageWriteParam iwp = writer.getDefaultWriteParam();

        iwp.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        iwp.setCompressionType(compression); // Lossy compression.
        iwp.setCompressionQuality(compressionRate); // We can control quality here.
        
        if (LOGGER.isLoggable(Level.FINE))
            LOGGER.fine("JPEG Encoding parameters:\n"+"compression:"+compression+"\n"+"compressionRate:"+compressionRate);
        
        try {
            writer.setOutput(destination);
            writer.write(null, new IIOImage(image, null, null), iwp);
            
        } catch (Exception e) {
            if(LOGGER.isLoggable(Level.SEVERE)){
                LOGGER.log(Level.SEVERE,e.getLocalizedMessage(),e);
            }
        } finally {
            try{
                writer.dispose();
            } catch (Exception e) {
                if(LOGGER.isLoggable(Level.FINE)){
                    LOGGER.log(Level.FINE,e.getLocalizedMessage(),e);
                }
            }
        }

        if (LOGGER.isLoggable(Level.FINE))
            LOGGER.fine("Writing out...");
        
    }

}
