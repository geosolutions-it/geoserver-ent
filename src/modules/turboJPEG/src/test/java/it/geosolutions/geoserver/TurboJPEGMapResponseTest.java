/* Copyright (c) 2011 GeoSolutions - http://www.geo-solutions.it/.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package it.geosolutions.geoserver;

import junit.framework.Test;

import org.geoserver.test.OneTimeSetupTest;
import org.geoserver.wms.map.RenderedImageMapOutputFormat;
import org.geoserver.wms.map.RenderedImageMapOutputFormatTest;
import org.geoserver.wms.map.RenderedImageMapResponse;
/**
 * Specific {@link RenderedImageMapResponse} for JPEG using LibJPEGTurbo.
 * 
 * @author Simone Giannecchini, GeoSolutions SAS
 * @author Daniele Romagnoli, GeoSolutions SAS
 *
 */
public class TurboJPEGMapResponseTest extends RenderedImageMapOutputFormatTest {

    public static Test suite() {
        return new OneTimeSetupTest.OneTimeTestSetup(new TurboJPEGMapResponseTest());
    }

    protected RenderedImageMapOutputFormat getProducerInstance() {
        return new RenderedImageMapOutputFormat("image/jpeg", getWMS());
    }

}
