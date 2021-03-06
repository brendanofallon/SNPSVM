package snpsvm.bamreading;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import snpsvm.bamreading.FastaIndex.IndexNotFoundException;
import snpsvm.bamreading.FastaReader2.EndOfContigException;
import util.ArrayCircularQueue;
import util.ArrayCircularQueue.EmptyQueueException;
import util.ArrayCircularQueue.FullQueueException;

public class FastaWindow {

	final int windowSize = 256;
	private int leftEdge = -1;
	private FastaReader2 reader;
	final ArrayCircularQueue bases = new ArrayCircularQueue(windowSize);
	
	public FastaWindow(File fastaFile) throws IOException, IndexNotFoundException {
		this(new FastaReader2(fastaFile));
	}
	
	public FastaWindow(FastaReader2 reader) {
		this.reader = reader;
	}
	
//	public Map<String, Integer> getContigSizes() {
//		return reader.getContigSizes();
//	}
	
	/**
	 * Reference index of left (trailing) edge
	 * @return
	 */
	public int indexOfLeftEdge() {
		return leftEdge;
	}
	
	/**
	 * Reference index of right (leading) edge
	 * @return
	 */
	public int indexOfRightEdge() {
		return leftEdge+bases.size();
	}
	
	/**
	 * Current number of bases in window
	 * @return
	 */
	public int getCurrentSize() {
		return bases.size();
	}
	
	public int getMaxSize() {
		return windowSize;
	}
	
	/**
	 * A collection with all of the contig names, taken straight from the index
	 * @return
	 */
	public Collection<String> getContigs() {
		return reader.getIndex().getContigs();
	}
	
	/**
	 * The length of the requested contig
	 * @param contig
	 * @return
	 */
	public Long getContigLength(String contig) {
		return reader.getContigLength(contig);
	}
	
	/**
	 * Aligns left edge at given reference position (1-based coord) on the given contig
	 * @param contig
	 * @param leftEdgePos
	 * @throws IOException
	 * @throws EndOfContigException 
	 * @throws FullQueueException 
	 */
	public void resetTo(String contig, int leftEdgePos) throws IOException, EndOfContigException, FullQueueException {
		if (reader.getCurrentContig() == null) {
			reader.advanceToContig(contig);	
		}
		
		if ((! reader.getCurrentContig().equals(contig))) {
			bases.clear();
			this.leftEdge = 1;
			reader.advanceToContig(contig);			
		}
		else {
			if (leftEdgePos < indexOfRightEdge()) {
				//System.out.println(" Shifting from " + leftEdge + "-" + indexOfRightEdge() + " to " + leftEdgePos + " without filling");
				//Advance by amount less than current window size, so just shift and don't clear any bases
				while(leftEdge < leftEdgePos) {
					shift();
				}
				return;
			}

		}

		//System.out.println(" Skipping from " + leftEdge + "-" + indexOfRightEdge() + " to " + leftEdgePos + " and re-filling");
		bases.clear();
		//Left edge may be at default value of -1
		if (leftEdge < 0)
			leftEdge = 1;

		reader.advanceToPosition(leftEdgePos-1); //reader actually keeps things 0-indexed, so subtract 1
		leftEdge = leftEdgePos;
		int contigSize = (int) (reader.getContigLength(contig).intValue());
//		System.out.println(">" + leftEdgePos);
		for(int i=leftEdgePos; i<Math.min(leftEdgePos+windowSize, contigSize); i++) {
			char c = reader.nextBase();
			bases.add( c );
			//System.out.print(c);
		}
	//	System.out.println();
		
	}
	
	/**
	 * Shift by one base to the right
	 * @throws EndOfContigException 
	 */
	public void shift() throws EndOfContigException {
		if (bases.size()>0) {
			try {
				bases.remove();
				leftEdge++;
			} catch (EmptyQueueException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			try {
				bases.add( reader.nextBase() );
			} catch (FullQueueException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				
				e.printStackTrace();
			} 
		}
		
	}
	
	public void shift(int howmany) throws EndOfContigException {
		for(int i=0; i<howmany; i++)
			shift();
	}
	
	/**
	 * Return base at given reference position. This uses 1-based coordinates!
	 * @param refPos
	 * @return
	 */
	public char getBaseAt(int refPos) {
		if (refPos < leftEdge)
			throw new IllegalArgumentException("Can't access position before left edge: position: " + refPos + " but left edge is: " + leftEdge);
		if (refPos > leftEdge+bases.size()) {
			throw new IllegalArgumentException("Can't access position after right edge: position: " + refPos + " but right edge is: " + (leftEdge+bases.size()));
		}
		int index = refPos - leftEdge;
		return bases.get(index);
	}
	
	public String allToString() {
		StringBuilder strB  = new StringBuilder();
		strB.append(leftEdge + " : " );
		for(int i=0; i<bases.size(); i++) {
			strB.append( bases.get(i));
		}
		return strB.toString();
	}

	public boolean containsContig(String contig) {
		return reader.containsContig(contig);
	}
	
}
