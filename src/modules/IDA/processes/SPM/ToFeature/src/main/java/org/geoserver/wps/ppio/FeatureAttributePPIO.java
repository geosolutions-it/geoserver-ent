/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.ppio;

import java.io.InputStream;

import com.thoughtworks.xstream.XStream;
 
 
/**
 * 
 * 
 * @author Alessio Fabiani - GeoSolutions
 * 
 */
public class FeatureAttributePPIO extends XStreamPPIO {
 
    protected FeatureAttributePPIO() {
        super(FeatureAttribute.class);
    }

	@Override
	public FeatureAttributePPIO decode(InputStream input) throws Exception {
		// prepare xml encoding
        XStream xstream = buildXStream();

		// write out FeatureAttributePPIO
        return (FeatureAttributePPIO) xstream.fromXML(input);
	}
}