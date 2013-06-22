package libsvm;

import java.io.File;

/**
 * Run libsvm-predict given a model and some input data. Results are wrapped in a LIBSVMResult object.
 * Execution  blocks until the operation is complete.
 * @author brendan
 *
 */
public class LIBSVMPredictor extends LIBSVMTool {
	
	
	public static final String predictionExecutable = "svm-predict";
	private boolean initialized = false;
	private boolean removeTempFiles = true;
	
	public LIBSVMResult predictData(File inputData, LIBSVMModel model) {
		return predictData(inputData, model, false);
	}
	
	public LIBSVMResult predictData(File inputData, LIBSVMModel model, boolean scaleDataFirst) {
		if (! initialized) {
			initialize();
		}
		
		if (scaleDataFirst) {
			LIBSVMScale scaler = new LIBSVMScale();
			File scaledData = scaler.scaleData(inputData);
			inputData = scaledData;
		}
		
		String pathToOutput = inputData.getAbsolutePath() + ("." + (int)(1000.0*Math.random())) + ".output";

		String command = libsvmPath + predictionExecutable + " -b 1 " + inputData.getAbsolutePath() + " " + model.getModelPath() + " " + pathToOutput ;
		//System.out.println(command);
		
		executeCommand(command);
		
		File outputData = new File(pathToOutput);
		if (removeTempFiles) {
			outputData.deleteOnExit();
		}
		LIBSVMResult result = new LIBSVMResult(outputData);
		result.setInputData(inputData);
		
		return result;
	}

	public boolean isRemoveTempFiles() {
		return removeTempFiles;
	}

	public void setRemoveTempFiles(boolean removeTempFiles) {
		this.removeTempFiles = removeTempFiles;
	}
	
	
}
