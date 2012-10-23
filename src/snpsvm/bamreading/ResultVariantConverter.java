package snpsvm.bamreading;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import libsvm.LIBSVMResult;

/**
 * Converts the results of a libsvm prediction run into a list of Variants
 * @author brendanofallon
 *
 */
public class ResultVariantConverter {

	public List<Variant> createVariantList(LIBSVMResult result) throws IOException {

		List<Variant> variants = new ArrayList<Variant>(256); 
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

		int varIndex = 1;
		int noVarIndex = 2;
		if (toks[2].equals("1")) {
			varIndex = 2;
			noVarIndex = 1;
		}
		resultLine = resultReader.readLine();
		String posLine = posReader.readLine();
		while(resultLine != null && posLine != null) {
			toks = resultLine.split(" ");
			double qScore = parseQuality(toks[noVarIndex], toks[varIndex]);
			if (qScore > 1) {
				Variant var = toVariant(posLine, qScore);
				variants.add(var);
			}

			resultLine = resultReader.readLine();
			posLine = posReader.readLine();
		}

		resultReader.close();
		posReader.close();

		return variants;
	}

	public static Variant toVariant(String posLine, double qScore) {
		String[] posToks = posLine.split(":");

		char ref = posToks[2].charAt(0);
		String contig = posToks[0];
		int pos = Integer.parseInt(posToks[1]);
		//int end = pos + 1;
		String pileUp = posToks[3];
		char alt = computeAlt(ref, pileUp);
		//int depth = posToks[3].length();

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
		

		Variant var = new Variant(contig, pos, ref, alt, qScore, hetProb/(hetProb + homRefProb + homNonRefProb));
		return var;
	}

	private double parseQuality(String varProb, String noVarProb) {
		Double p1 = Double.parseDouble(varProb);
		Double p2 = Double.parseDouble(noVarProb);
		double quality = computeQuality(p1, p2);
		return quality;
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
		if (A >= C && A>=G && A>=T) {
			return 'A';
		}
		if (C >= A && C>=G && C>=T) {
			return 'C';
		}
		if (G >= A && G>=C && G>=T) {
			return 'G';
		}
		if (T >= A && T>=C && T>=G) {
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
