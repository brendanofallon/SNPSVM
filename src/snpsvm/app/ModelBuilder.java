package snpsvm.app;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

import libsvm.LIBSVMModel;
import libsvm.LIBSVMTrain;
import snpsvm.bamreading.IntervalList;
import snpsvm.bamreading.TrainingEmitter;
import snpsvm.counters.ColumnComputer;

public class ModelBuilder extends AbstractModule {

	@Override
	public boolean matchesModuleName(String name) {
		return name.equalsIgnoreCase("buildmodel");
	}

	@Override
	public void performOperation(String name, ArgParser args) {
		String referencePath = getRequiredStringArg(args, "-R", "Missing required argument for reference file, use -R");
		String trueTrainingPath = getRequiredStringArg(args, "-T", "Missing required argument for true training sites file, use -T");
		String falseTrainingPath = getRequiredStringArg(args, "-F", "Missing required argument for false training sites file, use -F");
		String inputBAMPath = getRequiredStringArg(args, "-B", "Missing required argument for input BAM file, use -B");
		String modelPath = getRequiredStringArg(args, "-M", "Missing required argument for model destination file, use -M");
		
		String intervalsStr = getOptionalStringArg(args, "-L");
		IntervalList intervals = new IntervalList();
		
		//Determine if intervalsStr points to a file
		File testFile = new File(intervalsStr);
		if (testFile.exists()) {
			System.err.println("Building interval list from file " + testFile.getName());
			intervals.buildFromBEDFile(testFile);
		}
		else {
			try {
				intervals.buildFromString(intervalsStr);
			}
			catch (Exception ex) {
				System.err.println("Error parsing intervals from " + intervalsStr);
				return;
			}
		}
		
		File referenceFile = new File(referencePath);
		File trueTraining = new File(trueTrainingPath);
		File falseTraining = new File(falseTrainingPath);
		File inputBAM = new File(inputBAMPath);
		
		File modelDestination = new File(modelPath);
		
		
		generateModel(inputBAM, referenceFile, trueTraining, falseTraining, modelDestination, intervals);
		
		
	}


	public static void generateModel(File knownBAM, 
			File ref, 
			File trueTraining, 
			File falseTraining,
			File modelFile,
			IntervalList intervals,
			List<ColumnComputer> counters) throws IOException {
		
		TrainingEmitter emitter = new TrainingEmitter(trueTraining, falseTraining, ref, knownBAM, counters);
		
		
		//Read BAM file, write results to training file
		File trainingFile = new File(knownBAM.getName().replace(".bam", "") + ".training.csv");
		PrintStream trainingStream = new PrintStream(new FileOutputStream(trainingFile));
		
		
		LIBSVMTrain trainer = new LIBSVMTrain();
		LIBSVMModel model = trainer.createModel(trainingFile, modelFile, true);
		trainingStream.close();
	}
}
