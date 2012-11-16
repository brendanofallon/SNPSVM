package snpsvm.app;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import libsvm.LIBSVMModel;
import libsvm.LIBSVMTrain;
import snpsvm.bamreading.FastaIndex.IndexNotFoundException;
import snpsvm.bamreading.IntervalList;
import snpsvm.bamreading.IntervalList.Interval;
import snpsvm.bamreading.TrainingEmitter;
import snpsvm.counters.BinomProbComputer;
import snpsvm.counters.ColumnComputer;
import snpsvm.counters.CounterSource;
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

public class ModelBuilder extends AbstractModule {
	
	public ModelBuilder() {
	}
	
	@Override
	public boolean matchesModuleName(String name) {
		return name.equalsIgnoreCase("buildmodel");
	}

	@Override
	public void performOperation(String name, ArgParser args) {
		String referencePath;
		String trueTrainingPath;
		String falseTrainingPath;
		String modelPath;
		String inputBAMPath;
		
		try {
			referencePath = getRequiredStringArg(args, "-R", "Missing required argument for reference file, use -R");
			trueTrainingPath = getRequiredStringArg(args, "-T", "Missing required argument for true training sites file, use -T");
			falseTrainingPath = getRequiredStringArg(args, "-F", "Missing required argument for false training sites file, use -F");
			inputBAMPath = getRequiredStringArg(args, "-B", "Missing required argument for input BAM file, use -B");
			modelPath = getRequiredStringArg(args, "-M", "Missing required argument for model destination file, use -M");

		} catch (MissingArgumentException e1) {
			System.err.println(e1.getMessage());
			return;
		}
		
		//See if user has asked for training data to be appended to existing data file. 
		String existingDataPath = getOptionalStringArg(args, "-A");
		File existingDataFile = null;
		if (existingDataPath != null) {
			existingDataFile = new File(existingDataPath);
			if (! existingDataFile.exists()) {
				System.err.println("Could not find specified training data file: " + existingDataFile.getAbsolutePath());
			}
		}
		
		IntervalList intervals = getIntervals(args);
		
		File referenceFile = new File(referencePath);
		File trueTraining = new File(trueTrainingPath);
		File falseTraining = new File(falseTrainingPath);
		File inputBAM = new File(inputBAMPath);
		
		File modelDestination = new File(modelPath);
		
		
		try {
			
			generateModel(inputBAM, 
					referenceFile, 
					trueTraining, 
					falseTraining, 
					modelDestination,
					existingDataFile,
					intervals);
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IndexNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}

	public static void generateModel(File knownBAM, 
			File ref, 
			File trueTraining, 
			File falseTraining,
			File modelFile,
			File extantData) throws IOException, IndexNotFoundException {
		
		generateModel(knownBAM, ref, trueTraining, falseTraining, modelFile, null);
	}

	public static void generateModel(File knownBAM, 
			File ref, 
			File trueTraining, 
			File falseTraining,
			File modelFile,
			File extantData,
			IntervalList intervals) throws IOException, IndexNotFoundException {
		
		List<ColumnComputer> counters = CounterSource.getCounters();
		
		TrainingEmitter emitter = new TrainingEmitter(trueTraining, falseTraining, ref, knownBAM, counters);
		
		//Read BAM file, write results to training file
		File trainingFile = null;
		PrintStream trainingStream = null;
		if (extantData == null) {
			trainingFile = new File(knownBAM.getName().replace(".bam", "") + ".training.csv");
			trainingStream = new PrintStream(new FileOutputStream(trainingFile));
		}
		else {
			System.out.println("Appending training data to existing data file " + extantData.getName());
			trainingStream = new PrintStream(new FileOutputStream(extantData));
		}
		
		if (intervals == null) {
			emitter.emitAll(trainingStream); 
		}
		else {
			for(String contig : intervals.getContigs()) {
				for(Interval interval : intervals.getIntervalsInContig(contig)) {
					emitter.emitWindow(contig, interval.getFirstPos(), interval.getLastPos(), trainingStream);
				}
			}
		}
		trainingStream.close();
		
		emitter.emitTrainingCounts();
		
		LIBSVMTrain trainer = new LIBSVMTrain();
		LIBSVMModel model = trainer.createModel(trainingFile, modelFile, true);
		
		System.out.println("\n Created training data file: " + trainingFile);
		System.out.println("\n Created model file: " + modelFile);
	}

	@Override
	public void emitUsage() {
		System.out.println("Model Builder : Create a new SNP calling model from a .BAM file and VCF files specifying sites at which known true and false positive variant calls exist");
		System.out.println("  -R reference file (fasta-formatted)");
		System.out.println("  -T true variant sites VCF file");
		System.out.println("  -F false / invariant sites VCF file");
		System.out.println("  -B input BAM file");
		System.out.println("  -M name of output .model file");
		System.out.println("Optional :");
		System.out.println("  -A [existing training data file]   Append data to existing training file");
	}
}
