package snpsvm.counters;

import java.util.Iterator;

import snpsvm.bamreading.AlignmentColumn;
import snpsvm.bamreading.FastaWindow;
import snpsvm.bamreading.MappedRead;

/**
 * Attempts to compute something similar to a GaTK / samtools calculation, where
 * we want to find the probability that this site contains either a heterozygous or a homozygous non-reference call
 * @author brendan
 *
 */
public class BinomProbComputer implements ColumnComputer {

	Double[] value = new Double[1];
	
	@Override
	public Double[] computeValue(FastaWindow window, AlignmentColumn col) {
		int refCount = 0;
		int altCount = 0;
		if (col.getDepth() > 0) {
			final char refBase = window.getBaseAt(col.getCurrentPosition());
			Iterator<MappedRead> it = col.getIterator();
			while(it.hasNext()) {
				MappedRead read = it.next();
				if (read.hasBaseAtReferencePos(col.getCurrentPosition())) {
					byte b = read.getBaseAtReferencePos(col.getCurrentPosition());
					byte q = read.getQualityAtReferencePos(col.getCurrentPosition());
					if (b == 'N' || q < 10.0) 
						continue;
					if (b == refBase)
						refCount++;
					else
						altCount++;
				}
			}
		}
		
		
		
		int T = refCount + altCount;
		int X = altCount;
		
		if (T > 250) {
			X = (250 * X)/T;
			T = 250;
		}
		
		
		//Compute het prob
		//Each read has 50% chance of coming from source with a non-reference base
		double hetProb = binomPDF((int)Math.round(X), (int)Math.round(T), 0.5);
		
		
		//Compute homo non-reference prob
		double homNonRefProb = binomPDF((int)Math.round(X), (int)Math.round(T), 0.99);
		
		//Compute homo-reference prob
		double homRefProb = binomPDF((int)Math.round(X), (int)Math.round(T), 0.005);
		
		double result =  1.0 - homRefProb / (hetProb + homNonRefProb + homRefProb); 
		
		if (result < 0 || result > 1) {
			System.err.println("Whoa, got result : " + result);
		}
		if (hetProb < 0 || hetProb > 1) {
			System.err.println("Whoa, got het probability: " + hetProb);
		}
		if (homNonRefProb < 0 || homNonRefProb > 1) {
			System.err.println("Whoa, got homo non ref probability: " + homNonRefProb);
		}
		if (homRefProb < 0 || homRefProb > 1) {
			System.err.println("Whoa, got homo ref probability: " + homRefProb);
		}
		value[0] = result;
		return value;
	}
	
	public static double binomPDF(int k, int n, double p) {		
		return nChooseK(n, k) * Math.pow(p, k) * Math.pow(1.0-p, n-k);
	}
	
	public static double nChooseK(int n, int k) {
		double prod = 1.0;
		for(double i=1; i<=k; i++) {
			prod *= (double)(n-k+i)/i;
		}
		return prod;
	}

	@Override
	public String getName() {
		return "distro.prob";
	}
}
