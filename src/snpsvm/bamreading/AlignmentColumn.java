package snpsvm.bamreading;

import java.io.File;
import java.util.Iterator;



/**
 * Works with a BAMWindow to provide access to the bases that align to a given site
 * @author brendan
 *
 */
public class AlignmentColumn {

	final int MAX_DEPTH = 256; //never consider more than this many reads
	private final int[] baseCounts = new int[4];
	public static final int A = 0;
	public static final int C = 1;
	public static final int G = 2;
	public static final int T = 3;
	
	final BamWindow bam;
	
	final byte[] bases = new byte[MAX_DEPTH];
	private int currentDepth = 0;
	private boolean dirty = true; //Flag is set when we advance to indicate that current info in bases[] is wrong
	
	public AlignmentColumn(BamWindow bamWindow) {
		bam = bamWindow;
	}
	
	public AlignmentColumn(File bamFile) {
		bam = new BamWindow(bamFile);
	}
	
	/**
	 * A reference to the BamWindow that stores bam information
	 * @return
	 */
	public BamWindow getBamWindow() {
		return bam;
	}
	
	/**
	 * Returns an upper bound on depth that does not reflect the fact that some reads may have insertions / deletions that prevent 
	 * exact mapping at the current site. The true depth may be less than this depth, but it will never be greater
	 * @return
	 */
	public int getApproxDepth() {
		return bam.size();
	}
	
	public String getCurrentContig() {
		return bam.getCurrentContig();
	}
	
	public byte[] getBases() {
		if (dirty) {
			calculateBases();
		}
		return bases;
	}
	
	public int getDepth() {
		if (dirty) {
			calculateBases();
		}
		return currentDepth;
	}
	
	public boolean hasDifferingBase(char c) {
		byte[] bases = getBases();
		for(int i=0; i<getDepth(); i++) {
			if (c != (char)bases[i])
				return true;
		}
		return false;
	}
	
	/**
	 * Counts the number of bases that differ from the given base at the current position
	 * @param c
	 * @return
	 */
	public int countDifferingBases(char c) {
		byte[] bases = getBases();
		int count = 0;
		for(int i=0; i<getDepth(); i++) {
			if (c != (char)bases[i])
				count++;
		}
		return count;
	}
        
        /**
	 * Counts the number of bases that differ from the given base at the current position
	 * @param c
	 * @return
	 */
	public boolean hasTwoDifferingBases(char refBase) {
		byte[] bases = getBases();
		int count = 0;
		for(int i=0; i<getDepth(); i++) {
			if (refBase != (char)bases[i] && ( (char)bases[i] != 'N'))
				count++;
			if (count > 1)
				return true;
		}
		return false;
	}
        
	/**
	 * Obtain an array with counts of each base (counts are indexed by the static fields A,C,G and T in this class)
	 * @return
	 */
     public int[] getBaseCounts() {
    	baseCounts[A] = 0;
    	baseCounts[C] = 0;
    	baseCounts[G] = 0;
    	baseCounts[T] = 0;
    	
 		for(int i=0; i<getDepth(); i++) {
 			switch( (char)bases[i]) {
 			case 'A' : baseCounts[A]++; break;
 			case 'C' : baseCounts[C]++; break;
 			case 'G' : baseCounts[G]++; break;
 			case 'T' : baseCounts[T]++; break;
 			}
 		}
 		return baseCounts;
     }
	
	/**
	 * Obtain a string representation of the bases at the current position
	 * @return
	 */
	public String getBasesAsString() {
		byte[] bases = getBases();
		StringBuilder str = new StringBuilder();
		for(int i=0; i<getDepth(); i++) {
			str.append(new Character((char) bases[i]));
		}
		return str.toString();
	}
	
	public int getCurrentPosition() {
		return bam.getCurrentPosition();
	}
	
	/**
	 * Iterates over MappedReads aligning to the current position
	 * @return
	 */
	public Iterator<MappedRead> getIterator() {
		return bam.getIterator();
	}
	
	public void advance() {
		bam.advanceBy(1);
		dirty = true;
	}
	
	public void advance(int bases) {
		bam.advanceBy(bases);
		dirty = true;
	}
	
	public void advanceTo(String contig, int pos) {
		bam.advanceTo(contig, pos);
		dirty = true;
	}
	
	public boolean hasMoreReadsInCurrentContig() {
		return bam.hasMoreReadsInCurrentContig();
	}
	
	private void calculateBases() {
		Iterator<MappedRead> it = bam.getIterator();

		currentDepth = 0;
        
		final int pos = getCurrentPosition();
		
		while(it.hasNext() && currentDepth < MAX_DEPTH) {
			MappedRead rec = it.next(); 
			int readPos = rec.refPosToReadPos(pos);
			if (readPos > -1) {
				bases[currentDepth] = rec.getBaseAtReadPos(readPos);
				currentDepth++;
			}
			
		}
		dirty = false;
	}

	public static String dataString(Double[] data) {
		StringBuilder strB = new StringBuilder();
		for(int i=0; i<data.length; i++) {
			strB.append(data[i] + "\t");
		}
		return strB.toString();
	}
	
}

