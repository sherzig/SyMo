package edu.gatech.mbse.plugins.mdmc.util;

public class WindowsRegistry {

	/**
	 * 
	 * @param key path in the registry
	 * @param value registry key
	 * @return registry value or null if not found
	 */
	public static final String readRegistry(String key, String value) {
		try {
			// Run reg query, then read output with StreamReader
			Process process = Runtime.getRuntime().exec("reg query " + "\"" + key + "\" /v " + value);

			StreamReader reader = new StreamReader(process.getInputStream());
			reader.start();
			process.waitFor();
			reader.join();
			String output = reader.getResult();
			
			if(output.length() < 5)
				return null;
			
			// Output has the following format:
			// \n<Version information>\n\n<key>\t<registry type>\t<value>
			if (!output.contains("\t")) {
				return (output.substring(output.lastIndexOf("  ") + 1)).replaceAll("\\r\\n|\\r|\\n", "").trim();
			}

			// Parse out the value
			String[] parsed = output.split("\t");
			return parsed[parsed.length - 1];
		}
		catch (Exception e) {
			return null;
		}
	}

}