package snpsvm.app;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.text.DecimalFormat;
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
			callSNPs(inputBAM, reference, model, vcf, intervals, counters, writeData);
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
			List<ColumnComputer> counters,
			boolean writeData) throws IOException {
		
		File data = new File(destination.getName().replace(".vcf", "") + ".data");
		File positionsFile = new File(destination.getName().replace(".vcf", "") + ".pos");
		
		if (writeData) {

			ReferenceBAMEmitter emitter = new ReferenceBAMEmitter(ref, knownBAM, counters);
			emitter.setPositionsFile(positionsFile);
			
			PrintStream trainingStream = new PrintStream(new FileOutputStream(data));		
			if (intervals == null) {
				emitter.emitAll(trainingStream); 
			}
			else {

				DecimalFormat formatter = new DecimalFormat("#0.00");
				double ex = intervals.getExtent();
				double counted = 0;
				int index =0 ;
				int prevLength = 0;
				for(String contig : intervals.getContigs()) {
					//System.err.println("Emitting contig : " + contig);
					for(Interval interval : intervals.getIntervalsInContig(contig)) {
						//System.err.println("\t interval : " + interval.getFirstPos() + " - " + interval.getLastPos());
						emitter.emitWindow(contig, interval.getFirstPos(), interval.getLastPos(), trainingStream);
						counted += interval.getLastPos() - interval.getFirstPos();
						index++;
						if (index % 200 ==0) {
							for(int i=0; i<prevLength; i++) {
								System.err.print('\b');
							}
							String msg = "Completed " + ("" + counted).replace(".0", "") + " bases, " + formatter.format(100* counted / ex) + "%";
							prevLength = msg.length();
							System.err.print(msg);
						}
					}
				}
			}
			trainingStream.close();

		}
		LIBSVMPredictor predictor = new LIBSVMPredictor();
		LIBSVMResult result = predictor.predictData(data, new LIBSVMModel(model));
		result.setPositionsFile(positionsFile);

		ResultEmitter resultWriter = new ResultEmitter();

		resultWriter.writeResults(result, destination);

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
