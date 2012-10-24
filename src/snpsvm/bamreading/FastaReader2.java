package snpsvm.bamreading;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A higher-performance version of a FastaReader, this uses FileChannels to memory map and seek 
 * to the correct positoin
 * @author brendan
 *
 */
public class FastaReader2 {

	public static final long BUFFER_SIZE = 1024; //SIze of buffer in bytes
	final static char CONTIG_START = '>';
	final File fastaFile;
	private String currentContig = null;
	private long currentContigPos = 0;
	
	private ByteBuffer buffer = ByteBuffer.allocate( (int)BUFFER_SIZE);
	private long chrPos = 0; //Chromosomal position of current pointer
	private long chrBufferOffset = 0; //Chromosomal offset of beginning of buffer
	
	private Map<String, Long> contigMap = new HashMap<String, Long>();
	private final FileChannel chan;
	
	private int lineLength;
	
	public FastaReader2(File fastaFile) throws IOException, UnevenLineLengthException {
		this.fastaFile = fastaFile;
		
		lineLength = checkLineLength();
		
		
		FileInputStream fis = new FileInputStream(fastaFile);
		chan = fis.getChannel();
		
		//build a contig map
		int pos = 0; //Number of chars starting with char after first newline after most recent contig
		long absPos = 0; //Total number of chars (bytes) since beginning of file
		
		long size = chan.size();
		while (absPos < size) {
			int actuallyRead = chan.read(buffer);
			if (actuallyRead < 1) {
				break;
			}
			List<ContigPos> contigs = findContigs();
			if (contigs != null) {
				for(ContigPos contig : contigs) {
					contigMap.put(contig.contig, absPos + contig.absPos);
					System.out.println("Putting contig " + contig.contig + " - " + (absPos + contig.absPos));
				}
			}
			buffer.clear();
			absPos += actuallyRead;
		}		
	}
	
	private int checkLineLength() throws IOException, UnevenLineLengthException {
		BufferedReader reader = new BufferedReader(new FileReader(fastaFile));
		String line = reader.readLine();
		while(line.startsWith(">") && line != null) {
			line = reader.readLine();
		}
		
		int count = 0;
		int ll = line.length();
		while(count < 1000 && line != null) {
			int length = line.length();
			if (length != ll) {
				line = reader.readLine();
				if (line != null && !(line.trim().length()==0 || (line.startsWith(">"))))
					throw new UnevenLineLengthException(); 
			}
			count++;
			line = reader.readLine();
		}
		
		reader.close();
		
		return ll+1; //readline doesn't include \n, so it must be added
	}
	
	/**
	 * Scan the given byte buffer and read in contig names and the starting position of the sequence data after the name
	 * @param buf
	 * @param size
	 * @return
	 * @throws IOException 
	 */
	private List<ContigPos> findContigs() throws IOException {
		List<ContigPos> poses = null;
		for(int i=0; i<BUFFER_SIZE; i++) {
			char c = (char) buffer.get(i);
			if (c == CONTIG_START) {
				//Get contig name
				String contigName = readContigName(i+1);
				
				//position should be first character after next newline
				int j = i + contigName.length();
				while(j < buffer.limit() && (char)buffer.get(j) != '\n') {
					j++;
				}
			
				if (poses == null) {
					poses = new ArrayList<ContigPos>();
				}
				poses.add(new ContigPos(contigName, j+1));
				i = j+1;
			}
		}
		return poses;
	
	}
	
	private String readContigName(int startPos) throws IOException {
		StringBuffer str = new StringBuffer();
		int index = startPos;
		
		char c = (char) buffer.get(index);
		while(c != ' ' && c != '\n' ) {
			str.append(c);
			index++;
			
			if (index == buffer.limit()) {
				//Buffer boundary falls in the middle of contig name... read more bytes into buffer
				throw new IllegalStateException("Buffer boundary falls in middle of contig name... name is : " +str);
			}
			c = (char) buffer.get(index);
		}
		
	
		return str.toString();
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
		return currentContigPos; //Distance in bases from beginning of contig
	}
	
	/**
	 * Advance to given base in current contig 
	 * @param pos
	 * @throws IOException 
	 * @throws EndOfContigException 
	 */
	public void advanceToPosition(int pos) throws IOException, EndOfContigException {
		//Expect one newline char every lineLength bytes read, so read actual byte offset will be pos + (pos/lineLength)
		long newStart = currentContigPos + pos + pos/lineLength;
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
		//System.out.println("After read, bytes read: " + read + " buffer pos: " + buffer.position() + " limit: " + buffer.limit());
		chrBufferOffset += read;
	}
	
	public static void main(String[] args) throws IOException, EndOfContigException, UnevenLineLengthException {
		FastaReader2 fa2 = new FastaReader2(new File("/home/brendan/workspace/SNPSVM/practicefasta.fasta"));
		//FastaReader2 fa2 = new FastaReader2(new File("/home/brendan/resources/human_g1k_v37.fasta"));
		fa2.advanceToContig("one");
		fa2.advanceToPosition(14);
		for(int i=0; i<30; i++) {
			char c = fa2.nextBase();
			System.out.print(c);
		}
		
		System.out.println();
		fa2.advanceToContig("two");
		fa2.advanceToPosition(14);
		for(int i=0; i<5; i++) {
			char c = fa2.nextBase();
			System.out.print(c);
		}
		
		System.out.println();
		fa2.advanceToContig("one");
		//fa2.advanceToPosition(14);
		for(int i=0; i<5; i++) {
			char c = fa2.nextBase();
			System.out.print(c);
		}
		
		System.out.println();
		fa2.advanceToContig("three");
		fa2.advanceToPosition(5);
		for(int i=0; i<5; i++) {
			char c = fa2.nextBase();
			System.out.print(c);
		}
		
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
