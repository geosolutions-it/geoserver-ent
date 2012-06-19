/* Copyright (c) 2012 GeoSolutions - http://www.geo-solutions.it/.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package it.geosolutions.geoserver.map.turbojpeg;

import org.geoserver.wms.wms_1_1_1.GetMapIntegrationTest;

import com.mockrunner.mock.web.MockHttpServletResponse;
/**
 * 
 * @author Simone Giannecchini, GeoSolutions SAS
 *
 */
public class TurboJPEGGetMapTest extends GetMapIntegrationTest {
    public void testImage() throws Exception {
        MockHttpServletResponse response = getAsServletResponse("wms?bbox=-130,24,-66,50" 
                + "&styles=&layers=sf:states&Format=image/jpeg" + "&request=GetMap"
                + "&width=550" + "&height=250" + "&srs=EPSG:4326");
        checkImage(response,"image/jpeg");
    }

}
