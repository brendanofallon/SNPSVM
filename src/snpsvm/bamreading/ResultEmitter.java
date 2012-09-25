package snpsvm.bamreading;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import libsvm.LIBSVMResult;

public class ResultEmitter {

	public void writeResults(LIBSVMResult result, File destinationFile) throws IOException {
		BufferedWriter writer = new BufferedWriter(new FileWriter(destinationFile));
		BufferedReader resultReader = new BufferedReader(new FileReader(result.getFilePath()));
		BufferedReader posReader = new BufferedReader(new FileReader(result.getPositionsFile()));
		
		String resultLine = resultReader.readLine();
		resultLine = resultReader.readLine();
		String posLine = posReader.readLine();
		while(resultLine != null && posLine != null) {
			
			//double qScore = parseQuality(resultLine);
			
			if (resultLine.startsWith("1")) {
				//writer.write(posLine.replace(":", "\t") + "\n");
				toCSVLine(posLine, resultLine, writer);
			}
				
			resultLine = resultReader.readLine();
			posLine = posReader.readLine();
		}
				
		writer.close();
		resultReader.close();
		posReader.close();
	}
	
	
	private double parseQuality(String resultLine) {
		String[] resToks = resultLine.split(" ");
		Double p1 = Double.parseDouble(resToks[1]);
		Double p2 = Double.parseDouble(resToks[2]);
		double quality = computeQuality(p1, p2);
		return quality;
	}


	public static void toCSVLine(String posLine, String resultLine, Writer output) throws IOException {
		String[] posToks = posLine.split(":");
		String[] resToks = resultLine.split(" ");
		
		char ref = posToks[2].charAt(0);
		String contig = posToks[0];
		String pos = posToks[1];
		String alt = computeAlt(ref, posToks[3]);
		int depth = posToks[3].length();
		
		Double p1 = Double.parseDouble(resToks[1]);
		Double p2 = Double.parseDouble(resToks[2]);
		double quality = computeQuality(p1, p2);
		
		String zyg = "het";
		
		output.write(contig + "\t" + pos + "\t" + (pos+1) + "\t" + ref + "\t" + alt + "\t" + quality + "\t" + depth + "\t" + zyg + "\n");
	}

	/**
	 * Clumsy procedure to figure out alt allele....
	 * @param ref
	 * @param string
	 * @return
	 */
	private static String computeAlt(char ref, String pileup) {
		int A = 0;
		int C = 0;
		int G = 0;
		int T = 0;
		
		for(int i=0; i<pileup.length(); i++) {
			char c = pileup.charAt(i);
			if (c != ref) {
				switch(c) {
				case 'A' : A++; break;
				case 'C' : C++; break;
				case 'G' : G++; break;
				case 'T' : T++; break;
				}
			}
		}
		
		//Find max of all...
		if (A >= C && A>=G && A>=T) {
			return "A";
		}
		if (C >= A && C>=G && C>=T) {
			return "C";
		}
		if (G >= A && G>=C && G>=T) {
			return "G";
		}
		if (T >= A && T>=C && T>=G) {
			return "T";
		}
		
		
		return "?";
	}


	/**
	 * Returns phred-scaled quality value
	 * @param p1
	 * @param p2
	 * @return
	 */
	public static double computeQuality(double p1, double p2) {
		return -10 * Math.log10( p1 / (p1+p2));
	}
	
	
	
}
