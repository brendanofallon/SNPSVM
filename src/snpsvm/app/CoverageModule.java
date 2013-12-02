package snpsvm.app;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import javax.swing.Timer;

import snpsvm.bamreading.BAMWindowStore;
import snpsvm.bamreading.CallingOptions;
import snpsvm.bamreading.coverage.CoverageCaller;
import snpsvm.bamreading.coverage.IntervalCoverage;
import snpsvm.bamreading.intervalProcessing.IntervalList;

public class CoverageModule extends AbstractModule {
	
	@Override
	public void emitUsage() {
		System.out.println("Coverage module: Computes coverage statistics over a list of intervals");
		System.out.println(" coverage -B <input.bam> -L <intervals.bed>");
		System.out.println("Optional arguments:");
		System.out.println(" -C 0,15,74,100 Coverage thresholds to report");
		System.out.println(" -quiet [false] do not emit progress to std. out");
		System.out.println(" -noSummary [false] do not write final coverage summary");
		System.out.println(" -noIntervals [false] do not write per interval summary");
		System.out.println(" -z [false] emit depths as z-scores (subtract mean, divide by standard dev)");
	}

	@Override
	public boolean matchesModuleName(String name) {
		return name.equalsIgnoreCase("coverage");
	}

	@Override
	public void performOperation(String name, ArgParser args) {
		
		boolean emitProgress = true;
		boolean emitIntervalResults = true;
		boolean emitSummary = true;
		boolean emitZ = false;
		
		String inputBAMPath = null;
		File inputBAM = null;
		try {
			inputBAMPath = getRequiredStringArg(args, "-B", "Missing required argument for input BAM file, use -B");
			emitProgress = ! args.hasOption("-quiet");
			emitIntervalResults = ! args.hasOption("-noIntervals");
			emitSummary = ! args.hasOption("-noSummary");
			emitZ = args.hasOption("-z");
			String cutoffsStr = getOptionalStringArg(args, "-C");
			if (cutoffsStr != null) {
				String[] toks  = cutoffsStr.split(",");
				List<Integer> cuts = new ArrayList<Integer>();
				for(int i=0; i<toks.length; i++) {
					cuts.add( Integer.parseInt(toks[i].trim()));
				}
				
				IntervalCoverage.setCutoffs( cuts.toArray(new Integer[]{}));
			}
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
		
		
		Timer progressTimer = null;

		final long intervalExtent = intervals.getExtent();
		
		List<IntervalCoverage> allIntervals; 

		CallingOptions ops = new CallingOptions();
		ThreadPoolExecutor threadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(threads);

		final CoverageCaller caller = new CoverageCaller(threadPool, ops, bamWindows);

		//Submit multiple jobs to thread pool, returns immediately
		caller.submitAll(intervals);

		//Start timer / progress emitter
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
		
		//results header:
		PrintStream out = System.out;
		
		if (emitIntervalResults || emitSummary) {
			out.print("\n Interval \t\t\tMean");
			for(int i=0; i<IntervalCoverage.getCutoffCount(); i++) {
				out.print("\t" + IntervalCoverage.getCutoffs()[i]);
			}
			out.println();
		}
		
		if (emitZ) {
			//First compute mean coverage
			IntervalCoverage overall = new IntervalCoverage();
			overall.coverageAboveCutoff = new int[IntervalCoverage.getCutoffCount()];
			for (IntervalCoverage cov : allIntervals) {
				overall.basesActuallyExamined += cov.basesActuallyExamined;
				overall.coverageSum += cov.coverageSum;
				for(int i=0; i<overall.coverageCutoffs.length; i++) {
					overall.coverageAboveCutoff[i] += cov.coverageAboveCutoff[i];
				}
			}
			
			double meanCov = (double)overall.coverageSum / (double)overall.basesActuallyExamined;
			
			//do it again to compute stdev
			double sumSquares = 0;
			for (IntervalCoverage cov : allIntervals) {
				double intervalMean = (double)cov.coverageSum / (double)cov.basesActuallyExamined;
				sumSquares += (meanCov - intervalMean)*(meanCov-intervalMean);
			}
			double stdev = Math.sqrt(sumSquares / meanCov);
			
			//Now write depths transformed into z-scores
			for (IntervalCoverage cov : allIntervals) {
				out.print(cov.contig + " : " + cov.interval + "\t:\t" + smallFormatter.format( ((double)cov.coverageSum / (double)cov.basesActuallyExamined-meanCov)/stdev));
				out.println();
			}	
		}
		
		
		if (emitIntervalResults && (! emitZ)) {
			//Write results for each interval
			writeResults(allIntervals, out);
		}

		if (emitSummary) {
			//Compute overall result across all intervals
			IntervalCoverage overall = new IntervalCoverage();
			overall.coverageAboveCutoff = new int[IntervalCoverage.getCutoffCount()];
			for (IntervalCoverage cov : allIntervals) {
				overall.basesActuallyExamined += cov.basesActuallyExamined;
				overall.coverageSum += cov.coverageSum;
				for(int i=0; i<overall.coverageCutoffs.length; i++) {
					overall.coverageAboveCutoff[i] += cov.coverageAboveCutoff[i];
				}
			}

			out.print("All intervals\t " + "\t:\t" + formatter.format((double)overall.coverageSum / (double)overall.basesActuallyExamined));
			for(int i=0; i<overall.coverageCutoffs.length; i++) {
				out.print("\t" + formatter.format((double)overall.coverageAboveCutoff[i] / (double)overall.basesActuallyExamined));
			}
			out.println();
		}
		
	}

	protected void writeResults(List<IntervalCoverage> results, PrintStream out) {		
		for (IntervalCoverage cov : results) {
			out.print(cov.contig + " : " + cov.interval + "\t:\t" + formatter.format((double)cov.coverageSum / (double)cov.basesActuallyExamined));
			for(int i=0; i<cov.coverageCutoffs.length; i++) {
				out.print("\t" + formatter.format((double)cov.coverageAboveCutoff[i] / (double)cov.basesActuallyExamined));
			}
			out.println();
		}	
	}
	

}
