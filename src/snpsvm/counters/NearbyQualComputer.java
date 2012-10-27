package snpsvm.counters;

import java.util.Iterator;

import snpsvm.bamreading.AlignmentColumn;
import snpsvm.bamreading.FastaWindow;
import snpsvm.bamreading.MappedRead;

public class NearbyQualComputer implements ColumnComputer {

	public final int WINDOW_SIZE = 5; //Window spans the focus position, so 7 means three in either direction
	double[] values = new double[WINDOW_SIZE];
	double[] counts = new double[WINDOW_SIZE];
	public final double defaultVal = 20;
	
	@Override
	public String getName() {
		return "nearby.qualities";
	}

	@Override
	public int getColumnCount() {
		return values.length;
	}


	@Override
	public String getColumnDesc(int which) {
		return "Mean quality of sites aligning nearby";
	}
	
	@Override
	public double[] computeValue(final char refBase, FastaWindow window, AlignmentColumn col) {
		for(int i=0; i<WINDOW_SIZE; i++) {
			values[i] = 0.0;
			counts[i] = 0.0;
		}
		
		int offset = WINDOW_SIZE/2;
		if (col.getDepth() > 0) {
			Iterator<MappedRead> it = col.getIterator();
			while(it.hasNext()) {
				MappedRead read = it.next();
				
				for(int i=0; i<WINDOW_SIZE; i++) {
					int refPos = col.getCurrentPosition()-offset+i;
					if (read.hasBaseAtReferencePos(refPos)) {
						byte q = read.getQualityAtReferencePos(refPos);
						values[i] += q;
						counts[i]++;
					}
					else {
						values[i] += defaultVal;
						counts[i]++;
					}
				}
			}
		}
		
		for(int i=0; i<WINDOW_SIZE; i++) {
			if (counts[i] > 0)
				values[i] /= counts[i];
		}
		
		return values;
	}

}
