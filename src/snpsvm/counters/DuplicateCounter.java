package snpsvm.counters;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import net.sf.samtools.SAMRecord;

import snpsvm.bamreading.AlignmentColumn;
import snpsvm.bamreading.FastaWindow;
import snpsvm.bamreading.MappedRead;

/**
 * Counts the number of unique starting positions of reads for both
 * reads with and without the snp
 * @author brendanofallon
 *
 */
public class DuplicateCounter extends VarCountComputer {
	
	@Override
	public String getName() {
		return "dup.counter";
	}


	@Override
	public String getColumnDesc(int which) {
		if (which == ref)
			return "Number of reads with unique alignment start and end positions for reference reads";
		else
			return "Number of reads with unique alignment start and end positions for non-reference reads";
	}

	@Override
	public double[] computeValue(final char refBase, FastaWindow window, AlignmentColumn col) {
		values[ref] = 0.0;
		values[alt] = 0.0;
		Set<Pair> refPairs = new HashSet<Pair>();
		Set<Pair> altPairs = new HashSet<Pair>();
		if (col.getDepth() > 0) {
			Iterator<MappedRead> it = col.getIterator();
			while(it.hasNext()) {
				MappedRead read = it.next();
				if (read.hasBaseAtReferencePos(col.getCurrentPosition())) {
					byte b = read.getBaseAtReferencePos(col.getCurrentPosition());
					if (b == 'N' 
							|| (!read.getRecord().getProperPairFlag()) 
							|| read.getRecord().getMateUnmappedFlag()) 
						continue;
					
					int mateStart = read.getRecord().getMateAlignmentStart();
					int readStart = read.getRecord().getAlignmentStart();
					
					int first = Math.min(mateStart, readStart);
					int end = Math.max(mateStart, readStart);
					Pair p = new Pair(first, end);
					if (b == refBase) {
						refPairs.add(p); // will clobber old one if it exists
					}
					else {
						altPairs.add(p);
					}
				}
			}
		}
		
		int grainSize = 1000;
		values[ref] = Math.min(grainSize, refPairs.size());
		values[alt] = Math.min(grainSize, altPairs.size());
		
		values[ref] = values[ref] / (double)grainSize * 2.0 - 1.0;
		values[alt] = values[alt] / (double)grainSize * 2.0 - 1.0;
		
		return values;
	}

	private class Pair {
		int start;
		int end;
		
		public Pair(int start, int end) {
			this.start = start;
			this.end = end;
		}
		
		public boolean equals(Pair p) {
			return p.start == this.start && p.end == this.end;
		}
	}
	
}
