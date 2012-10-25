package snpsvm.bamreading;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
	private long currentContigPos = 0; //File position (byte offset) of of first base in current contig
	private long chrPos = 0; //Chromosomal position of current pointer
	private long chrBufferOffset = 0; //Chromosomal offset of beginning of buffer
	
	private Map<String, Long> contigMap = new HashMap<String, Long>();
	private final FileChannel chan;
	private final FastaIndex index;
	private int lineLength;
	
	public FastaReader2(File fastaFile) throws IOException, UnevenLineLengthException, IndexNotFoundException {
		this.fastaFile = fastaFile;
		
		index = new FastaIndex(fastaFile);
		lineLength = checkLineLength();
		System.out.println("Line length:" + lineLength);
		
		FileInputStream fis = new FileInputStream(fastaFile);
		chan = fis.getChannel();
		
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
		Long cPos = contigMap.get(newContig);
		if (cPos == null) {
			throw new IllegalArgumentException("Unknown contig : " + newContig);
		}
		buffer.clear();
		chan.read(buffer, cPos);
		currentContig = newContig;
		currentContigPos = cPos;
		chrBufferOffset = 0;
		chrPos = 0;
	}
	
	public String getCurrentContig() {
		return currentContig;
	}
	
	/**
	 * The total distance in bases from the beginning of the contig
	 * @return
	 */
	public long getCurrentPos() {
		return chrPos; //Distance in bases from beginning of contig
	}
	
	/**
	 * Advance to given base position in current contig 
	 * @param pos
	 * @throws IOException 
	 * @throws EndOfContigException 
	 */
	public void advanceToPosition(int pos) throws IOException, EndOfContigException {
		//Expect one newline char every lineLength bytes read, so read actual byte offset will be pos + (pos/lineLength)
		long newStart = currentContigPos + pos + pos/lineLength;
		System.out.println("Advance from " + currentContigPos + " to " + pos + ", offset: " + pos / lineLength);
		buffer.clear();
		int read = chan.read(buffer, newStart);
		if (read == -1)
			throw new EndOfContigException();
		
		chrPos = pos;
		chrBufferOffset = pos;
	}
	
	/**
	 * Return current base and advance position by one
	 * @return
	 * @throws IOException 
	 * @throws EndOfContigException 
	 */
	public char nextBase() throws IOException, EndOfContigException {
		int buffDif = (int)(chrPos - chrBufferOffset);
		if (buffDif == buffer.limit()) {
			bumpBuffer();
			buffDif = 0;
		}
		
		char c = (char) buffer.get(buffDif);
		while(c == '\n') {
			chrPos++;
			buffDif = (int)(chrPos - chrBufferOffset);
			if (buffDif == buffer.limit()) {
				bumpBuffer();
				buffDif =0 ;
			}
				
			c = (char) buffer.get(buffDif);
			
		}
		
		if (c==CONTIG_START) {
			throw new EndOfContigException();
		}
		
		chrPos++;
		return c;
	}
	
	/**
	 * Move the buffer ahead so that its new starting position is one beyond the previous 
	 * end position
	 * @throws IOException 
	 * @throws EndOfContigException 
	 */
	private void bumpBuffer() throws IOException, EndOfContigException {
		long newStart = currentContigPos + chrBufferOffset + buffer.limit();
		buffer.clear();
		int read = chan.read(buffer, newStart);
		if (read == -1)
			throw new EndOfContigException();
		System.out.println("Bumping buffer to pos: " + newStart);
		chrBufferOffset += read;
	}
	
	public static void main(String[] args) throws IOException, EndOfContigException, UnevenLineLengthException {
		//FastaReader2 fa2 = new FastaReader2(new File("/home/brendan/workspace/SNPSVM/practicefasta.fasta"));
		FastaReader2 fa2 = new FastaReader2(new File("/Users/brendanofallon/resources/testref.fa"));
		
		BufferedWriter writer = new BufferedWriter(new FileWriter("testfa2.txt"));
		fa2.advanceToContig("1");
		fa2.advanceToPosition(5050);
		for(int i=0; i<50; i++) {
			char c = fa2.nextBase();
			//System.out.print(c);
			writer.write(c);
			if (i>0 && i%60 == 0)
				writer.write('\n');
		}
		fa2.closeStream();
		writer.close();
		
		writer = new BufferedWriter(new FileWriter("testfa.txt"));
		System.out.println();
		FastaReader fa = new FastaReader(new File("/Users/brendanofallon/resources/testref.fa"));
		fa.advanceToTrack("1");
		fa.advanceToPos(5050);
		for(int i=0; i<50; i++) {
			char c= fa.nextPos();
			writer.write(c);
			if (i > 0 && i%60 == 0) 
				writer.write('\n');
		}
		writer.close();
		
//		System.out.println();
//		fa2.advanceToContig("17");
//		fa2.advanceToPosition(1400000);
//		for(int i=0; i<50000; i++) {
//			char c = fa2.nextBase();
//			System.out.print(c);
//		}
//		
//		System.out.println();
//		fa2.advanceToContig("1");
//		//fa2.advanceToPosition(14);
//		for(int i=0; i<5; i++) {
//			char c = fa2.nextBase();
//			System.out.print(c);
//		}
//		
//		System.out.println();
//		fa2.advanceToContig("20");
//		fa2.advanceToPosition(5);
//		for(int i=0; i<5; i++) {
//			char c = fa2.nextBase();
//			System.out.print(c);
//		}
		
	}
	
	/**
	 * Thrown when end of contig reached
	 * @author brendan
	 *
	 */
	class EndOfContigException extends Exception {
		
	}
	
	class UnevenLineLengthException extends Exception {
		
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
