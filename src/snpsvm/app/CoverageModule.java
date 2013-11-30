package snpsvm.app;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import javax.swing.Timer;

import snpsvm.app.AbstractModule.MissingArgumentException;
import snpsvm.bamreading.BAMWindowStore;
import snpsvm.bamreading.CallingOptions;
import snpsvm.bamreading.HasBaseProgress;
import snpsvm.bamreading.coverage.CoverageCaller;
import snpsvm.bamreading.coverage.IntervalCoverage;
import snpsvm.bamreading.intervalProcessing.IntervalList;
import snpsvm.bamreading.snpCalling.IntervalSNPCaller;
import snpsvm.bamreading.variant.Variant;

public class CoverageModule extends AbstractModule {

	@Override
	public void emitUsage() {
		System.out.println("Coverage module: Computes coverage statistics over a list of intervals");
		System.out.println(" coverage -B <input.bam> -L <intervals.bed>");
	}

	@Override
	public boolean matchesModuleName(String name) {
		return name.equalsIgnoreCase("coverage");
	}

	@Override
	public void performOperation(String name, ArgParser args) {
		
		String inputBAMPath = null;
		File inputBAM = null;
		try {
			inputBAMPath = getRequiredStringArg(args, "-B", "Missing required argument for input BAM file, use -B");
		} catch (MissingArgumentException e1) {
			System.err.println(e1.getMessage());
			return;
		}
		
		inputBAM = new File(inputBAMPath);
		if (! inputBAM.exists()) {
			System.err.println("Can't find input bam file at path : " + inputBAMPath);
			System.exit(1);
		}
		
		
		IntervalList intervals = getIntervals(args);
		
		int threads= CommandLineApp.configModule.getThreadCount();
		//Initialize BAMWindow store
		BAMWindowStore bamWindows = new BAMWindowStore(inputBAM, threads);
		boolean emitProgress = true;
		
		Timer progressTimer = null;

		final long intervalExtent = intervals.getExtent();
		
		List<IntervalCoverage> allIntervals; 

		CallingOptions ops = new CallingOptions();
		ThreadPoolExecutor threadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(threads);

		final CoverageCaller caller = new CoverageCaller(threadPool, ops, bamWindows);

		//Submit multiple jobs to thread pool, returns immediately
		caller.submitAll(intervals);

		if (emitProgress) {
			System.out.println("Examining " + intervals.getExtent() + " bases with " + threads + " threads in " + caller.getCallerCount() + " chunks");

			progressTimer = new javax.swing.Timer(100, new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent arg0) {
					emitProgressString(caller, intervalExtent);
				}
			});
			progressTimer.setDelay(419);
			progressTimer.start();
		}

		//Blocks until all regions are called
		allIntervals = caller.getResult();

		
		//Emit one more progress message
		if (emitProgress) {
			emitProgressString(caller, intervalExtent);
		}
		
		
		if (progressTimer != null)
			progressTimer.stop();
		
		Collections.sort(allIntervals);
		

		
		System.out.flush();
		System.out.println();
		writeResults(allIntervals, System.out);
		
		//Write the variants to a file
		//PrintStream writer = new PrintStream(new FileOutputStream(destination));
		
	}

	protected void writeResults(List<IntervalCoverage> results, PrintStream out) {
		DecimalFormat formatter = new DecimalFormat("#0.00");
		for (IntervalCoverage cov : results) {
			out.println(cov.contig + " : " + cov.interval + "\t:\t" + formatter.format((double)cov.coverageSum / (double)cov.basesActuallyExamined));
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
	
	private Long startTime = null;
	private int prevLength = 0;
	private int charIndex = 0;
	private static final char[] markers = {'|', '/', '-', '\\', '|', '/', '-', '\\'};
}
