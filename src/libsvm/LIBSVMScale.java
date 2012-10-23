package libsvm;

import java.io.File;

/**
 * Simple utility to scale data to fit in (-1..1), which is apparently better from libsvm's standpoint
 * @author brendan
 *
 */
public class LIBSVMScale extends LIBSVMTool {

	public static final String scaleExecutable = "svm-scale";
	private boolean initialized = false;

	public File scaleData(File rawData) {
		if (! initialized) {
			initialize();
		}
		
		String pathToOutput = rawData.getAbsolutePath() + ("." + (int)(10000.0*Math.random())) + ".scaled";

		String command = libsvmPath + scaleExecutable + " " + rawData.getAbsolutePath() + " > " + pathToOutput;
		
		//System.out.println("Executing command : " + command);
		
		executeCommand(command);
		
		return new File(pathToOutput);
	}
}
