package snpsvm.bamreading;

import java.io.File;
import java.util.Iterator;



/**
 * Works with a BAMWindow to provide access to the bases that align to a given site
 * @author brendan
 *
 */
public class AlignmentColumn {

	final int MAX_DEPTH = 1024; //never consider more than this many reads
	
	final BamWindow bam;
	
	final byte[] bases = new byte[MAX_DEPTH];
	private int currentDepth = 0;
	private boolean dirty = true; //Flag is set when we advance to indicate that current info in bases[] is wrong
	
	public AlignmentColumn(File bamFile) {
		bam = new BamWindow(bamFile);
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
		MappedRead rec = null;
		currentDepth = 0;
		while(it.hasNext() && currentDepth < MAX_DEPTH) {
			rec = it.next(); 
			
			if (rec.hasBaseAtReferencePos(getCurrentPosition())) {
				bases[currentDepth] = rec.getBaseAtReferencePos(getCurrentPosition());
				currentDepth++;
			}
			
		}
		
	}

	public static String dataString(Double[] data) {
		StringBuilder strB = new StringBuilder();
		for(int i=0; i<data.length; i++) {
			strB.append(data[i] + "\t");
		}
		return strB.toString();
	}
	
//	public static void main(String[] args) {
//		
//		File inBAM = new File( args[0] );
//		AlignmentColumn col = new AlignmentColumn(inBAM);
//		
//		col.advanceTo("1", 15813700);
//		
//		byte[] base = new byte[1];
//		
//		DepthComputer depth = new DepthComputer();
//		VarCountComputer varCount = new VarCountComputer();
//		QualSumComputer qSumComputer = new QualSumComputer();
//		PosDevComputer posDev = new PosDevComputer();
//		
//		while(col.getCurrentPosition()>0) {
//			byte[] bases = col.getBases();
//			if (col.getDepth() > 0) {
//				System.out.print(col.getCurrentPosition() + "\t:\t");
//				for(int i=0; i<col.getDepth(); i++) {
//					base[0] = bases[i];
//					System.out.print(new String( base));
//				}
//				
//				System.out.print( "\t" );
//				
//				String depthStr = dataString( depth.computeValue(col) );
//				//String valsStr = dataString( varCount.computeValue(col));
//				String qSumStr = dataString( qSumComputer.computeValue(col));
//				String posDevStr = dataString( posDev.computeValue(col));
//				
//				System.out.print( depthStr );
//				//System.out.print( valsStr );
//				System.out.print( qSumStr );
//				System.out.print( posDevStr );
//				
//				System.out.println();
//			}
//			col.advance();
//			
//		}
//		
//	}
	
}

