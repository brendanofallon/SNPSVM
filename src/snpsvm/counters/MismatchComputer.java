package snpsvm.counters;

import java.util.Iterator;

import snpsvm.bamreading.AlignmentColumn;
import snpsvm.bamreading.FastaWindow;
import snpsvm.bamreading.MappedRead;

public class MismatchComputer extends VarCountComputer {
	
	@Override
	public String getName() {
		return "mismatch.counts";
	}
	
	@Override
	public int getColumnCount() {
		return values.length;
	}


	@Override
	public String getColumnDesc(int which) {
		if (which == ref)
			return "Mean number of mismatching bases on reference reads";
		else
			return "Mean number of mismatching bases on non-reference reads";
	}

	@Override
	public double[] computeValue(final char refBase, FastaWindow window, AlignmentColumn col) {
		values[ref] = 0.0;
		values[alt] = 0.0;

		double refReads = 0;
		double altReads = 0;
		
		if (col.getDepth() > 0) {
			Iterator<MappedRead> it = col.getIterator();
			while(it.hasNext()) {
				MappedRead read = it.next();
				if (read.hasBaseAtReferencePos(col.getCurrentPosition())) {
					byte b = read.getBaseAtReferencePos(col.getCurrentPosition());
					if (b == 'N') 
						continue;
					
					int q = read.getMismatchCount(window);
					int index = 0;
					if ( b != refBase) {
						index = alt;
						altReads++;
					}
					else {
						refReads++;
					}
					
					values[index] += q;
										
				}
			}
		}
		if (refReads > 0)
			values[ref] /= refReads;
		
		if (altReads > 0)
			values[alt] /= altReads;
		
		
		values[alt] = values[alt] / 50.0 * 2.0 -1.0;
		values[ref] = values[ref] / 50.0 * 2.0 -1.0;
		return values;
	}

}
