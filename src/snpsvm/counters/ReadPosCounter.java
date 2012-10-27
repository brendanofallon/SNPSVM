package snpsvm.counters;

import java.util.Iterator;

import snpsvm.bamreading.AlignmentColumn;
import snpsvm.bamreading.FastaWindow;
import snpsvm.bamreading.MappedRead;

/**
 * Computes the average position in the read of reference and alt alleles
 * @author brendan
 *
 */
public class ReadPosCounter extends VarCountComputer {
	
	final double[] counts = new double[2];
	
	@Override
	public String getName() {
		return "read.pos";
	}

	@Override
	public int getColumnCount() {
		return values.length;
	}


	@Override
	public String getColumnDesc(int which) {
		if (which == ref)
			return "Mean read position of reference bases";
		else
			return "Mean read position of non-reference bases";
			
	}
	
	@Override
	public double[] computeValue(final char refBase, FastaWindow window, AlignmentColumn col) {
		values[ref] = 0.0;
		values[alt] = 0.0;
		counts[ref] = 0.0; 
		counts[alt] = 0.0;
		
		if (col.getDepth() > 0) {
			Iterator<MappedRead> it = col.getIterator();
			while(it.hasNext()) {
				MappedRead read = it.next();
				if (read.hasBaseAtReferencePos(col.getCurrentPosition())) {
					byte b = read.getBaseAtReferencePos(col.getCurrentPosition());
					int readPos = read.refPosToReadPos(col.getCurrentPosition());
					
					if (b == 'N') 
						continue;
					
					if (b == refBase) {
						values[ref]+=readPos;
						counts[ref]++;
					}
					else
						values[alt]+=readPos;
						counts[alt]++;
				}
			}
		}
		
		if (counts[ref] > 0)
			values[ref] /= counts[ref];
		if (counts[alt] > 0)
			values[alt] /= counts[alt];
		return values;
	}

}
