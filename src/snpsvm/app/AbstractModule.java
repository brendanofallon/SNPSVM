package snpsvm.app;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;

import snpsvm.bamreading.HasBaseProgress;
import snpsvm.bamreading.intervalProcessing.IntervalList;
import snpsvm.counters.CounterSource;

public abstract class AbstractModule implements Module {

	protected DecimalFormat smallFormatter = new DecimalFormat("#0.0000");
	protected DecimalFormat formatter = new DecimalFormat("#0.00");
	private Long startTime = null;
	private int prevLength = 0;
	private int charIndex = 0;
	private static final char[] markers = {'|', '/', '-', '\\', '|', '/', '-', '\\'};
	
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
	
	protected void emitProgressString(HasBaseProgress caller, long intervalExtent) {
		double basesCalled = 1.0 * caller.getBasesCalled();
		double frac = basesCalled / intervalExtent;
		if (startTime == null) {
			startTime = System.currentTimeMillis();
			System.out.println("   Elapsed       Bases      Bases / sec   % Complete     mem");
		}
		long elapsedTimeMS = System.currentTimeMillis() - startTime;
		double elapsedSecs = elapsedTimeMS / 1000.0;
		double basesPerSec = basesCalled / (double)elapsedSecs;
		DecimalFormat formatter = new DecimalFormat("#0.00");
		DecimalFormat intFormatter = new DecimalFormat("0");
		for(int i=0; i<prevLength; i++) {
			System.out.print('\b');
		}
		char cm = markers[charIndex % markers.length];
                long usedBytes = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
                
                long usedMB = usedBytes / (1024*1024);
                double usedGB = usedMB / 1024.00;
                String memStr = usedMB + "MB";
                if (usedMB > 1000)
                    memStr = formatter.format(usedGB) + "GB";
                
		String msg = cm + "  " + toUserTime(elapsedSecs) + " " + padTo("" + intFormatter.format(basesCalled), 12) + "  " + padTo("" + formatter.format(basesPerSec), 12) + "  " + padTo(formatter.format(100.0*frac), 8) + "% " + padTo(memStr, 12);
		System.out.print(msg);
		prevLength = msg.length();
		charIndex++;
	}

	protected String toUserTime(double secs) {
		int minutes = (int)Math.floor(secs / 60.0);
		int hours = (int)Math.floor(minutes / 60.0);
		secs = secs % 60;
		DecimalFormat formatter = new DecimalFormat("#0.00");
		if (hours < 1) {
			if (secs < 10)
				return minutes + ":0" + formatter.format(secs);
			else
				return minutes + ":" + formatter.format(secs);
			
		}
		else {
			if (secs < 10)
				return hours + ":" + minutes + ":0" + formatter.format(secs);
			else 
				return hours + ":" + minutes + ":" + formatter.format(secs);
		}
		
	}
	
	private static String padTo(String str, int len) {
		while(str.length() < len) {
			str = " " + str;
		}
		return str;
	}
}
