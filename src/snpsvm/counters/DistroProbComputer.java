package snpsvm.counters;

import java.util.Iterator;

import snpsvm.bamreading.AlignmentColumn;
import snpsvm.bamreading.FastaWindow;
import snpsvm.bamreading.MappedRead;

public class DistroProbComputer implements ColumnComputer {

	Double[] value = new Double[1];
	
	@Override
	public Double[] computeValue(final char refBase, FastaWindow window, AlignmentColumn col) {
		int refCount = 0;
		int altCount = 0;
		if (col.getDepth() > 0) {
			Iterator<MappedRead> it = col.getIterator();
			while(it.hasNext()) {
				MappedRead read = it.next();
				if (read.hasBaseAtReferencePos(col.getCurrentPosition())) {
					byte b = read.getBaseAtReferencePos(col.getCurrentPosition());
					if (b == 'N') 
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
		
		
		//Compute homo prob
		double homProb = binomPDF((int)Math.round(X), (int)Math.round(T), 0.99);
		
		//Compute error prob
		double errProb = binomPDF((int)Math.round(X), (int)Math.round(T), 0.2);
		
		double result = Math.log(errProb / (hetProb + homProb + errProb)); 
		
		if (result < -10)
			result = -10;
		
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
