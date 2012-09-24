package snpsvm.bamreading;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import libsvm.LIBSVMResult;

public class ResultEmitter {

	public void writeResults(File positionsFile, LIBSVMResult result, File destinationFile) throws IOException {
		BufferedWriter writer = new BufferedWriter(new FileWriter(destinationFile));
		BufferedReader resultReader = new BufferedReader(new FileReader(result.getFilePath()));
		BufferedReader posReader = new BufferedReader(new FileReader(positionsFile));
		
		String resultLine = resultReader.readLine();
		resultLine = resultReader.readLine();
		String posLine = posReader.readLine();
		while(resultLine != null && posLine != null) {
			if (resultLine.startsWith("1")) {
				writer.write(posLine.replace(":", "\t") + "\n");
			}
				
			resultLine = resultReader.readLine();
			posLine = posReader.readLine();
		}
		
		//Sanity check, make sure depths match
//		int poslineDepth = parseDepthFromPosLine(posLine);
//		int resultLineDepth = parseDepthFromResultLine(resultLine);
//		
//		if (poslineDepth != resultLineDepth) {
//			System.err.println("Warning: depths are not consistent, depth from pos line: " + poslineDepth + " depth from result line: " + resultLineDepth);
//			System.err.println("Result line: " + resultLine + "\n Pos line: " + posLine);
//		}
		
		writer.close();
		resultReader.close();
		posReader.close();
	}

	private int parseDepthFromResultLine(String resultLine) {
		String[] toks = resultLine.split("\t");
		if (toks.length>1) {
			return Integer.parseInt(toks[1].replace("1:", "").replace(".0", ""));
		}
		return 0;
	}

	private int parseDepthFromPosLine(String posLine) {
		String[] toks = posLine.split("\t");	
		if (toks.length>2) {
			return toks[3].length();
		}
		return 0;
	}
}
