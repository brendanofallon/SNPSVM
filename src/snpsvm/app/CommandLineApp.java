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
import snpsvm.counters.ColumnComputer;
import snpsvm.counters.DepthComputer;
import snpsvm.counters.DistroProbComputer;
import snpsvm.counters.MQComputer;
import snpsvm.counters.MismatchComputer;
import snpsvm.counters.NearbyQualComputer;
import snpsvm.counters.PosDevComputer;
import snpsvm.counters.QualSumComputer;
import snpsvm.counters.StrandBiasComputer;
import util.Timer;

public class CommandLineApp {

	
	public static void callSNPs(File knownBAM, 
								File ref,
								LIBSVMModel model,
								List<ColumnComputer> counters) throws IOException {

		ReferenceBAMEmitter emitter = new ReferenceBAMEmitter(ref, knownBAM, counters);
		File positionsFile = new File("/home/brendan/bamreading/chr56789.pos");
		emitter.setPositionsFile(positionsFile);
		
		//Read BAM file, write results to training file
		File trainingFile = new File("/home/brendan/bamreading/tc5.csv");
		PrintStream trainingStream = new PrintStream(new FileOutputStream(trainingFile));
		emitter.emitContig("5", trainingStream);
//		emitter.emitContig("6", trainingStream);
//		emitter.emitContig("7", trainingStream);
//		emitter.emitContig("8", trainingStream);
//		emitter.emitContig("9", trainingStream);
		trainingStream.close();
		
		LIBSVMPredictor predictor = new LIBSVMPredictor();
		LIBSVMResult result = predictor.predictData(trainingFile, model);
		result.setPositionsFile(positionsFile);
		
		ResultEmitter resultWriter = new ResultEmitter();
		
		resultWriter.writeResults(result, new File("/home/brendan/bamreading/testresults.csv"));

	}

	
	public static void predictCV(File knownBAM, 
			File ref, 
			File trueTraining, 
			File falseTraining, 
			LIBSVMModel model,
			List<ColumnComputer> counters) throws IOException {
		
		
		TrainingEmitter emitter = new TrainingEmitter(trueTraining, falseTraining, ref, knownBAM, counters);
		File positionsFile = new File("/home/brendan/bamreading/chr5.pos");
		emitter.setPositionsFile(positionsFile);
		
		//Read BAM file, write results to training file
		File trainingFile = new File("/home/brendan/bamreading/tc5.csv");
		PrintStream trainingStream = new PrintStream(new FileOutputStream(trainingFile));
		emitter.emitContig("5", trainingStream);
//		emitter.emitContig("6", trainingStream);
//		emitter.emitContig("7", trainingStream);
//		emitter.emitContig("8", trainingStream);
//		emitter.emitContig("9", trainingStream);
		trainingStream.close();
		
		LIBSVMPredictor predictor = new LIBSVMPredictor();
		LIBSVMResult result = predictor.predictData(trainingFile, model);
		result.setPositionsFile(positionsFile);
		
		ResultEmitter resultWriter = new ResultEmitter();
		
		resultWriter.writeResults(result, new File("/home/brendan/bamreading/testresults.csv"));

		
	}
	
	public static void generateModel(File knownBAM, 
			File ref, 
			File trueTraining, 
			File falseTraining,
			File modelFile,
			List<ColumnComputer> counters) throws IOException {
		
		TrainingEmitter emitter = new TrainingEmitter(trueTraining, falseTraining, ref, knownBAM, counters);
		
		
		//Read BAM file, write results to training file
		File trainingFile = new File("/home/brendan/bamreading/tc1-4.knowns.csv");
		PrintStream trainingStream = new PrintStream(new FileOutputStream(trainingFile));
		emitter.emitContig("1", trainingStream);
		emitter.emitContig("2", trainingStream);
		emitter.emitContig("3", trainingStream);
		emitter.emitContig("4", trainingStream);
//		emitter.emitContig("5", trainingStream);
//		emitter.emitContig("6", trainingStream);
//		emitter.emitContig("7", trainingStream);
//		emitter.emitContig("8", trainingStream);
//		emitter.emitContig("9", trainingStream);
		trainingStream.close();
		
		LIBSVMTrain trainer = new LIBSVMTrain();
		LIBSVMModel model = trainer.createModel(trainingFile, modelFile, true);
		
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
		
		File reference = new File("/home/brendan/resources/human_g1k_v37.fasta");
		File trueTraining = new File("/home/brendan/resources/1000G_omni2.5.b37.sites.vcf");
		//File trueTraining = new File("/home/brendan/bamreading/medtest.knowns.csv");
		File falseTraining = new File("/home/brendan/bamreading/medtest.chr1234.lowqual.csv");
		List<ColumnComputer> counters = new ArrayList<ColumnComputer>();
		counters.add( new DepthComputer());
		counters.add( new QualSumComputer());
		counters.add( new PosDevComputer());
		counters.add( new MQComputer());
		counters.add( new DistroProbComputer());
		counters.add( new NearbyQualComputer());
		counters.add( new StrandBiasComputer());
		counters.add( new MismatchComputer());
		
		File inputBAM = new File("/home/brendan/oldhome/medtest/medtest.final.bam");
//		File outputFile = new File("/home/brendan/bamreading/testoutput.csv");
//		emitData(inputBAM, reference, outputFile, counters);
//		
//		System.exit(0);
		
		//LIBSVMModel model = new LIBSVMModel(new File("/home/brendan/bamreading/tc1-4.model"));
		
		//Create model
		File modelFile = new File("/home/brendan/bamreading/tc1-4.model");
		generateModel(inputBAM, reference, trueTraining, falseTraining, modelFile, counters);
		
		
		//predictCV(inputBAM, reference, trueTraining, falseTraining, model, counters);
		
		
		callSNPs(inputBAM, reference, new LIBSVMModel(modelFile), counters);		
		
		
		mainTimer.stop();
		System.err.println("Elapsed time :  " + mainTimer.getTotalTimeSeconds() + " seconds");
	}
}
