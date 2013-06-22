package snpsvm.counters;

import java.util.Iterator;

import snpsvm.bamreading.AlignmentColumn;
import snpsvm.bamreading.FastaWindow;
import snpsvm.bamreading.MappedRead;

/**
 * Just emits fraction of reads with variant base
 * @author brendan
 *
 */
public class VarFracCounter implements ColumnComputer {
	
	double[] value = new double[1];
	
	@Override
	public double[] computeValue(final char refBase, FastaWindow window, AlignmentColumn col) {
		double refCount = 0;
		double altCount = 0;
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
		
		
		
		double result = 0.0;
		if ( (refCount + altCount) > 0.0)
			result = altCount / (refCount + altCount);
		
		value[0] = result;
		value[0] = value[0]*2.0 - 1.0; //Scale to between -1 and 1
		return value;
	}
	

	@Override
	public String getName() {
		return "var.frac";
	}


	@Override
	public int getColumnCount() {
		return value.length;
	}


	@Override
	public String getColumnDesc(int which) {
		return "fraction of non-reference bases at site";
	}

}
