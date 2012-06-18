/*
 *    ImageI/O-Ext - OpenSource Java Image translation Library
 *    http://www.geo-solutions.it/
 *    http://java.net/projects/imageio-ext/
 *    (C) 2007 - 2011, GeoSolutions
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    either version 3 of the License, or (at your option) any later version.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package it.geosolutions.geoserver.map.turbojpeg;

import it.geosolutions.geoserver.map.turbojpeg.TurboJpegImageWorker;
import it.geosolutions.imageio.plugins.turbojpeg.TurboJpegImageWriterSpi;
import it.geosolutions.imageio.plugins.turbojpeg.TurboJpegUtilities;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

import junit.framework.Assert;

import org.geotools.image.ImageWorker;
import org.geotools.test.TestData;
import org.junit.Before;
import org.junit.Test;

/**
 * 
 * @author Simone Giannecchini, GeoSolutions SAS
 *
 */
public class TurboImageWorkerTest extends Assert {

    static final String ERROR_LIB_MESSAGE = "The TurboJpeg native library hasn't been loaded: Skipping test";

    static boolean SKIP_TESTS = false;
    
    static final Logger LOGGER = Logger.getLogger(TurboImageWorkerTest.class.toString());

    static final TurboJpegImageWriterSpi turboSPI = new TurboJpegImageWriterSpi();

    @Before
    public void setup() {
        SKIP_TESTS = !TurboJpegUtilities.isTurboJpegAvailable();
    }

    @Test
    public void errors() throws IOException{
    	if (SKIP_TESTS){
    	    LOGGER.warning(ERROR_LIB_MESSAGE);
    	    return;
    	}
        
    	//test-data
        final File input = TestData.file(this, "testmergb.png");
        assertTrue("Unable to find test data", input.exists() && input.isFile() && input.canRead());

       

        // create output file
        final File output = TestData.temp(this, "output.jpeg");
        try{
        	new TurboJpegImageWorker(ImageIO.read(input)).writeTurboJPEG(new FileOutputStream(output), 1.5f);
        	assertFalse("We should not be allowed to specify compression ratios > 1",true);
        } catch (Exception e) {
			// TODO: handle exception
		}
        
        try{
        	new TurboJpegImageWorker(ImageIO.read(input)).writeTurboJPEG(new FileOutputStream(output), -.5f);
        	assertFalse("We should not be allowed to specify compression ratios > 1",true);
        } catch (Exception e) {
			// TODO: handle exception
		}
        
        

    }
    
    @Test
    public void writer() throws IOException{
    	if (SKIP_TESTS){
    	    LOGGER.warning(ERROR_LIB_MESSAGE);
    	    return;
    	}
        
    	//test-data
        final File input = TestData.file(this, "testmergb.png");
        assertTrue("Unable to find test data", input.exists() && input.isFile() && input.canRead());

       

        // create output file
        final File output = TestData.temp(this, "output.jpeg");
        new TurboJpegImageWorker(ImageIO.read(input)).writeTurboJPEG(new FileOutputStream(output), .5f);
        assertTrue("Unable to create output file", output.exists() && output.isFile());
        
        new ImageWorker(output).getBufferedImage().flush();

    }
}
