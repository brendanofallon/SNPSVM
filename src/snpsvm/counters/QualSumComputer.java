package snpsvm.counters;

import java.util.Iterator;

import snpsvm.bamreading.AlignmentColumn;
import snpsvm.bamreading.FastaWindow;
import snpsvm.bamreading.MappedRead;

public class QualSumComputer extends VarCountComputer {
	
	final double maxScore = 500.0; //Maximum value for any base
	
	@Override
	public String getName() {
		return "quality.sums";
	}
	
	@Override
	public int getColumnCount() {
		return values.length;
	}


	@Override
	public String getColumnDesc(int which) {
		if (which == ref)
			return "Sum of quality scores of reference bases";
		else
			return "Sum of quality scores of non-reference bases";
			
	}
	
	@Override
	public double[] computeValue(final char refBase, FastaWindow window, AlignmentColumn col) {
		values[ref] = 0.0;
		values[alt] = 0.0;
		
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
					if (b != refBase)
						index = 1;
					values[index] += q;
					if (values[index] > maxScore)
						values[index] = maxScore;
					
					
				}
			}
		}
		
		values[ref] = Math.min(1024, values[ref]);
		values[alt] = Math.min(1024, values[alt]);
		values[ref] = values[ref] / 1024.0 * 2.0 -1.0;
		values[alt] = values[alt] / 1024.0 * 2.0 -1.0;
		
		return values;
	}

}
