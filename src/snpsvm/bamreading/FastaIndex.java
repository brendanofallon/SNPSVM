package snpsvm.bamreading;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Reads samtools (.fai) - style fasta index files
 * @author brendanofallon
 *
 */
public class FastaIndex {

	private Map<String, ChrInfo> infoMap = new HashMap<String, ChrInfo>();
	
	public FastaIndex(File fastaFile) throws IndexNotFoundException, IOException {
		if (! fastaFile.exists()) {
			System.out.println("Could not find fasta file : " + fastaFile.getAbsolutePath());
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
		while(line != null) {
			ChrInfo info = parseInfo(line);
			if (info != null) {
				infoMap.put(info.contigName, info);
			}
			line = reader.readLine();
		}
		reader.close();
	}
	
	public long getContigLength(String contigName) {
		ChrInfo info = infoMap.get(contigName);
		return info.length;
	}
	
	/**
	 * Returns sum of all contig sizes
	 * @return
	 */
	public long getExtent() {
		long sum = 0;
		for (ChrInfo info : infoMap.values()) {
			sum += info.length;
		}
		return sum;
	}
	
	public long getContigByteOffset(String contigName) {
		ChrInfo info = infoMap.get(contigName);
		if (info == null) {
			System.out.println("Contig names are :");
			for(String contig : getContigs()) {
				System.out.println(contig);
			}
			throw new IllegalArgumentException("No contig with name: "+ contigName);
		}
		return info.byteOffset;
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
		info.contigName = toks[0].split(" ")[0];
		info.length = Integer.parseInt( toks[1] );
		info.byteOffset = Long.parseLong( toks[2] );
		info.lineBaseCount = Integer.parseInt(toks[3]);
		info.lineLength = Integer.parseInt(toks[4]);
		return info;
	}
	
	/**
	 * Return all contig names in index
	 * @return
	 */
	public Collection<String> getContigs() {
		return infoMap.keySet();
	}
	
	/**
	 * Attempt to find the index file associated with the given fasta file
	 * @param fastaFile
	 * @return
	 */
	private File findIndex(File fastaFile) {
		String presumedPath = fastaFile.getAbsolutePath() + ".fai";
		File index = new File(presumedPath);
		return index;
	}

	/**
	 * Thrown when we can't find the index file 
	 * @author brendanofallon
	 *
	 */
	public class IndexNotFoundException extends Exception {
		
	}

	class ChrInfo {
		String contigName; //Name of contig
		long length; //Number of bases in contig
		long byteOffset; //Byte offset of first base in contig
		int lineBaseCount; //Number of bases per line
		int lineLength; //Total number of chars per line, including newline characters
	}
}
