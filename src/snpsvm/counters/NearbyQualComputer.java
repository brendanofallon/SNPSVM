package snpsvm.counters;

import java.util.Iterator;

import snpsvm.bamreading.AlignmentColumn;
import snpsvm.bamreading.FastaWindow;
import snpsvm.bamreading.MappedRead;

public class NearbyQualComputer implements ColumnComputer {

	public final int WINDOW_SIZE = 5; //Window spans the focus position, so 7 means three in either direction
	Double[] values = new Double[WINDOW_SIZE];
	public final double defaultVal = 50;
	
	@Override
	public String getName() {
		return "nearby.qualities";
	}

	@Override
	public Double[] computeValue(FastaWindow window, AlignmentColumn col) {
		for(int i=0; i<WINDOW_SIZE; i++) {
			values[i] = 0.0;
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
					}
					else {
						values[i] += defaultVal;
					}
				}
			}
		}
		
		return values;
	}

}
