package snpsvm.app;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Interface for configuration / properties, users use this module to add / remove / list properties
 * @author brendanofallon
 *
 */
public class ConfigModule implements Module {

	public static final String configFilePath = System.getProperty("user.home") + "/.snpsvm.config";
	
	private Map<String, String> properties = null;
	
	@Override
	public boolean matchesModuleName(String name) {
		return name.equals("config");
	}

	@Override
	public void performOperation(String name, ArgParser args) {
		//We support only three args, add, remove, and list
		loadProperties();
		
		
		if (args.hasOption("-add")) {
			String prop = args.getStringArg("-add");
			if (! prop.contains("=")) {
				System.err.println("Please supply a property in the form key=value");
			}
			else {
				String[] toks = prop.split("=");
				properties.put(toks[0].trim(), toks[1].trim());
				System.err.println("Added property " + toks[0] + " = " + toks[1]);
			}
		}
		
		if (args.hasOption("-remove")) {
			String prop = args.getStringArg("-remove");
			if (properties.containsKey(prop)) {
				properties.remove(prop);
				System.err.println("Removed property " + prop);
			}
			else {
				System.err.println("Property " + prop + " not found");
			}
		}
		
		if (args.hasOption("-list")) {
			System.out.println("Properties contains " + properties.size() + " pairs");
			for(String key : properties.keySet()) {
				System.out.println(key + " = " + properties.get(key));
			}
		}
		
		
		writeProperties();
	}
	
	/**
	 * Read properties from file into map
	 */
	private void loadProperties() {
		properties = new HashMap<String, String>();
		
		try {
			BufferedReader reader = new BufferedReader(new FileReader(configFilePath));
			String line = reader.readLine();
				
			while(line != null) {
				if (line.length()>0 && (! line.startsWith("#"))) {
					String[] toks=  line.split("=");
					if (toks.length != 2) {
						System.err.println("Warning: Could not read config property: " + line);
					}
					else {
						properties.put(toks[0], toks[1]);
					}
				}
				line = reader.readLine();
			}
			reader.close();
		} catch (FileNotFoundException e) {
			
		} catch (IOException e) {
			System.err.println("Error : Could not read config file " + configFilePath);
		}
		
	}

	
	private void writeProperties() {
		if (properties == null) {
			return;
		}
		
		try {
			File configFile = new File(configFilePath); 
			if (! configFile.exists()) {
				System.err.println("No configuration file found, creating new one at : " + configFile.getAbsolutePath());
				configFile.createNewFile();
			}
			
			BufferedWriter writer = new BufferedWriter(new FileWriter(configFilePath));
			for(String key : properties.keySet()) {
				writer.write(key + "=" + properties.get(key) + "\n");
			}
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
}
