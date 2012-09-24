package libsvm;

import java.io.File;

public class LIBSVMModel {

	public static String defaultPath = "/home/brendan/libsvm-3.12/";
	private File modelFile = null;
	
	public LIBSVMModel(File file) {
		this.modelFile = file;
	}
	
	/**
	 * Obtain the absolute path to the file containing the model
	 * @return
	 */
	public String getModelPath() {
		return modelFile.getAbsolutePath();
	}
}
