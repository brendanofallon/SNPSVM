package snpsvm.app;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import javax.swing.Timer;

import snpsvm.bamreading.BAMWindowStore;
import snpsvm.bamreading.FastaReader;
import snpsvm.bamreading.HasBaseProgress;
import snpsvm.bamreading.IntervalList;
import snpsvm.bamreading.IntervalList.Interval;
import snpsvm.bamreading.SplitSNPAndCall;
import snpsvm.bamreading.Variant;
import snpsvm.counters.BinomProbComputer;
import snpsvm.counters.ColumnComputer;
import snpsvm.counters.DepthComputer;
import snpsvm.counters.DinucRepeatCounter;
import snpsvm.counters.DistroProbComputer;
import snpsvm.counters.HomopolymerRunCounter;
import snpsvm.counters.MQComputer;
import snpsvm.counters.MeanQualityComputer;
import snpsvm.counters.MismatchComputer;
import snpsvm.counters.NearbyQualComputer;
import snpsvm.counters.NucDiversityCounter;
import snpsvm.counters.PosDevComputer;
import snpsvm.counters.QualSumComputer;
import snpsvm.counters.ReadPosCounter;
import snpsvm.counters.StrandBiasComputer;

public class Predictor extends AbstractModule {

	List<ColumnComputer> counters;
	boolean emitProgress = true;
	
	public Predictor() {
		counters = new ArrayList<ColumnComputer>();
		
		counters.add( new DepthComputer());
		counters.add( new BinomProbComputer());
		counters.add( new QualSumComputer());
		counters.add( new MeanQualityComputer());
		counters.add( new PosDevComputer());
		counters.add( new MQComputer());
		counters.add( new DistroProbComputer());
		counters.add( new NearbyQualComputer());
		counters.add( new StrandBiasComputer());
		counters.add( new MismatchComputer());
		counters.add( new ReadPosCounter());
		counters.add( new HomopolymerRunCounter());
		counters.add( new DinucRepeatCounter());
		counters.add( new NucDiversityCounter());
//		counters.add( new ContextComputer());
	}
	
	@Override
	public boolean matchesModuleName(String name) {
		return name.equalsIgnoreCase("predict") || name.equals("emit");
	}
	
	public void emitColumnNames() {
		int index = 1;
		for(ColumnComputer counter : counters) {
			for(int i=0; i<counter.getColumnCount(); i++) {
				System.out.println(index + "\t" + counter.getColumnDesc(i));
				index++;
			}
		}
	}

