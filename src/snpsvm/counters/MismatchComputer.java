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
					char b = (char)read.getBaseAtReferencePos(col.getCurrentPosition());
					if (b == 'N' || refBase == 'N') 
						continue;
					
					int q = read.getMismatchCount(window);
					int index = ref;
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
			
		 
		if(values[ref] > 20.0) {
			//System.out.println("Crazy ref value: " + values[ref] + " at pos: " + col.getCurrentContig() + ":" + col.getCurrentPosition());
			values[ref] = 20.0;
		}
		if(values[alt] > 20.0) {
			//System.out.println("Crazy alt value: " + values[alt] + " at pos: " + col.getCurrentContig() + ":" + col.getCurrentPosition());
			values[alt] = 20.0;
		}
		
		values[alt] = values[alt]/20.0* 2.0 -1.0;		
		values[ref] = values[ref]/20.0* 2.0 -1.0;

		return values;
	}

}
