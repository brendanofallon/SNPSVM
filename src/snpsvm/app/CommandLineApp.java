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
	
	
	//Stores some useful, persistent key=value pairs set by user, such as location of libsvm
	public static ConfigModule configModule = new ConfigModule();
	
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
		
		mainTimer.stop();
		System.err.println("Elapsed time :  " + mainTimer.getTotalTimeSeconds() + " seconds");
	}
}
