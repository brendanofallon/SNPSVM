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
import snpsvm.bamreading.ResultEmitter;
import snpsvm.bamreading.TrainingEmitter;
import snpsvm.counters.ColumnComputer;
import snpsvm.counters.DepthComputer;
import snpsvm.counters.MQComputer;
import snpsvm.counters.PosDevComputer;
import snpsvm.counters.QualSumComputer;
import util.Timer;

public class CommandLineApp {

	
	public static void predictCV(File knownBAM, 
			File ref, 
			File trueTraining, 
			File falseTraining, 
			LIBSVMModel model,
			List<ColumnComputer> counters) throws IOException {
		
		
		TrainingEmitter emitter = new TrainingEmitter(trueTraining, falseTraining, ref, knownBAM, counters);
		File positionsFile = new File("/home/brendan/bamreading/chr56789.pos");
		emitter.setPositionsFile(positionsFile);
		
		//Read BAM file, write results to training file
		File trainingFile = new File("/home/brendan/bamreading/tc56789.csv");
		PrintStream trainingStream = new PrintStream(new FileOutputStream(trainingFile));
		emitter.emitContig("5", trainingStream);
		emitter.emitContig("6", trainingStream);
		emitter.emitContig("7", trainingStream);
		emitter.emitContig("8", trainingStream);
		emitter.emitContig("9", trainingStream);
		trainingStream.close();
		
		LIBSVMPredictor predictor = new LIBSVMPredictor();
		LIBSVMResult result = predictor.predictData(trainingFile, model);
		
		ResultEmitter resultWriter = new ResultEmitter();
		
		resultWriter.writeResults(positionsFile, result, new File("/home/brendan/bamreading/testresults.csv"));

		
	}
	
	public static void main(String[] args) throws IOException {
		
		Timer mainTimer = new Timer("main");
		mainTimer.start();
		
		File reference = new File("/home/brendan/resources/human_g1k_v37.fasta");
		File trueTraining = new File("/home/brendan/resources/1000G_omni2.5.b37.sites.vcf");
		File falseTraining = new File("/home/brendan/bamreading/medtest.chr56789.lowqual.csv");
		File bam = new File("/home/brendan/oldhome/medtest/medtest.final.bam");
		List<ColumnComputer> counters = new ArrayList<ColumnComputer>();
		counters.add( new DepthComputer());
		counters.add( new QualSumComputer());
		counters.add( new PosDevComputer());
		counters.add( new MQComputer());
		
		
		File inputBAM = new File("/home/brendan/oldhome/medtest/medtest.final.bam");
		LIBSVMModel model = new LIBSVMModel(new File("/home/brendan/bamreading/tc1-4.model"));
		predictCV(inputBAM, reference, trueTraining, falseTraining, model, counters);
		
		//Training
//		File trainingFile = new File("/home/brendan/bamreading/tc56789.csv");
//		TrainingEmitter emitter = new TrainingEmitter(trueTraining, falseTraining, reference, bam, counters);
//		PrintStream trainingStream = new PrintStream(new FileOutputStream(trainingFile));
//
//		File positionsFile = new File("/home/brendan/bamreading/chr56789.pos");
//		emitter.setPositionsFile(positionsFile);
		
//		emitter.emitContig("1", trainingStream);
//		emitter.emitContig("2", trainingStream);
//		emitter.emitContig("3", trainingStream);
//		emitter.emitContig("4", trainingStream);
//		emitter.emitContig("5", trainingStream);
//		emitter.emitContig("6", trainingStream);
//		emitter.emitContig("7", trainingStream);
//		emitter.emitContig("8", trainingStream);
//		emitter.emitContig("9", trainingStream);
//		trainingStream.close();
		
		//Create the model
//		File trainingData = new File("/home/brendan/bamreading/trainingChr1scaled.csv");
//		LIBSVMTrain trainer = new LIBSVMTrain();
//		LIBSVMModel model = trainer.createModel(trainingData);
		
		
		//Prediction
//		File destFile = new File("/home/brendan/bamreading/medtest3.csv");
//		destFile.createNewFile();
//		PrintStream outStream = new PrintStream(new FileOutputStream(destFile));
//		
//		
//				
//		ReferenceBAMEmitter emitter = new ReferenceBAMEmitter(reference, bam, counters);
//		emitter.setPositionsFile( positionsFile );
//		emitter.emitContig("10", outStream);
//		
//		outStream.close();

		

		
//		LIBSVMModel model = new LIBSVMModel(new File("/home/brendan/bamreading/trainingChr1scaled.csv.614.model"));
//		File dataFile = new File("/home/brendan/bamreading/medtest3scaled.csv");
//		LIBSVMPredictor predictor = new LIBSVMPredictor();
//		LIBSVMResult result = predictor.predictData(dataFile, model);
//		
//		ResultEmitter resultWriter = new ResultEmitter();
//		
//		resultWriter.writeResults(positionsFile, result, new File("/home/brendan/bamreading/variablepos.csv"));
		
		mainTimer.stop();
		System.err.println("Elapsed time :  " + mainTimer.getTotalTimeSeconds() + " seconds");
	}
}
