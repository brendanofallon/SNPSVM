package snpsvm.app;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import snpsvm.bamreading.IntervalList;
import snpsvm.bamreading.SplitSNPAndCall;
import snpsvm.bamreading.Variant;
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
	
	public static void callSNPs(File inputBAM, 
			File ref,
			File model,
			File destination,
			IntervalList intervals,
			List<ColumnComputer> counters) throws IOException {
		
		//Somehow logically divide work into rational number of workers
		//No clue what the optimum will be here
		int numThreads = 2;
		ThreadPoolExecutor threadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(numThreads);
		
		//SNPCaller snpCaller = new SNPCaller(ref, inputBAM, model, intervals, counters);
		
		SplitSNPAndCall caller = new SplitSNPAndCall(ref, inputBAM, model, counters, threadPool);
		
		caller.submitAll(intervals);
		
		List<Variant> allVars = caller.getAllVariants();
		
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
