package snpsvm.app;

import java.util.HashMap;
import java.util.Map;

/**
 * Simple command line argument parser
 * @author brendan
 *
 */
public class ArgParser {
	
	Map<String, String> args;
	
	public ArgParser(String[] argv) {
		args = new HashMap<String, String>();
		
		int index = 0;
		while(index < argv.length) {
			String arg = argv[index];
			if (arg.startsWith("-")) {
				String val = "";
				if (index != (argv.length-1)) {
					if (! argv[index+1].startsWith("-")) {
						val = argv[index+1];
						index++;
					}
				}
				args.put(arg, val);

			}
			index++;
		}
	}

	/**
	 * True if an argument has been supplied that matches the given arg key
	 * @param arg
	 * @return
	 */
	public boolean hasOption(String arg) {
		return args.containsKey(arg);
	}
	
	/**
	 * Return a string-valued value associated with the given argument key
	 * @param key
	 * @return
	 */
	public String getStringArg(String key) {
		return args.get(key);
	}
	
	/**
	 * Attempt to parse a double from the value associated with the given argument key
	 * @param key
	 * @return
	 */
	public Double getDoubleArg(String key) {
		if (! hasOption(key)) {
			return null;
		}
		
		try {
			Double val = Double.parseDouble(args.get(key));
			return val;
		}
		catch (NumberFormatException nfe) {
			return null;
		}
	}
	
	/**
	 * Attempt to parse an integer from the value associated with the given argument key
	 * @param key
	 * @return
	 */
	public Integer getIntegerArg(String key) {
		if (! hasOption(key)) {
			return null;
		}
		
		try {
			Integer val = Integer.parseInt(args.get(key));
			return val;
		}
		catch (NumberFormatException nfe) {
			return null;
		}
	}
	
}
