package snpsvm.bamreading;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Reads samtools (.faidx) - style fasta index files
 * @author brendanofallon
 *
 */
public class FastaIndex {

	private Map<String, ChrInfo> infoMap = new HashMap<String, ChrInfo>();
	
	public FastaIndex(File fastaFile) throws IndexNotFoundException, IOException {
		if (! fastaFile.exists()) {
			throw new FileNotFoundException();
		}
		
		File indexFile = findIndex(fastaFile);
		if (! indexFile.exists()) {
			throw new IndexNotFoundException();
		}
		if (! indexFile.canRead()) {
			System.err.println("Index file found but is not readable");
			throw new IndexNotFoundException();
		}
		
		BufferedReader reader = new BufferedReader(new FileReader(indexFile));
		String line = reader.readLine();
		ChrInfo info = parseInfo(line);
		if (info != null) {
			infoMap.put(info.contigName, info);
		}
		
		reader.close();
	}
	
	public long getContigLength(String contigName) {
		ChrInfo info = infoMap.get(contigName);
		return info.length;
	}
	
	public long getContigByteOffset(String contigName) {
		ChrInfo info = infoMap.get(contigName);
		return info.byteOffet;
	}
	
	public int getLineBaseCount(String contigName) {
		ChrInfo info = infoMap.get(contigName);
		return info.lineBaseCount;
	}
	
	public int getLineLength(String contigName) {
		ChrInfo info = infoMap.get(contigName);
		return info.lineLength;
	}
	
	private ChrInfo parseInfo(String line) {
		String[] toks = line.split("\t");
		if (toks.length != 5) {
			return null;	
		}
		
		ChrInfo info = new ChrInfo();
		info.contigName = toks[0];
		info.length = Integer.parseInt( toks[1] );
		info.byteOffet = Long.parseLong( toks[2] );
		info.lineBaseCount = Integer.parseInt(toks[3]);
		info.lineLength = Integer.parseInt(toks[4]);
		return info;
	}
	
	/**
	 * Attempt to find the index file associated with the given fasta file
	 * @param fastaFile
	 * @return
	 */
	private File findIndex(File fastaFile) {
		String presumedPath = fastaFile.getAbsolutePath() + ".faidx";
		File index = new File(presumedPath);
		return index;
	}

	/**
	 * Thrown when we can't find the index file 
	 * @author brendanofallon
	 *
	 */
	class IndexNotFoundException extends Exception {
		
	}

	class ChrInfo {
		String contigName; //Name of contig
		long length; //Number of bases in contig
		long byteOffet; //Byte offset of first base in contig
		int lineBaseCount; //Number of bases per line
		int lineLength; //Total number of chars per line, including newline characters
	}
}
