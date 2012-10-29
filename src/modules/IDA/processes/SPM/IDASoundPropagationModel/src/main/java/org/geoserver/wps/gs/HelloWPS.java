package org.geoserver.wps.gs;

import org.geotools.process.factory.DescribeParameter;
import org.geotools.process.factory.DescribeProcess;
import org.geotools.process.factory.DescribeResult;
import org.geotools.process.gs.GSProcess;

@DescribeProcess(title="helloWPS", description="Hello WPS Sample")
public class HelloWPS implements GSProcess {

   @DescribeResult(name="result", description="output result")
   public String execute(
		   @DescribeParameter(name="name", description="name to return") String name) 
   {
     return "Hello, " + name;
   }

}