package snpsvm.app;

import java.io.File;
import java.io.IOException;

import snpsvm.bamreading.IntervalList;

public abstract class AbstractModule implements Module {

	
	public String getRequiredStringArg(ArgParser args, String arg, String errorMessage) {
		if (args.hasOption(arg)) {
			return args.getStringArg(arg);
		}
		else {
			System.err.println(errorMessage);
			return null;
		}
	}
	
	/**
	 * Attempt to build a list of intervals from the -L argument. If the arg specifies a file, we assume it's
	 * a .BED file and try to read the intervals from it. If not, we treat it as a string and try to parse
	 * intervals of the form chrX:1-1000,chr5:1000-52134987 from it
	 * @param args
	 * @return An IntervalList containing the intervals described, or null if there are no specified intervals
	 */
	public IntervalList getIntervals(ArgParser args) {
		String intervalsStr = getOptionalStringArg(args, "-L");
		if (intervalsStr == null) {
			return null;
		}
		IntervalList intervals = new IntervalList();
		File testFile = new File(intervalsStr);
		if (testFile.exists()) {
			try {
				intervals.buildFromBEDFile(testFile);
			} catch (IOException e) {
				System.err.println("Error building interval list from BED file :  " + e.getMessage());
			}
		}
		else {
			try {
				intervals.buildFromString(intervalsStr);
			}
			catch (Exception ex) {
				System.err.println("Error parsing intervals from " + intervalsStr + " : " + ex.getMessage());
				return null;
			}
		}
		
		return intervals;
	}
	
	/**
	 * Return the given argument if it was given, otherwise return null and emit no message
	 * @param args
	 * @param arg
	 * @return
	 */
	public String getOptionalStringArg(ArgParser args, String arg) {
		if (args.hasOption(arg)) {
			return args.getStringArg(arg);
		}
		else {
			return null;
		}
	}
	
	/**
	 * Return true if the user has supplied the given arg
	 * @param args
	 * @param arg
	 * @return
	 */
	public boolean hasArg(ArgParser args, String arg) {
		if (args.hasOption(arg)) {
			return true;
		}
		else {
			return false;
		}
	}
	
}
