package snpsvm.bamreading;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;


public class FastaReader {
	protected String currentLine = null;
	protected String currentTrack = null;
	protected int currentPos = -1;
	protected int lineOffset = 0;
	BufferedReader reader;
	final File sourceFile;
	
	private Map<String, Integer> contigSizes;
	
	public FastaReader(File file) throws IOException {
		this.sourceFile = file;
		buildContigMap(); //Only necessary if you want a listing of all contigs and their sizes
		initialize();
	}
	
	/**
	 * Returns a map of all contig names and their sizes
	 * @return
	 */
	public Map<String, Integer> getContigSizes() {
		return contigSizes;
	}
	
	/**
	 * Scan entire fasta file and build a map with all contig names and their lengths
	 * @throws IOException 
	 */
	private void buildContigMap() throws IOException {
		contigSizes = new HashMap<String, Integer>();

		//Search for .dict file in same directory as source file
		String dictFilename = sourceFile.getName().replace(".fasta", "").replace(".fa", "") + ".dict";
		File dictFile = new File(sourceFile.getParentFile() + System.getProperty("file.separator") + dictFilename);
		if (dictFile.exists()) {
			System.out.println("Found fasta dictionary in file: " + dictFile.getAbsolutePath());
			BufferedReader dictReader = new BufferedReader(new FileReader(dictFile));
			String line = dictReader.readLine();
			while(line != null) {
				if (line.startsWith("@SQ")) {
					String[] toks = line.split("\t");
					String name = toks[1].replace("SN:", "");
					try {
						Integer length = Integer.parseInt(toks[2].replace("LN:", ""));
						contigSizes.put(name, length);
					}
						catch(NumberFormatException nfe) {
							nfe.printStackTrace();
						}

					}
				line = dictReader.readLine();
			}
			return;
		}
		
		reader = new BufferedReader(new FileReader(sourceFile));
		currentLine = reader.readLine();
		
		System.err.print("Scanning contigs in fasta file...");
		System.err.flush();
		
		String contig = null;
		int contSize = 0;
		while(currentLine != null) {
			if (currentLine.startsWith(">")) {
				if (contig != null) {
					contigSizes.put(contig, contSize);
				}
				
				String chrStr = currentLine.trim().replace(">", "").replace("chr", "");
				int endPos = chrStr.indexOf(" ");
				if (endPos > 0) {
					chrStr = chrStr.substring(0, endPos);
				}
				contig = chrStr;
				contSize = 0;
			}
			else {
				contSize += currentLine.trim().length();
			}
			
			currentLine = reader.readLine();
		}
		
		System.err.println(".done, scanned " + contigSizes.size() + " contigs ");
	}
	
	
	
	private void initialize() throws IOException {
		reader = new BufferedReader(new FileReader(sourceFile));
		currentLine = reader.readLine();
		if (! currentLine.trim().startsWith(">")) {
			throw new IOException("First line doesn't start with >");
		}
		String chrStr = currentLine.trim().replace(">", "").replace("chr", "");
		int endPos = chrStr.indexOf(" ");
		if (endPos > 0) {
			chrStr = chrStr.substring(0, endPos);
		}
		
		currentTrack = chrStr;
		advanceLine();
	}
	
	public char getBaseAt(String track, int pos) throws IOException {
		if (pos <= 0) {
			throw new IllegalArgumentException("Remember, bases are ONE-INDEXED, so the first base is base #1, not 0, so please enter a pos > " + pos);
		}
		pos--;
		if (! track.equals(currentTrack))
			advanceToTrack(track);
		
		if (pos < currentPos) {
			throw new IllegalArgumentException("Can't go backwards, current pos is : " + currentPos + " but requested pos : " + pos);
		}
		
		advanceToPos(pos);
		return currentLine.charAt(lineOffset);
	}
	
	/**
	 * Get number of current track
	 * @return
	 */
	public String getCurrentTrack() {
		return currentTrack;
	}
	
	/**
	 * Get current position in ZERO-INDEXED coordinates
	 * @return
	 */
	public int getCurrentPos() {
		return currentPos;
	}
	
	/**
	 * Returns the base we're currently pointing at
	 * @return
	 */
	public char getCurrentBase() {
		return currentLine.charAt(lineOffset);
	}
	
	public void advanceToPos(int pos) throws IOException {
		int toAdvance = pos - currentPos; //Total number of bases to advance
		advance(toAdvance);
	}
	
