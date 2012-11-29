package snpsvm.counters;

import java.util.Iterator;

import snpsvm.bamreading.AlignmentColumn;
import snpsvm.bamreading.FastaWindow;
import snpsvm.bamreading.MappedRead;

/**
 * Fraction of reads with an indel that overlaps the given site
 * @author brendan
 *
 */
public class OverlappingIndelComputer implements ColumnComputer {

	double[] value = new double[1];
	
	@Override
	public double[] computeValue(final char refBase, FastaWindow window, AlignmentColumn col) {
		double totCounted = 0;
		double indels = 0;
		
		if (col.getDepth() > 0) {
			Iterator<MappedRead> it = col.getIterator();
			while(it.hasNext()) {
				MappedRead read = it.next();
				if (read.containsPosition(col.getCurrentPosition())) {
					totCounted++;
					if (! read.hasBaseAtReferencePos(col.getCurrentPosition())) {
						indels++;
					}
				}
				
			}
		}
		
		
				
		value[0] = indels / totCounted;
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
