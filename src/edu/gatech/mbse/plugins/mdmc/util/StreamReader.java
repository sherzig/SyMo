/**
 * 
 */
package edu.gatech.mbse.plugins.mdmc.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

/**
 * 
 * @author Sebastian
 *
 */
public class StreamReader extends Thread {
	
	private InputStream is;
	private StringWriter sw = new StringWriter();

	/**
	 * 
	 * @param is
	 */
	public StreamReader(InputStream is) {
		this.is = is;
	}

	/**
	 * 
	 */
	public void run() {
		try {
			int c;
			while ((c = is.read()) != -1)
				sw.write(c);
		} catch (IOException e) {
		}
	}

	/**
	 * 
	 * @return
	 */
	public String getResult() {
		return sw.toString();
	}
	
}