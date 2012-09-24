package libsvm;

import java.io.File;

public class LIBSVMResult {

	private File resultFile = null;
	
	public LIBSVMResult(File resultsFile) {
		this.resultFile = resultsFile;
	}
	
	public String getFilePath() {
		return resultFile.getAbsolutePath();
	}
}
