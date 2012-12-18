package snpsvm.bamreading;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Map;

import snpsvm.bamreading.FastaIndex.IndexNotFoundException;

/**
 * A higher-performance version of a FastaReader, this uses FileChannels to memory map and seek 
 * to the correct positoin
 * @author brendan
 *
 */
public class FastaReader2 {

	public static final long BUFFER_SIZE = 128; //Size of buffer in bytes
	final static char CONTIG_START = '>';
	final File fastaFile;
	
	private ByteBuffer buffer = ByteBuffer.allocate( (int)BUFFER_SIZE);
	
	private String currentContig = null;
	private long currentContigByteStart = 0; //File position (byte offset) of of first base in current contig
	private long chrCharsRead = 0; //Number of characters read (including newlines) from the current contig
	private long chrBasesRead = 0; //Number of bases read in current contig, this is the 'currentPos'
	private long chrBufferOffset = 0; //Chromosomal offset of beginning of buffer
	
	//private Map<String, Long> contigMap = new HashMap<String, Long>();
	private final FileChannel chan;
	private final FastaIndex index;
	
	public FastaReader2(File fastaFile) throws IOException, IndexNotFoundException {
		this.fastaFile = fastaFile;
		index = new FastaIndex(fastaFile);
		
		FileInputStream fis = new FileInputStream(fastaFile);
		chan = fis.getChannel();
		
	}
	
	/**
	 * A reference to the (final) index of the fasta file
	 * @return
	 */
	public FastaIndex getIndex() {
		return index;
	}
	
	/**
	 * Returns sum of all contig sizes
	 * @return
	 */
	public long getExtent() {
		return getIndex().getExtent();
	}
	
	public IntervalList toIntervals() {
		IntervalList intervals = new IntervalList();
		
		for(String contig : index.getContigs()) {
			intervals.addInterval(contig, 1,(int)index.getContigLength(contig));
		}
		return intervals;
	}
	
	/**
	 * A reference to the file this fasta reader is reading
	 * @return
	 */
	public File getFile() {
		return fastaFile;
	}
	
	/**
	 * Close the 
	 */
	public void closeStream() {
		try {
			chan.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	/**
	 * Advance to the beginning of the given contig. The next base read will be the first base in the given contig
	 * @param newContig
	 * @throws IOException
	 */
	public void advanceToContig(String newContig) throws IOException {
		Long cPos = index.getContigByteOffset(newContig);
		if (cPos == null) {
			throw new IllegalArgumentException("Unknown contig : " + newContig);
		}
		buffer.clear();
		chan.read(buffer, cPos);
		currentContig = newContig;
		currentContigByteStart = cPos;
		chrBufferOffset = 0;
		chrCharsRead = 0;
		chrBasesRead = 0;
	}
	
	public String getCurrentContig() {
		return currentContig;
	}
	
	/**
	 * The total distance in bases from the beginning of the contig
	 * @return
	 */
	public long getCurrentPos() {
		return chrBasesRead; //Distance in bases from beginning of contig
	}
	
	/**
	 * Advance to given base position in current contig 
	 * @param pos
	 * @throws IOException 
	 * @throws EndOfContigException 
	 */
	public void advanceToPosition(int pos) throws IOException, EndOfContigException {
		//Expect one newline char every lineLength bytes read, so read actual byte offset will be pos + (pos/lineLength)
		int offset = pos/ index.getLineBaseCount(currentContig);
		long newStart = currentContigByteStart + pos + offset;
		
		//System.out.println("Advance from " + getCurrentPos() + " to " + pos + "  byte pos: " + newStart + " pos dif: " + (pos - getCurrentPos()) + " byte dif: "+ (newStart - chrCharsRead));
		buffer.clear();
		int read = chan.read(buffer, newStart);
		if (read == -1)
			throw new EndOfContigException();
		
		chrCharsRead = pos + offset;
		chrBasesRead = pos;
		chrBufferOffset = pos + offset;
	}
	
	/**
	 * Return current base and advance position by one
	 * @return
	 * @throws IOException 
	 * @throws EndOfContigException 
	 */
	public char nextBase() throws IOException, EndOfContigException {
		int buffDif = (int)(chrCharsRead - chrBufferOffset);
		if (buffDif == buffer.limit()) {
			bumpBuffer();
			buffDif = 0;
		}
		
		char c = (char) buffer.get(buffDif);
		while(c == '\n') {
			chrCharsRead++;
			buffDif = (int)(chrCharsRead - chrBufferOffset);
			if (buffDif == buffer.limit()) {
				bumpBuffer();
				buffDif =0 ;
			}
				
			c = (char) buffer.get(buffDif);
		}
		
		if (c==CONTIG_START) {
			throw new EndOfContigException();
		}
		
		chrCharsRead++;
		chrBasesRead++;
		return c;
	}
	
	/**
	 * Move the buffer ahead so that its new starting position is one beyond the previous 
	 * end position
	 * @throws IOException 
	 * @throws EndOfContigException 
	 */
	private void bumpBuffer() throws IOException, EndOfContigException {
		advanceToPosition((int)getCurrentPos());
	}
	
	/**
	 * Obtain map with all contigs and their sizes 
	 * @return
	 */
	public Map<String, Integer> getContigSizes() {
		Map<String, Integer> contigSizeMap = new HashMap<String, Integer>();
		for(String contig : index.getContigs()) {
			contigSizeMap.put(contig, (int) index.getContigLength(contig));
		}
		return contigSizeMap;
	}
	
	public static void main(String[] args) throws IOException, EndOfContigException, IndexNotFoundException {
		//FastaReader2 fa2 = new FastaReader2(new File("/home/brendan/workspace/SNPSVM/practicefasta.fasta"));
		FastaReader2 fa2 = new FastaReader2(new File("/Users/brendanofallon/resources/human_GRC37.fa"));

		for(int j=0; j<10; j++) {
			int pos = (int)(200000000*Math.random());
			StringBuffer test = new StringBuffer();
			fa2.advanceToContig("1");
			fa2.advanceToPosition(pos);
			for(int i=0; i<1024; i++) {
				char c = fa2.nextBase();
				test.append(c);
			}



			StringBuffer trueSeq = new StringBuffer();
			System.out.println();
			FastaReader fa = new FastaReader(new File("/Users/brendanofallon/resources/human_GRC37.fa"));
			fa.advanceToTrack("1");
			fa.advanceToPos(pos);
			for(int i=0; i<1024; i++) {
				char c= fa.nextPos();
				trueSeq.append(c);	
			}

			if (! test.toString().equals(trueSeq.toString())) {
				System.out.println("No match! Pos : " + pos);
				System.out.println(test);
				System.out.println(trueSeq);	
				for(int i=0; i<test.length(); i++) {
					if ( test.charAt(i) == trueSeq.charAt(i)) {
						System.out.print(" ");
					}
					else {
						System.out.print("*");
					}
				}
				System.out.println();
			}
			else {
				System.out.println("Perfect");
			}

		}
	}
	
	/**
	 * Thrown when end of contig reached
	 * @author brendan
	 *
	 */
	public class EndOfContigException extends Exception {
		
	}
	
	
	static class ContigPos {
		final String contig;
		final long absPos;
		
		public ContigPos(String name, long pos) {
			this.contig = name;
			this.absPos = pos;
		}
	}
}
