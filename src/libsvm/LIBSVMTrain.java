package libsvm;

import java.io.File;
import java.io.IOException;

public class LIBSVMTrain {

	public static final String defaultPath = "/home/brendan/libsvm-3.12/";
	public static final String trainingExecutable = "svm-train";
	
	public LIBSVMModel createModel(File trainingData) {
		
		String pathToModel = trainingData.getAbsolutePath() + ("." + (int)(1000.0*Math.random())) + ".model";
		LIBSVMModel model = new LIBSVMModel(new File(pathToModel));
		String command = defaultPath + trainingExecutable + " -t 2 -b 1 " + trainingData.getAbsolutePath() + " " + pathToModel;
		ProcessBuilder procBuilder = new ProcessBuilder("bash", "-c", command);
		System.out.println("Executing command : " + command);
		System.out.println("Running model, model file will be at : " + pathToModel);
		try {
			Process proc = procBuilder.start();
			int exitVal = proc.waitFor();
		}
		catch (InterruptedException ex) {
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return model;
	}
	
}
