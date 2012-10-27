package snpsvm.counters;

import java.util.Iterator;

import snpsvm.bamreading.AlignmentColumn;
import snpsvm.bamreading.FastaWindow;
import snpsvm.bamreading.MappedRead;

/**
 * Computes deviation in read position   
 * @author brendan
 *
 */
public class PosDevComputer extends VarCountComputer {

	//Buffers used to compute variance in single pass
	private final double[] M = new double[2];
	private final double[] S = new double[2];
	private final double[] K = new double[2];	
	
	@Override
	public String getName() {
		return "pos.dev";
	}


	@Override
	public int getColumnCount() {
		return values.length;
	}


	@Override
	public String getColumnDesc(int which) {
		if (which == ref)
			return "Stdev of read position of reference bases";
		else
			return "Stdev of read position of non-reference bases";
			
	}
	
	@Override
	public double[] computeValue(final char refBase, FastaWindow window, AlignmentColumn col) {
		values[ref] = 0.0;
		values[alt] = 0.0;
		
		M[ref] = 0.0;
		M[alt] = 0.0;
		
		S[ref] = 0.0;
		S[alt] = 0.0;
		
		K[ref] = 0.0;
		K[alt] = 0.0;
		
		if (col.getDepth() > 0) {
			Iterator<MappedRead> it = col.getIterator();
			
			while(it.hasNext()) {
				MappedRead read = it.next();
				if (read.hasBaseAtReferencePos(col.getCurrentPosition())) {
					byte b = read.getBaseAtReferencePos(col.getCurrentPosition());
					if (b == 'N') 
						continue;
					
					int readPos = read.refPosToReadPos(col.getCurrentPosition());
		
					int index = 0;
					if (b != refBase)
						index = 1;
					
					K[index]++; 
					
					double prevM = M[index];
					M[index] += (readPos - M[index])/K[index];
					S[index] += (readPos - prevM)*(readPos-M[index]);
					
					if (K[index] > 1)
						values[index] = S[index]/(K[index]-1);
				}
			}
		}
		
		return values;
	}

}

