/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;

import org.geotools.process.ProcessException;
import org.geotools.process.factory.DescribeParameter;
import org.geotools.process.factory.DescribeProcess;
import org.geotools.process.factory.DescribeResult;
import org.geotools.process.gs.GSProcess;
import org.opengis.util.ProgressListener;

/**
 * 
 * 
 * @author Alessio Fabiani - GeoSolutions
 * 
 */
@DescribeProcess(title = "cmd line Process", description = "Execute a shell command through a Process Builder.")
public class CmdLine implements GSProcess {

	@DescribeResult(name = "result", description = "The process output response.")
	public String execute(
			@DescribeParameter(name = "execCommands", min = 1, description = "Shell command full path.") List<String> execCommands,
			@DescribeParameter(name = "execDir", min = 1, description = "Shell command execution folder.") File execDir,
			@DescribeParameter(name = "waitForResponse", description = "Whether or not should wait for output.") Boolean waitForResponse,
			ProgressListener progressListener)
	throws ProcessException {

		String response = "";

		ProcessBuilder pb = new ProcessBuilder(execCommands);
		pb.redirectErrorStream(true);
		pb.directory(execDir);

		//		Map<String, String> env = pb.environment();
		//		env.put("VAR1", "myValue");
		//		env.remove("OTHERVAR");
		//		env.put("VAR2", env.get("VAR1") + "suffix");
		
		try
		{
			Process shell = pb.start();
			
			if (waitForResponse)
			{
				// To capture output from the shell
				InputStream shellIn = shell.getInputStream();
				
				// Wait for the shell to finish and get the return code
				int shellExitStatus = shell.waitFor();
				
				response = convertStreamToStr(shellIn);
				
				shellIn.close();
			}
		}
		catch (IOException e)
		{
			throw new ProcessException("Error occured while executing Shell command. Error Description: " + e.getMessage(), e);
		}
		catch (InterruptedException e)
		{
			throw new ProcessException("Error occured while executing Shell command. Error Description: " + e.getMessage(), e);
		}
		
		return response;
	}

	/*
	* To convert the InputStream to String we use the Reader.read(char[]
	* buffer) method. We iterate until the Reader return -1 which means
	* there's no more data to read. We use the StringWriter class to
	* produce the string.
	*/
	public static String convertStreamToStr(InputStream is) throws IOException {
		if (is != null) {
			Writer writer = new StringWriter();

			char[] buffer = new char[1024];
			try {
				Reader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
				int n;
				while ((n = reader.read(buffer)) != -1) {
					writer.write(buffer, 0, n);
				}
			} finally {
				is.close();
			}
			return writer.toString();
		} else {
			return "";
		}
	}
}