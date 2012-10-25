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
	private long currentContigByteStart = 0; //File position (byte offset) of of first base in current contig
	private long chrPos = 0; //Chromosomal position of current pointer
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
		
		double frac = (double)pos / (double)index.getLineLength(currentContig);
		long newStart = currentContigByteStart + pos + pos/ index.getLineBaseCount(currentContig);
		System.out.println("Advance from " + currentContigByteStart + " to " + pos + ", frac : " + frac + "  offset: " + pos / index.getLineLength(currentContig));
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
		long newStart = currentContigByteStart + chrBufferOffset + buffer.limit();
		buffer.clear();
		int read = chan.read(buffer, newStart);
		if (read == -1)
			throw new EndOfContigException();
		System.out.println("Bumping buffer to pos: " + newStart);
		chrBufferOffset += read;
	}
	
	public static void main(String[] args) throws IOException, EndOfContigException, IndexNotFoundException {
		//FastaReader2 fa2 = new FastaReader2(new File("/home/brendan/workspace/SNPSVM/practicefasta.fasta"));
		FastaReader2 fa2 = new FastaReader2(new File("/Users/brendanofallon/resources/testref.fa"));
		
		BufferedWriter writer = new BufferedWriter(new FileWriter("testfa2.txt"));
		int pos = 5001;
		StringBuffer test = new StringBuffer();
		fa2.advanceToContig("1");
		fa2.advanceToPosition(pos);
		for(int i=0; i<50; i++) {
			char c = fa2.nextBase();
			//System.out.print(c);
			writer.write(c);
			test.append(c);
			if (i>0 && i%60 == 0)
				writer.write('\n');
		}
		fa2.closeStream();
		writer.close();
		
		
		StringBuffer trueSeq = new StringBuffer();
		writer = new BufferedWriter(new FileWriter("testfa.txt"));
		System.out.println();
		FastaReader fa = new FastaReader(new File("/Users/brendanofallon/resources/testref.fa"));
		fa.advanceToTrack("1");
		fa.advanceToPos(pos);
		for(int i=0; i<50; i++) {
			char c= fa.nextPos();
			trueSeq.append(c);
			writer.write(c);
			if (i > 0 && i%60 == 0) 
				writer.write('\n');
		}
		writer.close();
		
		
		System.out.println(test);
		System.out.println(trueSeq);
		
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
	
	
	static class ContigPos {
		final String contig;
		final long absPos;
		
		public ContigPos(String name, long pos) {
			this.contig = name;
			this.absPos = pos;
		}
	}
}
