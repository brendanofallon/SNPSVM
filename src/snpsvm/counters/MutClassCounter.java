package snpsvm.counters;

import java.util.Iterator;

import snpsvm.bamreading.AlignmentColumn;
import snpsvm.bamreading.FastaWindow;
import snpsvm.bamreading.MappedRead;

public class MutClassCounter extends TsTvComputer {
	
	// private final double[] vals = new double[2];
	
	@Override
	public double[] computeValue(char refBase, FastaWindow window,
			AlignmentColumn col) {

		counts[0] = 0;
		counts[1] = 0;
		counts[2] = 0;
		counts[3] = 0;
		
		int totAlts = 0;
		double val = 0.0;
		
//		vals[0] = 0;
//		vals[1] = 0;
		values[0] = 0;
		
		if (col.getDepth() > 0) {
			Iterator<MappedRead> it = col.getIterator();
			while(it.hasNext()) {
				MappedRead read = it.next();
				if (read.hasBaseAtReferencePos(col.getCurrentPosition())) {
					byte b = read.getBaseAtReferencePos(col.getCurrentPosition());
					
					int index = -1;
					if (b != refBase && b != 'N') {
						switch(b) {
						case 'A' : index = 0;
						case 'C' : index = 1;
						case 'G' : index = 2;
						case 'T' : index = 3;
						}
						counts[index]++;
						totAlts++;
					}
					
				}
			}
			
			char alt = computeAlt(counts);
			
			if ((refBase == 'C' && alt == 'A') 
				|| (refBase == 'G' && alt=='T')) {
				val = 1.0;
			}
			else {
				val = -1.0;
			}
			
			
		}
		
		values[0] = val;
		
		return values;
	}

}
