/*
 *  GeoServer-Manager - Simple Manager Library for GeoServer
 *  
 *  Copyright (C) 2007,2011 GeoSolutions S.A.S.
 *  http://www.geo-solutions.it
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package it.geosolutions.geoserver.jms.test.rest;

import it.geosolutions.geoserver.rest.decoder.RESTCoverageStore;
import it.geosolutions.geoserver.rest.encoder.GSLayerEncoder;
import it.geosolutions.geoserver.rest.encoder.GSResourceEncoder.ProjectionPolicy;
import it.geosolutions.geoserver.rest.encoder.coverage.GSImageMosaicEncoder;
import it.geosolutions.geoserver.rest.encoder.metadata.GSDimensionInfoEncoder;
import it.geosolutions.geoserver.rest.encoder.metadata.GSDimensionInfoEncoder.Presentation;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.springframework.core.io.ClassPathResource;

/**
 * Testcase for publishing layers on geoserver.
 * We need a running GeoServer to properly run the tests. 
 * If such geoserver instance cannot be contacted, tests will be skipped.
 *
 * @author etj
 */
public class GeoserverRESTPublisherTest extends GeoserverRESTTest {

    private final static Logger LOGGER = Logger.getLogger(GeoserverRESTPublisherTest.class);

    public GeoserverRESTPublisherTest(String testName) {
        super(testName);
    }
    
    public void testWorkspaces() {
        if (!enabled()) return;
        deleteAll();

        assertEquals(0, masterReader.getWorkspaces().size());

        assertTrue(publisher.createWorkspace("WS1"));
        assertTrue(publisher.createWorkspace("WS2"));
        assertEquals(2, masterReader.getWorkspaces().size());

        try {
			Thread.sleep(TIMEOUT);
		} catch (InterruptedException e) {
			fail(e.getLocalizedMessage());
		}
        for (int i=0; i< nSlaves; i++){
        	assertEquals(2, reader[i].getWorkspaces().size());
        }
    }
    
    final static String styleName = "restteststyle";
    
    public void testExternalGeotiff() throws FileNotFoundException, IOException {
        if (!enabled()) return;
        deleteAll();

        assertEquals(0, masterReader.getStyles().size());

        
        File sldFile = new ClassPathResource("testdata/restteststyle.sld").getFile();

        // insert style
        assertTrue(publisher.publishStyle(sldFile));
        assertTrue(masterReader.existsStyle(styleName));
        
        try {
			Thread.sleep(TIMEOUT);
		} catch (InterruptedException e) {
			fail(e.getLocalizedMessage());
		}
        for (int i=0; i< nSlaves; i++){
        	assertTrue(reader[i].existsStyle(styleName));
        }
        
        String storeName = "testRESTStoreGeotiff";
        String layerName = "resttestdem";

        if (masterReader.getWorkspaces().isEmpty())
        	assertTrue(publisher.createWorkspace(DEFAULT_WS));

        File geotiff = new File("/media/share/testdata/resttestdem.tif");
        
        // known state?
//        assertFalse("Cleanup failed", existsLayer(layerName));

        // test insert
        RESTCoverageStore pc = publisher.publishExternalGeoTIFF(DEFAULT_WS, storeName, geotiff, "EPSG:4326", styleName);
        assertNotNull("publish() failed", pc);
        LOGGER.info(pc);
        
        assertTrue(existsLayer(layerName));
        
        RESTCoverageStore reloadedCS = masterReader.getCoverageStore(DEFAULT_WS, storeName);
        assertNotNull(reloadedCS);
        try {
			Thread.sleep(TIMEOUT);
		} catch (InterruptedException e) {
			fail(e.getLocalizedMessage());
		}
        for (int i=0; i< nSlaves; i++){
        	RESTCoverageStore slaveCS =reader[i].getCoverageStore(DEFAULT_WS, storeName);
        	assertNotNull("Unable to get coverageStore for reader n.:"+i,slaveCS);
//        	assertTrue(reloadedCS.equals(slaveCS));
        }

    }
    
