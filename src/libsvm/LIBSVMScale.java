package libsvm;

import java.io.File;
import java.io.IOException;

/**
 * Simple utility to scale data to fit in (-1..1), which is apparently better from libsvm's standpoint
 * @author brendan
 *
 */
public class LIBSVMScale {

	public static final String defaultPath = "/home/brendan/libsvm-3.12/";
	public static final String scaleExecutable = "svm-scale";
	
	public File scaleData(File rawData) {
		
		String pathToOutput = rawData.getAbsolutePath() + ("." + (int)(10000.0*Math.random())) + ".scaled";

		String command = defaultPath + scaleExecutable + " " + rawData.getAbsolutePath() + " > " + pathToOutput;
		
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
		
		return new File(pathToOutput);
	}
}
