package snpsvm.bamreading;

import java.io.BufferedReader;
import java.io.File;
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

		//Check validity of result
		File resFile = new File(result.getFilePath());
		if (! resFile.exists()) {
			throw new IllegalStateException("Result file " + result.getFilePath() + " does not exist");
		}
		if (result.getPositionsFile() == null) {
			throw new IllegalStateException("Result position info file has not been set.");
		}
		if ((! result.getPositionsFile().exists())) {
			throw new IllegalStateException("Result position info file " + result.getFilePath() + " does not exist");
		}
		
		List<Variant> variants = new ArrayList<Variant>(256); 
		BufferedReader resultReader = new BufferedReader(new FileReader(result.getFilePath()));
		BufferedReader posReader = new BufferedReader(new FileReader(result.getPositionsFile()));

		//System.out.println("Parsing results from output file: " + result.getFilePath() + " pos file: " + result.getPositionsFile().getName());
		String resultLine = resultReader.readLine();
		if (resultLine == null) {
			resultReader.close();
			posReader.close();
			throw new IllegalStateException("Error reading from prediction result file: " + result.getFilePath());
		}
		
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
			if (qScore > 0.01) {
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
		int[] baseCounts = toBaseCounts(posToks[3]);
		int altIndex = computeAlt(ref, baseCounts); //index of alt allele in base counts array


		int T = baseCounts[0] + baseCounts[1] + baseCounts[2] + baseCounts[3];
		int X = baseCounts[altIndex];
		
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
		
		char alt = 'N';
		if (altIndex == 0)
			alt = 'A';
		if (altIndex == 1)
			alt = 'C';
		if (altIndex == 2)
			alt = 'G';
		if (altIndex == 3)
			alt = 'T';
		
		int depth = baseCounts[0] + baseCounts[1] + baseCounts[2] + baseCounts[3];
		Variant var = new Variant(contig, pos, ref, alt, qScore, depth, hetProb/(hetProb + homRefProb + homNonRefProb));
		return var;
	}

	/**
	 * Parses a comma-separated series of integers to an array
	 * @param countStr
	 * @return
	 */
	private static int[] toBaseCounts(String countStr) {
		int[] counts = new int[4];
		String[] toks = countStr.split(",");
		if (toks.length != 4) {
			throw new IllegalArgumentException("Not exactly four tokens in base count string");
		}
		counts[0] = Integer.parseInt(toks[0]);
		counts[1] = Integer.parseInt(toks[1]);
		counts[2] = Integer.parseInt(toks[2]);
		counts[3] = Integer.parseInt(toks[3]);
		return counts;
	}

	private double parseQuality(String varProb, String noVarProb) {
		Double p1 = Double.parseDouble(varProb);
		Double p2 = Double.parseDouble(noVarProb);
		double quality = computeQuality(p1, p2);
		return quality;
	}
	
	/**
	 * Clumsy procedure to figure out alt allele....
	 * @param ref
	 * @param string
	 * @return
	 */
	private static int computeAlt(char ref, int[] counts) {
		int A = counts[0];
		int C = counts[1];
		int G = counts[2];
		int T = counts[3];

		if (ref == 'A')
			A = 0;
		if (ref == 'C')
			C = 0;
		if (ref == 'G')
			G = 0;
		if (ref == 'T')
			T = 0;
		
		//Find max of all...
		if (A >= C && A>=G && A>=T) {
			return 0;
		}
		if (C >= A && C>=G && C>=T) {
			return 1;
		}
		if (G >= A && G>=C && G>=T) {
			return 2;
		}
		if (T >= A && T>=C && T>=G) {
			return 3;
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