    public void testCreateDeleteImageMosaicDatastore() {
        if (!enabled()) {
            return;
        }
//        deleteAll();
        
        final String coverageStoreName = "resttestImageMosaic";

        final GSImageMosaicEncoder coverageEncoder = new GSImageMosaicEncoder();
        
        coverageEncoder.setAllowMultithreading(true);
        coverageEncoder.setBackgroundValues("");
        coverageEncoder.setFilter("");
        coverageEncoder.setInputTransparentColor("");
        coverageEncoder.setLatLonBoundingBox(-180, -90, 180, 90, "EPSG:4326");
        coverageEncoder.setMaxAllowedTiles(6000);
        coverageEncoder.setNativeBoundingBox(-180, -90, 180, 90, "EPSG:4326");
        coverageEncoder.setOutputTransparentColor("");
        coverageEncoder.setProjectionPolicy(ProjectionPolicy.REPROJECT_TO_DECLARED);
        coverageEncoder.setSRS("EPSG:4326");
        coverageEncoder.setSUGGESTED_TILE_SIZE("256,256");
        coverageEncoder.setUSE_JAI_IMAGEREAD(true);
        // activate time
//        final GSDimensionInfoEncoder time=new GSDimensionInfoEncoder(true);
//        time.setPresentation(Presentation.LIST);
//        // set time metadata
//        coverageEncoder.setMetadata("time", time);
//        // not active elevation
//        coverageEncoder.setMetadata("elevation", new GSDimensionInfoEncoder());
        
        if (masterReader.getWorkspaces().isEmpty())
        	assertTrue(publisher.createWorkspace(DEFAULT_WS));
        
        LOGGER.info(coverageEncoder.toString());
        
//        final String styleName = "testRasterStyle3";
//        File sldFile;
//		try {
//			sldFile = new ClassPathResource("testdata/raster.sld").getFile();
//	        // insert style
//	        assertTrue(publisher.publishStyle(sldFile,styleName));
//		} catch (IOException e1) {
//			assertFalse(e1.getLocalizedMessage(),Boolean.FALSE);
//			e1.printStackTrace();
//		}
        
        GSLayerEncoder layerEncoder=new GSLayerEncoder();
        
        layerEncoder.setDefaultStyle(styleName);
        LOGGER.info(layerEncoder.toString());
        // creation test
        RESTCoverageStore coverageStore =null;
        try {
        	final File mosaicFile = new File("/media/share/time_geotiff/");
        	
        	if (!publisher.createExternalMosaic(DEFAULT_WS,coverageStoreName,mosaicFile,coverageEncoder,layerEncoder)){
        		fail();
        	}
    		coverageStore = masterReader.getCoverageStore(DEFAULT_WS,coverageStoreName);
    		
    		if (coverageStore==null){
                LOGGER.error("*** coveragestore " + coverageStoreName + " has not been created.");
                fail("*** coveragestore " + coverageStoreName + " has not been created.");
    		}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
			fail(e.getLocalizedMessage());
		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getLocalizedMessage());
		}
		
        // removing recursively coveragestore
        boolean removed = publisher.removeCoverageStore(coverageStore.getWorkspaceName(), coverageStore.getName(), true);
        if( ! removed ){
            LOGGER.error("*** CoverageStore " + coverageStoreName + " has not been removed.");
            fail("*** CoverageStore " + coverageStoreName + " has not been removed.");
        }
        
    } 
    

    
    

//    protected void cleanupTestStyle(final String styleName) {
//        // dry run delete to work in a known state
//        if (masterReader.existsStyle(styleName)) {
//            LOGGER.info("Clearing stale test style " + styleName);
//            boolean ok = publisher.removeStyle(styleName);
//            if (!ok) {
//                fail("Could not unpublish style " + styleName);
//            }
//        }
//        assertFalse("Cleanup failed", masterReader.existsStyle(styleName));
//    }

//    /**
//     * @deprecated unsupported - should be external
//     * @throws FileNotFoundException
//     * @throws IOException
//     */
//    public void testPublishDeleteShapeZip() throws FileNotFoundException, IOException {
//        if (!enabled()) {
//            return;
//        }
//        deleteAllWorkspaces();
//        assertTrue(publisher.createWorkspace(DEFAULT_WS));
//
//        String storeName = "resttestshp";
//        String layerName = "cities";
//
//        File zipFile = new ClassPathResource("testdata/resttestshp.zip").getFile();
//
//        // known state?
//        cleanupTestFT(layerName, DEFAULT_WS, storeName);
//
//        // test insert
//        boolean published = publisher.publishShp(DEFAULT_WS, storeName, layerName, zipFile);
//        assertTrue("publish() failed", published);
//        assertTrue(existsLayer(layerName));
//
//        RESTLayer layer = masterReader.getLayer(layerName);
//
//        LOGGER.info("Layer style is " + layer.getDefaultStyle());
//
//        //test delete
//        boolean ok = publisher.unpublishFeatureType(DEFAULT_WS, storeName, layerName);
//        assertTrue("Unpublish() failed", ok);
//        assertFalse(existsLayer(layerName));
//
//        // remove also datastore
//        boolean dsRemoved = publisher.removeDatastore(DEFAULT_WS, storeName);
//        assertTrue("removeDatastore() failed", dsRemoved);
//
//    }
    

//	public void testDeleteUnexistingFT() throws FileNotFoundException, IOException {
//		String wsName = "this_ws_does_not_exist";
//		String storeName = "this_store_does_not_exist";
//		String layerName = "this_layer_does_not_exist";
//
//		boolean ok = publisher.unpublishFT(wsName, storeName, layerName);
//		assertFalse("unpublished not existing layer", ok);
//	}
    
    private boolean existsLayer(String layername) {
        try {
			Thread.sleep(TIMEOUT);
		} catch (InterruptedException e) {
			fail(e.getLocalizedMessage());
		}
		boolean test=masterReader.getLayer(layername) != null;
		if (!test){
			LOGGER.info("Layer is not present on the master");
			return test;
		}
        for (int i=0; i< nSlaves; i++){
        	test=test&&(reader[i].getLayer(layername)!= null);
        	if (!test){
    			LOGGER.info("Layer is not present on slave n: "+i+" toString: "+reader[i].toString());
    		}
        }
        return test;
    }
}