	@Override
	public void performOperation(String name, ArgParser args) {
		if (name.equals("emit")) {
			emitColumnNames();
			return;
		}
		
		
		String referencePath = getRequiredStringArg(args, "-R", "Missing required argument for reference file, use -R");
		String inputBAMPath = getRequiredStringArg(args, "-B", "Missing required argument for input BAM file, use -B");
		String modelPath = getRequiredStringArg(args, "-M", "Missing required argument for model file, use -M");
		String vcfPath = getRequiredStringArg(args, "-V", "Missing required argument for destination vcf file, use -V");
		boolean writeData = ! args.hasOption("-X");
		IntervalList intervals = getIntervals(args);
		
		if (!writeData) {
			System.err.println("Skipping reading of BAM file... re-calling variants from existing output");
		}
		
		File inputBAM = new File(inputBAMPath);
		File reference = new File(referencePath);
		File model = new File(modelPath);
		File vcf = new File(vcfPath);
		
		try {
			callSNPs(inputBAM, reference, model, vcf, intervals, counters);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Create a new interval list with no intervals extending beyond the range given
	 * in the refernece file
	 * @param reference
	 * @param intervals
	 * @return
	 * @throws IOException 
	 */
	protected static IntervalList validateIntervals(File reference, IntervalList intervals) throws IOException {
		FastaReader refReader = new FastaReader(reference);
		IntervalList newIntervals = new IntervalList();
		for(String contig : intervals.getContigs()) {
			Integer maxLength = refReader.getContigSizes().get(contig);
			if (maxLength == null) {
				throw new IllegalArgumentException("Could not find contig " + contig + " in reference");
			}
			for(Interval interval : intervals.getIntervalsInContig(contig)) {
				int newPos = Math.min(maxLength, interval.getLastPos());
				newIntervals.addInterval(contig, interval.getFirstPos(), newPos);
			}
		}
		
		
		return newIntervals;
	}
	
	public void callSNPs(File inputBAM, 
			File ref,
			File model,
			File destination,
			IntervalList intervals,
			List<ColumnComputer> counters) throws IOException {
		
		intervals = validateIntervals(ref, intervals);
		
		int threads= CommandLineApp.configModule.getThreadCount();
		//Initialize BAMWindow store
		BAMWindowStore bamWindows = new BAMWindowStore(inputBAM, threads);
		
		//Somehow logically divide work into rational number of workers
		Timer progressTimer = null;
		final int intervalExtent = intervals.getExtent();
		
		List<Variant> allVars; 
		if (threads > 1) {
			ThreadPoolExecutor threadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(threads);

			final SplitSNPAndCall caller = new SplitSNPAndCall(ref, bamWindows, model, counters, threadPool);
			
			
			//Submit multiple jobs to thread pool, returns immediately
			caller.submitAll(intervals);
			
			
			if (emitProgress) {
				System.out.println("Calling SNPs over " + intervals.getExtent() + " bases with " + threads + " threads in " + caller.getCallerCount() + " chunks");
				
				progressTimer = new javax.swing.Timer(100, new ActionListener() {

					@Override
					public void actionPerformed(ActionEvent arg0) {
						emitProgressString(caller, intervalExtent);
					}
				});
				progressTimer.setDelay(500);
				progressTimer.start();
			}

			//Blocks until all variants are called
			allVars = caller.getAllVariants();
			
			//Emit one more progress message
			if (emitProgress) {
				emitProgressString(caller, intervalExtent);
			}
		}
		else {
			final SNPCaller caller = new SNPCaller(ref, model, intervals, counters, bamWindows);
			
			if (emitProgress) {
				System.out.println("Calling SNPs over " + intervals.getExtent() + " bases with " + threads + " threads in 1 chunk");
				
				progressTimer = new javax.swing.Timer(100, new ActionListener() {

					@Override
					public void actionPerformed(ActionEvent arg0) {
						emitProgressString(caller, intervalExtent);
					}
				});
				progressTimer.setDelay(500);
				progressTimer.start();
			}
			
			caller.run();
			allVars = caller.getVariantList();
		}
		
		if (progressTimer != null)
			progressTimer.stop();
		
		Collections.sort(allVars);
		
		//Write the variants to a file
		PrintWriter writer = new PrintWriter(new PrintStream(new FileOutputStream(destination)));
		
		for(Variant var : allVars) {
			writer.println(var);
		}
		
		writer.close();
	}

	protected void emitProgressString(HasBaseProgress caller, int intervalExtent) {
		double basesCalled = (double)caller.getBasesCalled();
		double frac = basesCalled / intervalExtent;
		if (startTime == null) {
			startTime = System.currentTimeMillis();
			System.out.println("   Elapsed       Bases      Bases / sec   % Complete");
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
		String msg = cm + "  " + toUserTime(elapsedSecs) + " " + padTo("" + intFormatter.format(basesCalled), 12) + "  " + padTo("" + formatter.format(basesPerSec), 12) + "  " + padTo(formatter.format(100.0*frac), 8) + "%";
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
	@Override
	public void emitUsage() {
		System.out.println("Predictor (SNP caller) module");
		System.out.println(" -R reference file");
		System.out.println(" -B input BAM file");
		System.out.println(" -V output variant file");
		System.out.println(" -M model file produced by buildmodel");
	}

	private Long startTime = null;
	private int prevLength = 0;
	private int charIndex = 0;
	private static final char[] markers = {'|', '/', '-', '\\', '|', '/', '-', '\\'};
}
