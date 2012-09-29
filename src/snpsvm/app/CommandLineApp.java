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
import libsvm.LIBSVMTrain;
import snpsvm.bamreading.ReferenceBAMEmitter;
import snpsvm.bamreading.ResultEmitter;
import snpsvm.bamreading.TrainingEmitter;
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
import util.Timer;

public class CommandLineApp {

	
	public static void callSNPs(File knownBAM, 
								File ref,
								LIBSVMModel model,
								List<ColumnComputer> counters) throws IOException {

		ReferenceBAMEmitter emitter = new ReferenceBAMEmitter(ref, knownBAM, counters);
		File positionsFile = new File("/home/brendan/bamreading/chr16-22.pos");
		emitter.setPositionsFile(positionsFile);
		
		//Read BAM file, write results to training file
		File trainingFile = new File("/home/brendan/bamreading/tc16-22.csv");
		PrintStream trainingStream = new PrintStream(new FileOutputStream(trainingFile));		
		emitter.emitContig("16", trainingStream);
		emitter.emitContig("17", trainingStream);
		emitter.emitContig("18", trainingStream);
		emitter.emitContig("19", trainingStream);
		emitter.emitContig("20", trainingStream);
		emitter.emitContig("21", trainingStream);
		emitter.emitContig("22", trainingStream);
		trainingStream.close();
		
		LIBSVMPredictor predictor = new LIBSVMPredictor();
		LIBSVMResult result = predictor.predictData(trainingFile, model);
		result.setPositionsFile(positionsFile);
		
		ResultEmitter resultWriter = new ResultEmitter();
		
		resultWriter.writeResults(result, new File("/home/brendan/bamreading/chr16-22calls.csv"));

	}

	
	public static void predictCV(File knownBAM, 
			File ref, 
			File trueTraining, 
			File falseTraining, 
			LIBSVMModel model,
			List<ColumnComputer> counters) throws IOException {
		
		
		TrainingEmitter emitter = new TrainingEmitter(trueTraining, falseTraining, ref, knownBAM, counters);
		File positionsFile = new File("/home/brendan/bamreading/chr16-22.pos");
		emitter.setPositionsFile(positionsFile);
		
		//Read BAM file, write results to training file
		File trainingFile = new File("/home/brendan/bamreading/tc16-22.csv");
		PrintStream trainingStream = new PrintStream(new FileOutputStream(trainingFile));
		emitter.emitContig("16", trainingStream);
		emitter.emitContig("17", trainingStream);
		emitter.emitContig("18", trainingStream);
		emitter.emitContig("19", trainingStream);
		emitter.emitContig("20", trainingStream);
		emitter.emitContig("21", trainingStream);
		emitter.emitContig("22", trainingStream);
		trainingStream.close();
		
		LIBSVMPredictor predictor = new LIBSVMPredictor();
		LIBSVMResult result = predictor.predictData(trainingFile, model);
		result.setPositionsFile(positionsFile);
		
		ResultEmitter resultWriter = new ResultEmitter();
		
		resultWriter.writeResults(result, new File("/home/brendan/bamreading/chr16-22.cv.csv"));

		
	}
	
	public static void generateModel(File knownBAM, 
			File ref, 
			File trueTraining, 
			File falseTraining,
			File modelFile,
			List<ColumnComputer> counters) throws IOException {
		
		TrainingEmitter emitter = new TrainingEmitter(trueTraining, falseTraining, ref, knownBAM, counters);
		
		
		//Read BAM file, write results to training file
		File trainingFile = new File("/home/brendan/bamreading/NA12878.chr1-15.training.csv");
		PrintStream trainingStream = new PrintStream(new FileOutputStream(trainingFile));
		emitter.emitContig("1", trainingStream);
		emitter.emitContig("2", trainingStream);
		emitter.emitContig("3", trainingStream);
		emitter.emitContig("4", trainingStream);
		emitter.emitContig("5", trainingStream);
		emitter.emitContig("6", trainingStream);
		emitter.emitContig("7", trainingStream);
		emitter.emitContig("9", trainingStream);
		emitter.emitContig("10", trainingStream);
		emitter.emitContig("11", trainingStream);
		emitter.emitContig("12", trainingStream);
		emitter.emitContig("13", trainingStream);
		emitter.emitContig("14", trainingStream);
		emitter.emitContig("15", trainingStream);
		
		
		trainingStream.close();
		
		LIBSVMTrain trainer = new LIBSVMTrain();
		LIBSVMModel model = trainer.createModel(trainingFile, modelFile, true);
		
		emitter.emitTrainingCounts();
	}
	
	public static void emitData(File bamFile, 
			File ref, 
			File outputFile,
			List<ColumnComputer> counters) throws IOException {
		
		ReferenceBAMEmitter emitter = new ReferenceBAMEmitter(ref, bamFile, counters);
		File positionsFile = new File("/home/brendan/bamreading/chr5.pos");
		emitter.setPositionsFile(positionsFile);
		
		//Read BAM file, write results to output file
		PrintStream trainingStream = new PrintStream(new FileOutputStream(outputFile));
		emitter.emitContig("5", trainingStream);
		trainingStream.close();
		
	}
	
	public static void main(String[] args) throws IOException {

		Timer mainTimer = new Timer("main");
		mainTimer.start();
		
		if (args.length > 0) {
			ModuleList modules = new ModuleList();
			Module mod = modules.getModuleForName(args[0]);
			if (mod == null) {
				System.err.println("Could not find a module with name: " + args[0]);
				return;
			}
			System.err.println("Loading module " + mod.getClass().toString().replace(".class", ""));
			
			ArgParser argParser = new ArgParser(args);
			mod.performOperation(args[0], argParser);
			
			mainTimer.stop();
			System.err.println("Elapsed time :  " + mainTimer.getTotalTimeSeconds() + " seconds");
			
			return;
		}
		
		

		
		
		
		File reference = new File("/home/brendan/resources/human_g1k_v37.fasta");
		File trueTraining = new File("/home/brendan/bamreading/NA12878_auto.q0.highqual.known.csv");
		//File trueTraining = new File("/home/brendan/bamreading/medtest.knowns.csv");
		File falseTraining = new File("/home/brendan/bamreading/NA12878_auto.q0.loqual.novel.csv");
		List<ColumnComputer> counters = new ArrayList<ColumnComputer>();
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
		
		File inputBAM = new File("/home/brendan/bamreading/NA12878_auto.final.bam");
//		File outputFile = new File("/home/brendan/bamreading/testoutput.csv");
//		emitData(inputBAM, reference, outputFile, counters);
//		
//		System.exit(0);
		
		//LIBSVMModel model = new LIBSVMModel(new File("/home/brendan/bamreading/tc1-4.model"));
		
		//Create model
		File modelFile = new File("/home/brendan/bamreading/NA12878_auto.chr1-15.model");
		generateModel(inputBAM, reference, trueTraining, falseTraining, modelFile, counters);
		System.err.println("Model gen time :  " + mainTimer.getTotalTimeSeconds() + " seconds");
		
		LIBSVMModel model = new LIBSVMModel(modelFile);
		
		//predictCV(inputBAM, reference, trueTraining, falseTraining, model, counters);
		
		
		callSNPs(inputBAM, reference, model, counters);	
		
		
		mainTimer.stop();
		System.err.println("Elapsed time :  " + mainTimer.getTotalTimeSeconds() + " seconds");
	}
}
