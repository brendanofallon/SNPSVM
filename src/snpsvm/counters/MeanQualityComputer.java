package snpsvm.counters;

import java.util.Iterator;

import snpsvm.bamreading.AlignmentColumn;
import snpsvm.bamreading.FastaWindow;
import snpsvm.bamreading.MappedRead;

public class MeanQualityComputer extends VarCountComputer {

	@Override
	public String getName() {
		return "mean.quality";
	}
	
	@Override
	public int getColumnCount() {
		return values.length;
	}


	@Override
	public String getColumnDesc(int which) {
		if (which == ref)
			return "Mean quality of reference bases";
		else
			return "Mean quality of non-reference bases";
	}
	
	@Override
	public double[] computeValue(final char refBase, FastaWindow window, AlignmentColumn col) {
		values[ref] = 0.0;
		values[alt] = 0.0;
		
		int refCount = 0;
		int altCount = 0;
		
		
		if (col.getDepth() > 0) {
			Iterator<MappedRead> it = col.getIterator();

			while(it.hasNext()) {
				MappedRead read = it.next();
				if (read.hasBaseAtReferencePos(col.getCurrentPosition())) {
					byte b = read.getBaseAtReferencePos(col.getCurrentPosition());
					byte q = read.getQualityAtReferencePos(col.getCurrentPosition());
					int index = 0;
					if (b == 'N') 
						continue;
					
					if (b != refBase) {
						index = 1;
						altCount++;
					}
					else {
						refCount++;
					}
					
					values[index] += q;
					
				}
			}
		}
		
		if (refCount > 0)
			values[ref] /= refCount;
		if (altCount > 0)
			values[alt] /= altCount;
		
		values[ref] = values[ref] / 60.0 * 2.0 -1.0;
		values[alt] = values[alt] / 60.0 * 2.0 -1.0;
		
		return values;
	}
}
