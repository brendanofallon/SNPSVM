package snpsvm.app;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import libsvm.LIBSVMModel;
import libsvm.LIBSVMPredictor;
import libsvm.LIBSVMResult;
import snpsvm.bamreading.IntervalList;
import snpsvm.bamreading.IntervalList.Interval;
import snpsvm.bamreading.ReferenceBAMEmitter;
import snpsvm.bamreading.ResultEmitter;
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
		//counters.add( new StrandBiasComputer());
		counters.add( new MismatchComputer());
		
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
		String vcfPath = getRequiredStringArg(args, "-V", "Missing required argument for destination file, use -V");
		IntervalList intervals = getIntervals(args);
		
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
	
	public static void callSNPs(File knownBAM, 
			File ref,
			File model,
			File destination,
			IntervalList intervals,
			List<ColumnComputer> counters) throws IOException {

		ReferenceBAMEmitter emitter = new ReferenceBAMEmitter(ref, knownBAM, counters);
		File data = new File(destination.getName().replace(".vcf", "") + ".data");
		File positionsFile = new File(destination.getName().replace(".vcf", "") + ".pos");
		emitter.setPositionsFile(positionsFile);

		//Read BAM file, write results to training file
		
		PrintStream trainingStream = new PrintStream(new FileOutputStream(data));		
		if (intervals == null) {
			emitter.emitAll(trainingStream); 
		}
		else {
			for(String contig : intervals.getContigs()) {
				System.err.println("Emitting contig : " + contig);
				for(Interval interval : intervals.getIntervalsInContig(contig)) {
					System.err.println("\t interval : " + interval.getFirstPos() + " - " + interval.getLastPos());
					emitter.emitWindow(contig, interval.getFirstPos(), interval.getLastPos(), trainingStream);
				}
			}
		}
		trainingStream.close();

		LIBSVMPredictor predictor = new LIBSVMPredictor();
		LIBSVMResult result = predictor.predictData(data, new LIBSVMModel(model));
		result.setPositionsFile(positionsFile);

		ResultEmitter resultWriter = new ResultEmitter();

		resultWriter.writeResults(result, destination);

	}

}