	public void advance(int toAdvance) throws IOException {
		if (currentLine ==null) 
			throw new IllegalArgumentException("line is null, end of file probably reached");
		
		String currentTrack= getCurrentTrack();
		
		int toEndOfLine = currentLine.length() - lineOffset -1; //Number of bases to end of current line, 0 means lineOffset is pointing at last character in line
		
		while(toAdvance > toEndOfLine) {
			int advanced = advanceLine();
			if (currentLine ==null) {
				return;
			}
			if (! currentTrack.equals( getCurrentTrack()))
				throw new IllegalArgumentException("Advance went past end of track " + currentTrack);
			toEndOfLine = currentLine.length() - lineOffset-1; 
			toAdvance -= advanced;
		}
		
		//Presumably, the number of bases we need to advance is now less than the length of the current line
		//so we can just bump the pointer to the right spot
		lineOffset += toAdvance;
		currentPos += toAdvance;
	}
	
	/**
	 * Emit the base at the current position and advance the current position pointer by one unit 
	 * @return
	 * @throws IOException 
	 */
	public char nextPos() throws IOException {
		char c = currentLine.charAt(lineOffset);
		advance(1);
		return c;
	}
	

	
	/**
	 * Write all bases from the current position to the given position (staying on same chromosome)
	 * to the given PrintStream
	 * @param endPos
	 * @param out
	 * @throws IOException 
	 */
	public void emitBasesTo(int endPos, Writer out) throws IOException {
		String startTrack = getCurrentTrack();
		while (currentPos < endPos) {
			if (! getCurrentTrack().equals( startTrack)) {
				throw new IllegalArgumentException("Advanced past end of track " + startTrack);
			}
			out.write( nextPos() );
		}
	}

	/**
	 * Emit exactly the same bases to BOTH of the given writers. This is convenient if you're writing
	 * results for both chromosomes of a vcf file simultaneously
	 * @param endPos
	 * @param out1
	 * @param out2
	 * @throws IOException
	 */
	public void emitBasesTo(int endPos, Writer out1, Writer out2) throws IOException {
		String startTrack = getCurrentTrack();
		while (currentPos < endPos) {
			if (! getCurrentTrack().equals( startTrack)) {
				throw new IllegalArgumentException("Advanced past end of track " + startTrack);
			}
			//out.write((1+currentPos) + "\t" + nextPos() + "\n");
			char c = nextPos();
			//refOut.write( c );
			out1.write( c );
			out2.write( c );
		}
	}
	
	/**
	 * Emit all bases to until the end of this contig is reached 
	 * @param out
	 * @throws IOException
	 */
	public void emitBasesToContigEnd(Writer out) throws IOException {
		String startTrack = getCurrentTrack();
		while ( getCurrentTrack().equals( startTrack)) {
			out.write( nextPos() );
			if (currentLine == null)
				return;
		}
	}
	
	/**
	 * Read in one more line from the reader and put it (after trimming) in currentLine
	 * This also resets the lineOffset
	 * @throws IOException
	 */
	private int advanceLine() throws IOException {
		int advanced = currentLine.length() - lineOffset;
		currentPos += advanced;
		if (currentLine.startsWith(">")) {
			currentPos = 0;
		}
		currentLine = reader.readLine();
		if (currentLine != null)
			currentLine = currentLine.trim();
		lineOffset = 0;
		return advanced;
	}
	
	/**
	 * Advances the current position marker until it finds a contig name equal to the given track number
	 * @param track
	 * @throws IOException
	 */
	public void advanceToTrack(String track) throws IOException {
		
		while(currentLine != null && (! currentTrack.equals(track))) {
			while(currentLine != null && (! currentLine.startsWith(">"))) {
				advanceLine();
			}
			
			if (currentLine == null) {
				break;
			}
			
			String chrStr = currentLine.trim().replace(">", "").replace("chr", "");
			int endPos = chrStr.indexOf(" ");
			if (endPos > 0) {
				chrStr = chrStr.substring(0, endPos);
			}
			
		//	System.out.println("Searching for contig: "+ track + ".. looking at " + chrStr);
			
			if (chrStr.equals(track)) {
				currentPos=0;
				currentTrack = track;
				advanceLine();
				return;
			}	
			advanceLine();
		}
		
		
		
		if (currentLine == null) {
			initialize();
			advanceToTrack(track);
		}
		
		if (currentLine == null) {
			System.err.println("Oops, somehow missed contig " + track + " maybe contigs are not in order?");
			throw new IOException("Bad tracks");
		}
	}
	
	
}
