package libsvm;

import java.io.File;

public class LIBSVMTrain extends LIBSVMTool {

	public static final String trainingExecutable = "svm-train";
	
	private double defaultC = 250.0;
	private double defaultG = 0.005;
	
	public LIBSVMModel createModel(File trainingData) {
		return createModel(trainingData, false);
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
		if (! initialized) {
			initialize();
		}

		if (scaleDataFirst) {
			LIBSVMScale scaler = new LIBSVMScale();
			File scaledData = scaler.scaleData(trainingData);
			trainingData = scaledData;
		}
		
		String pathToModel = destinationModelFile.getAbsolutePath();
		LIBSVMModel model = new LIBSVMModel(new File(pathToModel));
		String command = libsvmPath + trainingExecutable + " -t 2 -b 1 -c " + defaultC + " -g " + defaultG + " " + trainingData.getAbsolutePath() + " " + pathToModel;
		executeCommand(command);
		return model;
	}
	
}
