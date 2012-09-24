package snpsvm.counters;

import java.util.Iterator;

import snpsvm.bamreading.AlignmentColumn;
import snpsvm.bamreading.MappedRead;

/**
 * Computes sum of mapping quality of reads with reference bases and non-reference bases
 * @author brendan
 *
 */
public class MQComputer extends VarCountComputer {

	final Double maxScore = 1000.0;
	
	@Override
	public String getName() {
		return "mapping.quality";
	}

	@Override
	public Double[] computeValue(char refBase, AlignmentColumn col) {
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
					
					int q = read.getRecord().getMappingQuality();
					int index = 0;
					if ( b != refBase)
						index = 1;
					values[index] += q;
					if (values[index] > maxScore)
						values[index] = maxScore;
					
					
				}
			}
		}
		
		return values;
	}

}
