package snpsvm.bamreading;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.DecimalFormat;

import libsvm.LIBSVMResult;

public class ResultEmitter {

	
	private static DecimalFormat formatter = new DecimalFormat("0.0##");
	
	public void writeResults(LIBSVMResult result, File destinationFile) throws IOException {
		
		BufferedReader resultReader = new BufferedReader(new FileReader(result.getFilePath()));
		BufferedReader posReader = new BufferedReader(new FileReader(result.getPositionsFile()));
		
		String resultLine = resultReader.readLine();
		//Columns may be in any order, so parse the first result line to figure out which one's which
		String[] toks = resultLine.split(" ");
		if (toks.length < 3) {
			resultReader.close();
			posReader.close();
			throw new IllegalArgumentException("Incorrect number of tokens in first result line, can't figure out what the columns are");
		}
		
		BufferedWriter writer = new BufferedWriter(new FileWriter(destinationFile));
		int varIndex = 1;
		int noVarIndex = 2;
		if (toks[2].equals("1")) {
			varIndex = 2;
			noVarIndex = 1;
		}
		
		
		writer.write("#contig\t start\t end\t ref\t alt\t quality\t depth\t zyg\t homRefProb\t hetProb\t homNonRefProb\n");
		
		resultLine = resultReader.readLine();
		String posLine = posReader.readLine();
		while(resultLine != null && posLine != null) {	
			toks = resultLine.split(" ");
			double qScore = parseQuality(toks[noVarIndex], toks[varIndex]);
			if (qScore > 1) {
				//writer.write(posLine.replace(":", "\t") + "\n");
				toCSVLine(posLine, qScore, writer);
			}
				
			resultLine = resultReader.readLine();
			posLine = posReader.readLine();
		}
				
		writer.close();
		resultReader.close();
		posReader.close();
	}
	
	
	private double parseQuality(String varProb, String noVarProb) {
		Double p1 = Double.parseDouble(varProb);
		Double p2 = Double.parseDouble(noVarProb);
		double quality = computeQuality(p1, p2);
		return quality;
	}


	public static void toCSVLine(String posLine, double qScore, Writer output) throws IOException {
		String[] posToks = posLine.split(":");
		
		
		char ref = posToks[2].charAt(0);
		String contig = posToks[0];
		String pos = posToks[1];
		String pileUp = posToks[3];
		int end = (Integer.parseInt(pos)+1);
		char alt = computeAlt(ref, pileUp);
		int depth = posToks[3].length();
		

		int T = pileUp.length();
		int X = count(alt, pileUp);
		
		if (T > 250) {
			X = (250 * X)/T;
			T = 250;
		}
		
		
		//Compute het prob
		//Each read has 50% chance of coming from source with a non-reference base
		double hetProb = util.Math.binomPDF((int)Math.round(X), (int)Math.round(T), 0.5);
		
		
		//Compute homo non-reference prob
		double homNonRefProb = util.Math.binomPDF((int)Math.round(X), (int)Math.round(T), 0.99);
		
		//Compute homo-reference prob
		double homRefProb = util.Math.binomPDF((int)Math.round(X), (int)Math.round(T), 0.005);
		
		double tot = hetProb + homNonRefProb + homRefProb;
		
		double varProb = 1.0 - homRefProb / (homRefProb + hetProb + homNonRefProb);
		
		String zyg = "het";
		if (homNonRefProb > hetProb) {
			zyg = "hom";
		}
		
		output.write(contig + "\t" + pos + "\t" + (end) + "\t" + ref + "\t" + alt + "\t" + formatter.format(qScore) + "\t" + depth + "\t" + zyg + "\t" + formatter.format(homRefProb/tot) + "\t" + formatter.format(hetProb/tot) + "\t" + formatter.format(homNonRefProb/tot) + "\n");
	}

	/**
	 * Return number of occurrences of c in str
	 * @param c
	 * @param str
	 * @return
	 */
	private static int count(char c, String str) {
		int count = 0;
		for(int i=0; i<str.length(); i++) {
			if (str.charAt(i) == c) 
				count++;
		}
		return count;
	}
	
	/**
	 * Clumsy procedure to figure out alt allele....
	 * @param ref
	 * @param string
	 * @return
	 */
	private static char computeAlt(char ref, String pileup) {
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
		if (ref != 'A' && A >= C && A>=G && A>=T) {
			return 'A';
		}
		if (ref != 'C' && C >= A && C>=G && C>=T) {
			return 'C';
		}
		if (ref != 'G' && G >= A && G>=C && G>=T) {
			return 'G';
		}
		if (ref != 'T' && T >= A && T>=C && T>=G) {
			return 'T';
		}
		
		
		return '?';
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
