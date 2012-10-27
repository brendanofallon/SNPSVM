package snpsvm.counters;

import java.util.Iterator;

import snpsvm.bamreading.AlignmentColumn;
import snpsvm.bamreading.FastaWindow;
import snpsvm.bamreading.MappedRead;

/**
 * Computes mean of mapping quality of reads with reference bases and non-reference bases
 * @author brendan
 *
 */
public class MQComputer extends VarCountComputer {

	//final Double maxScore = 1000.0;
	final double[] counts = new double[2];
	
	@Override
	public String getName() {
		return "mapping.quality";
	}

	@Override
	public int getColumnCount() {
		return values.length;
	}


	@Override
	public String getColumnDesc(int which) {
		if (which == ref)
			return "Mean mapping quality of reads with reference base";
		else
			return "Mean mapping quality of reads with non-reference base";
	}
	
	@Override
	public double[] computeValue(final char refBase, FastaWindow window, AlignmentColumn col) {
		values[ref] = 0.0;
		values[alt] = 0.0;
		counts[0] = 0.0;
		counts[1] = 0.0;
		
		if (col.getDepth() > 0) {
			Iterator<MappedRead> it = col.getIterator();
			while(it.hasNext()) {
				MappedRead read = it.next();
				if (read.hasBaseAtReferencePos(col.getCurrentPosition())) {
					byte b = read.getBaseAtReferencePos(col.getCurrentPosition());
					if (b == 'N') 
						continue;
					
					int q = read.getRecord().getMappingQuality();
					int index = 0;
					if ( b != refBase)
						index = 1;
					values[index] += q;
					counts[index]++;
					
					
				}
			}
		}
		
		if (counts[0] > 0)
			values[0] /= counts[0];
		if (counts[1] > 0) 
			values[1] /= counts[1];
		
		return values;
	}

}
