package snpsvm.app;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import snpsvm.bamreading.BAMWindowStore;
import snpsvm.bamreading.FastaReader;
import snpsvm.bamreading.IntervalList;
import snpsvm.bamreading.SplitSNPAndCall;
import snpsvm.bamreading.Variant;
import snpsvm.bamreading.IntervalList.Interval;
import snpsvm.counters.BinomProbComputer;
import snpsvm.counters.ColumnComputer;
import snpsvm.counters.DepthComputer;
import snpsvm.counters.DistroProbComputer;
import snpsvm.counters.MQComputer;
import snpsvm.counters.MeanQualityComputer;
import snpsvm.counters.MismatchComputer;
import snpsvm.counters.NearbyQualComputer;
import snpsvm.counters.PosDevComputer;
import snpsvm.counters.QualSumComputer;
import snpsvm.counters.ReadPosCounter;
import snpsvm.counters.StrandBiasComputer;

public class Predictor extends AbstractModule {

	List<ColumnComputer> counters;
	
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
//		counters.add( new HomopolymerRunCounter());
//		counters.add( new DinucRepeatCounter());
//		counters.add( new NucDiversityCounter());
//		counters.add( new ContextComputer());
	}
	
	@Override
	public boolean matchesModuleName(String name) {
		return name.equalsIgnoreCase("predict");
	}

	@Override
	public void performOperation(String name, ArgParser args) {
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
	
	public static void callSNPs(File inputBAM, 
			File ref,
			File model,
			File destination,
			IntervalList intervals,
			List<ColumnComputer> counters) throws IOException {
		
		intervals = validateIntervals(ref, intervals);
		
		//Initialize BAMWindow store
		BAMWindowStore bamWindows = new BAMWindowStore(inputBAM, 8);
		
		//Somehow logically divide work into rational number of workers
		//No clue what the optimum will be here
		
		List<Variant> allVars; 
		int threads= CommandLineApp.configModule.getThreadCount();
		if (threads > 1) {
			ThreadPoolExecutor threadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(threads);

			SplitSNPAndCall caller = new SplitSNPAndCall(ref, bamWindows, model, counters, threadPool);

			caller.submitAll(intervals);

			allVars = caller.getAllVariants();

		}
		else {
			SNPCaller caller = new SNPCaller(ref, model, intervals, counters, bamWindows);
			caller.run();
			allVars = caller.getVariantList();
		}
		
		for(Variant var : allVars) {
			System.out.println(var);
		}
	}

	@Override
	public void emitUsage() {
		System.out.println("Predictor (SNP caller) module");
		System.out.println(" -R reference file");
		System.out.println(" -B input BAM file");
		System.out.println(" -V output variant file");
		System.out.println(" -M model file produced by buildmodel");
	}

}
