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

	double[] value = new double[1];
	
	@Override
	public double[] computeValue(final char refBase, FastaWindow window, AlignmentColumn col) {
		int refCount = 0;
		int altCount = 0;
		if (col.getDepth() > 0) {
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
		double hetProb = util.Math.binomPDF((int)Math.round(X), (int)Math.round(T), 0.5);
		
		
		//Compute homo non-reference prob
		double homNonRefProb = util.Math.binomPDF((int)Math.round(X), (int)Math.round(T), 0.99);
		
		//Compute homo-reference prob
		double homRefProb = util.Math.binomPDF((int)Math.round(X), (int)Math.round(T), 0.005);
		
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
		value[0] = value[0]*2.0 - 1.0; //Scale to between -1 and 1
		return value;
	}
	

	@Override
	public String getName() {
		return "distro.prob";
	}


	@Override
	public int getColumnCount() {
		return value.length;
	}


	@Override
	public String getColumnDesc(int which) {
		return "probability of non-reference base at site";
	}
}
