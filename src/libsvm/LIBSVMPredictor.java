package libsvm;

import java.io.File;
import java.io.IOException;

/**
 * Run libsvm-predict given a model and some input data. Results are wrapped in a LIBSVMResult object.
 * Execution  blocks until the operation is complete.
 * @author brendan
 *
 */
public class LIBSVMPredictor {
	
	public static final String defaultPath = "/home/brendan/libsvm-3.12/";
	public static final String predictionExecutable = "svm-predict";
	
	public LIBSVMResult predictData(File inputData, LIBSVMModel model) {
		return predictData(inputData, model, true);
	}
	
	public LIBSVMResult predictData(File inputData, LIBSVMModel model, boolean scaleDataFirst) {
		
		
		if (scaleDataFirst) {
			LIBSVMScale scaler = new LIBSVMScale();
			File scaledData = scaler.scaleData(inputData);
			inputData = scaledData;
		}
		
		String pathToOutput = inputData.getAbsolutePath() + ("." + (int)(1000.0*Math.random())) + ".output";

		String command = defaultPath + predictionExecutable + " -b 1 " + inputData.getAbsolutePath() + " " + model.getModelPath() + " " + pathToOutput ;
		
		ProcessBuilder procBuilder = new ProcessBuilder("bash", "-c", command);
		System.out.println("Executing command : " + command);
		
		try {
			Process proc = procBuilder.start();
			int exitVal = proc.waitFor();
		}
		catch (InterruptedException ex) {
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return new LIBSVMResult(new File(pathToOutput));
	}
}
