package snpsvm.app;

import java.io.File;
import java.io.IOException;

import snpsvm.bamreading.intervalProcessing.IntervalList;
import snpsvm.counters.CounterSource;

public abstract class AbstractModule implements Module {

	/**
	 * Return a string associated with the given arg key (i.e. return "str" from -x str) or throw
	 * an error if -x was not given
	 * @param args
	 * @param arg
	 * @param errorMessage
	 * @return
	 * @throws MissingArgumentException
	 */
	public String getRequiredStringArg(ArgParser args, String arg, String errorMessage) throws MissingArgumentException {
		if (args.hasOption(arg)) {
			return args.getStringArg(arg);
		}
		else {
			throw new MissingArgumentException(errorMessage);
		}
	}
	
	/**
	 * Mostly a debugging method to allow command-line access to which features (counters) to include / exclude
	 * @param args
	 */
	protected void processExcludedIntervals(ArgParser args) {
		if (args.hasOption("-x")) {
			String excludes = args.getStringArg("-x");
			String[] toks = excludes.split(",");
			for(String tok : toks) {
				int col = Integer.parseInt(tok);
				System.out.println("Excluding column #" + col);
				CounterSource.excludeCounter(col);
			}
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
	 * Return the given argument if it was given, otherwise return null and emit no message
	 * @param args
	 * @param arg
	 * @return
	 */
	public Double getOptionalDoubleArg(ArgParser args, String arg) {
		if (args.hasOption(arg)) {
			return args.getDoubleArg(arg);
		}
		else {
			return null;
		}
	}
	
	/**
	 * Return the given argument if it was given, otherwise return null and emit no message
	 * @param args
	 * @param arg
	 * @return
	 */
	public Integer getOptionalIntegerArg(ArgParser args, String arg) {
		if (args.hasOption(arg)) {
			return args.getIntegerArg(arg);
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
	
	/**
	 * These are thrown when the user has not specified a required argument
	 * @author brendanofallon
	 *
	 */
	class MissingArgumentException extends Exception {
		
		public MissingArgumentException(String message) {
			super(message);
		}
	}
}
