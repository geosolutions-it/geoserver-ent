/* Copyright (c) 2011 GeoSolutions - http://www.geo-solutions.it/.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package it.geosolutions.geoserver.map.turbojpeg;

import it.geosolutions.imageio.plugins.turbojpeg.TurboJpegImageWriteParam;
import it.geosolutions.imageio.plugins.turbojpeg.TurboJpegImageWriter;
import it.geosolutions.imageio.plugins.turbojpeg.TurboJpegImageWriterSpi;
import it.geosolutions.imageio.plugins.turbojpeg.TurboJpegUtilities;
import it.geosolutions.imageio.utilities.ImageOutputStreamAdapter2;

import java.awt.image.ColorModel;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.IIOImage;
import javax.imageio.ImageWriteParam;
import javax.imageio.spi.ImageOutputStreamSpi;
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
class TurboJpegImageWorker extends ImageWorker {

    static final String ERROR_LIB_MESSAGE = "The TurboJpeg native library hasn't been loaded: Skipping";

    static final String ERROR_FILE_MESSAGE = "The specified input file can't be read: Skipping";

    /**
     * The logger to use for this class.
     */
    private final static Logger LOGGER = Logging.getLogger(TurboJpegImageWorker.class);

    private final static TurboJpegImageWriterSpi TURBO_JPEG_SPI = new TurboJpegImageWriterSpi();

    public TurboJpegImageWorker(RenderedImage image) {
        super(image);
    }
    
    /**
     * Writes outs the image contained into this {@link ImageWorker} as a JPEG using the provided destination , compression and compression rate.
     * <p>
     * The destination object can be anything providing that we have an {@link ImageOutputStreamSpi} that recognizes it.
     * 
     * @param destination where to write the internal {@link #image} as a JPEG.
     * @param compressionRate percentage of compression.
     * @return this {@link ImageWorker}.
     * @throws IOException In case an error occurs during the search for an {@link ImageOutputStream} or during the eoncding process.
     */
    public final void writeTurboJPEG(final OutputStream destination, final float compressionRate)
            throws IOException {

        if (!TurboJpegUtilities.isTurboJpegAvailable()) {
            throw new IllegalStateException(ERROR_LIB_MESSAGE);
        }

        // Reformatting this image for jpeg.
        if (LOGGER.isLoggable(Level.FINE))
            LOGGER.fine("Encoding input image to write out as JPEG using .");

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

        // Getting a writer.
        if (LOGGER.isLoggable(Level.FINE))
            LOGGER.fine("Creating a TURBO JPEG writer and configuring it.");

        final TurboJpegImageWriter writer = (TurboJpegImageWriter) TURBO_JPEG_SPI
                .createWriterInstance();
        // Compression is available on both lib
        TurboJpegImageWriteParam iwp = (TurboJpegImageWriteParam) writer.getDefaultWriteParam();
        final ImageOutputStreamAdapter2 outStream = new ImageOutputStreamAdapter2(destination);
        iwp.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        iwp.setCompressionType("JPEG");
        iwp.setCompressionQuality(compressionRate); // We can control quality here.

        if (LOGGER.isLoggable(Level.FINE))
            LOGGER.fine("Writing image out...");

        try {
            writer.setOutput(outStream);
            writer.write(null, new IIOImage(image, null, null), iwp);
        } finally {
            try {
                writer.dispose();
            } catch (Throwable e) {
                if (LOGGER.isLoggable(Level.FINE))
                    LOGGER.log(Level.FINE, e.getLocalizedMessage(), e);
            }
            try {
                outStream.close();
            } catch (Throwable e) {
                if (LOGGER.isLoggable(Level.FINE))
                    LOGGER.log(Level.FINE, e.getLocalizedMessage(), e);
            }
        }
    }

}
