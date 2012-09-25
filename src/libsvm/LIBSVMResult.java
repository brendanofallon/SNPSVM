package libsvm;

import java.io.File;

/**
 * Stores some minimal information about the result of a libsvm prediction run.
 * 
 * @author brendan
 *
 */
public class LIBSVMResult {

	private File resultFile = null; //Result of libsvm-predict run
	private File inputDataFile = null; //Data supplied to libsvm-predict, should have same line number as resultFile
	private File positionsFile = null; //Contains position-related info for each line to classify
	
	public LIBSVMResult(File resultsFile) {
		this.resultFile = resultsFile;
	}
	
	/**
	 * Specify path to input data file, useful for parsing and conversion of results to other formats
	 * @param inputData
	 */
	public void setInputData(File inputData) {
		this.inputDataFile = inputData;
	}
	
	public File getPositionsFile() {
		return positionsFile;
	}

	public void setPositionsFile(File positionsFile) {
		this.positionsFile = positionsFile;
	}

	/**
	 * Obtain the path to the input data file that was used in the libsvm-predict run, may be null
	 * @return
	 */
	public String getInputDataPath() {
		if (inputDataFile == null)
			return null;
		else
			return inputDataFile.getAbsolutePath();
	}
	
	/**
	 * Get path to prediction result file
	 * @return
	 */
	public String getFilePath() {
		return resultFile.getAbsolutePath();
	}
}
