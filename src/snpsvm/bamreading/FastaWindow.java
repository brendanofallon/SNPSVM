package snpsvm.bamreading;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import util.ArrayCircularQueue;
import util.ArrayCircularQueue.EmptyQueueException;
import util.ArrayCircularQueue.FullQueueException;

public class FastaWindow {

	final int windowSize = 256;
	private int leftEdge = -1;
	private FastaReader reader;
	final ArrayCircularQueue bases = new ArrayCircularQueue(windowSize);
	
	public FastaWindow(File fastaFile) throws IOException {
		this(new FastaReader(fastaFile));
	}
	
	public FastaWindow(FastaReader reader) {
		this.reader = reader;
	}
	
	public Map<String, Integer> getContigSizes() {
		return reader.getContigSizes();
	}
	
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
	 * Aligns left edge at given reference position (1-based coord) on the given contig
	 * @param contig
	 * @param leftEdgePos
	 * @throws IOException
	 */
	public void resetTo(String contig, int leftEdgePos) throws IOException {
		bases.clear();
		reader.advanceToTrack(contig);
		leftEdgePos--;
		
		reader.advance(leftEdgePos);
		leftEdge = leftEdgePos+1;
		int contigSize = reader.getContigSizes().get(contig);
		
		for(int i=leftEdgePos; i<Math.min(leftEdgePos+windowSize, contigSize); i++) {
			Character b = reader.nextPos();
			try {
				bases.add(b);
			} catch (FullQueueException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				break;
			}
		}
	}
	
	/**
	 * Shift by one base to the right
	 */
	public void shift() {
		if (bases.size()>0) {
			try {
				bases.remove();
				leftEdge++;
			} catch (EmptyQueueException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			try {
				bases.add( reader.nextPos() );
			} catch (FullQueueException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}
	
	public void shift(int howmany) {
		for(int i=0; i<howmany; i++)
			shift();
	}
	
	/**
	 * Return base at given reference position. This uses 1-based coordinates!
	 * @param refPos
	 * @return
	 */
	public char getBaseAt(int refPos) {
		if (refPos <= leftEdge)
			throw new IllegalArgumentException("Can't access position before left edge: position: " + refPos + " but left edge is: " + leftEdge);
		if (refPos > leftEdge+bases.size()) {
			throw new IllegalArgumentException("Can't access position after right edge: position: " + refPos + " but right edge is: " + (leftEdge+bases.size()));
		}
		int index = refPos - leftEdge-1;
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
	
	public static void main(String[] args) throws IOException {
		FastaWindow fw = new FastaWindow(new File("/home/brendan/resources/human_g1k_v37.fasta"));
		
		fw.resetTo("21", 1000000);
		System.out.println( fw.allToString() );
		
		int count = 0;
		while(true) {
			fw.shift(100);
			System.out.println( fw.allToString() );
			count++;
		}
	}
}
