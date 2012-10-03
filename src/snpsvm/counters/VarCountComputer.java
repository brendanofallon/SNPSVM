package snpsvm.counters;

import java.util.Iterator;

import snpsvm.bamreading.AlignmentColumn;
import snpsvm.bamreading.FastaWindow;
import snpsvm.bamreading.MappedRead;

/**
 * Counts the number of times each base appears
 * @author brendan
 *
 */
public class VarCountComputer implements ColumnComputer {

	final Double[] values = new Double[2];
	
	static final int ref = 0;
	static final int alt = 1;

	
	
	
	@Override
	public String getName() {
		return "var.counts";
	}

	@Override
	public Double[] computeValue(final char refBase, FastaWindow window, AlignmentColumn col) {
		values[ref] = 0.0;
		values[alt] = 0.0;
		if (col.getDepth() > 0) {
			Iterator<MappedRead> it = col.getIterator();
			while(it.hasNext()) {
				MappedRead read = it.next();
				if (read.hasBaseAtReferencePos(col.getCurrentPosition())) {
					byte b = read.getBaseAtReferencePos(col.getCurrentPosition());
					if (b == 'N') 
						continue;
					if (b == refBase)
						values[ref]++;
					else
						values[alt]++;
				}
			}
		}
		
		return values;
	}

}
