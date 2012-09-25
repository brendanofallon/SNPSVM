package libsvm;

import java.io.File;
import java.io.IOException;

public class LIBSVMTrain {

	public static final String defaultPath = "/home/brendan/libsvm-3.12/";
	public static final String trainingExecutable = "svm-train";
	
	public LIBSVMModel createModel(File trainingData) {
		return createModel(trainingData, true);
	}
	
	/**
	 * Train a new model and store the result in a model with a made-up name
	 * @param trainingData
	 * @return
	 */
	public LIBSVMModel createModel(File trainingData, boolean scaleDataFirst) {
		String pathToModel = trainingData.getAbsolutePath() + ("." + (int)(10000.0*Math.random())) + ".model";
		return createModel(trainingData, new File(pathToModel), scaleDataFirst);
	}
	
	/**
	 * Train a new model and the store the result in the given destination file
	 * @param trainingData
	 * @param destinationModelFile
	 * @return
	 */
	public LIBSVMModel createModel(File trainingData, File destinationModelFile, boolean scaleDataFirst) {
		

		if (scaleDataFirst) {
			LIBSVMScale scaler = new LIBSVMScale();
			File scaledData = scaler.scaleData(trainingData);
			trainingData = scaledData;
		}
		
		String pathToModel = destinationModelFile.getAbsolutePath();
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
