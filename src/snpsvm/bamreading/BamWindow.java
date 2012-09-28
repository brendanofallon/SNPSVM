package snpsvm.bamreading;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.NoSuchElementException;

import net.sf.samtools.SAMFileHeader;
import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMFileReader.ValidationStringency;
import net.sf.samtools.SAMRecord;
import net.sf.samtools.SAMRecordIterator;
import net.sf.samtools.SAMSequenceDictionary;
import net.sf.samtools.SAMSequenceRecord;

/**
 * Not really a window, a collections of SAMRecords that covers a particular spot
 * and which can be moved in one direction
 * @author brendan
 *
 */
public class BamWindow {

	public static final boolean DEBUG = true;
	
	final File bamFile;
	final SAMFileReader samReader; 
	private SAMRecordIterator recordIt; //Iterator for traversing over SAMRecords
	private SAMRecord nextRecord; //The next record to be added to the window, may be null if there are no more
	
	private String currentContig = null;
	private int currentPos = -1; //In reference coordinates
	final LinkedList<MappedRead> records = new LinkedList<MappedRead>();
	private Map<String, Integer> contigMap = null;
	private SAMSequenceDictionary sequenceDict = null;
	
	public BamWindow(File bamFile) {
		this.bamFile = bamFile;
		
		SAMFileReader.setDefaultValidationStringency(ValidationStringency.LENIENT);
		samReader = new SAMFileReader(bamFile);
		samReader.setValidationStringency(ValidationStringency.LENIENT);
		SAMFileHeader header = samReader.getFileHeader();
		sequenceDict = header.getSequenceDictionary();
		contigMap = new HashMap<String, Integer>();
		for(SAMSequenceRecord seqRec : sequenceDict.getSequences()) {
			contigMap.put(seqRec.getSequenceName(), seqRec.getSequenceLength());
		}
		
		
		recordIt = samReader.iterator();
		nextRecord = recordIt.next();
	}
	
	public int getCurrentPosition() {
		return currentPos;
	}
	
	public String getCurrentContig() {
		return currentContig;
	}
	/**
	 * Return total number of reads at the current position
	 * @return
	 */
	public int size() {
		//number of records on current position
		return records.size();
	}
	
	/**
	 * Return the mean inferred insertion size of all records in this window
	 * @return
	 */
	public double meanInsertSize() {
		double sum = 0;
		for(MappedRead rec : records) {
			sum += Math.abs(rec.getRecord().getInferredInsertSize());
		}
		return sum / (double)records.size();
	}
	
	/**
	 * Obtain an interator for the SAMRecords at the current position
	 * @return
	 */
	public Iterator<MappedRead> getIterator() {
		return records.iterator();
	}
	
	/**
	 * Advance the current position by the given number of bases
	 * @param bases
	 */
	public void advanceBy(int bases) {
		int newTarget = currentPos + bases;
		if (! hasMoreReadsInCurrentContig()) {
			if (DEBUG)
				System.out.println("No more reads in contig : " + currentContig);
			return;
		}
		
		advanceTo(currentContig, newTarget);
	}
	
	/**
	 * True if there is another read to be read in the current contigg
	 * @return
	 */
	public boolean hasMoreReadsInCurrentContig() {
		return nextRecord != null && nextRecord.getReferenceName().equals(currentContig);
	}
	
	/**
	 * A sanity check to ensure that all reads span the current position
	 */
	public void checkReads() {
		if (records.size()>0) {
			Iterator<MappedRead> it = getIterator();
			MappedRead read = it.next();
			while(read != null) {
				if (read.getRecord().getAlignmentStart() <= currentPos && read.getRecord().getAlignmentEnd() >= currentPos) {
					//cool
				}
				else {
					System.out.println("Read start: " + read.getRecord().getAlignmentStart() + " end:" + read.getRecord().getAlignmentEnd());
					throw new IllegalArgumentException("Uugh, read does not span current position of : " + currentPos);
				}
				try {
					read = it.next();
				}
				catch(NoSuchElementException ex) {
					read = null;
				}
			}	
		}
	}
	
	/**
	 * Advance to given contig if necessary, then advance to given position
	 * @param contig
	 * @param pos
	 */
	public void advanceTo(String contig, int pos) {
		//Advance to wholly new site
		//Expand leading edge until the next record is beyond target pos
		
		if (pos == 76982) {
			System.out.println("beak");
		}
		
		advanceToContig(contig);
		
		if (pos > contigMap.get(contig)) {
			throw new IllegalArgumentException("Contig " + contig + " has only " + contigMap.get(contig) + " bases, can't advance to " + pos);
		}
		
		
		if (nextRecord != null) {
			if (! nextRecord.getReferenceName().equals(currentContig)) {
				throw new IllegalArgumentException("Whoa! We're not searching the right contig, record contig is : " + contig +  " but current is : " + currentContig);
			}
		}
		
		//Must occur BEFORE we try to get new records...
		currentPos = pos;
		
		while(nextRecord != null 
				&& nextRecord.getAlignmentStart() <= pos
				&& nextRecord.getReferenceName().equals(currentContig)) {
			expand();
			shrinkTrailingEdge();
		}
		
		shrinkTrailingEdge();
	
		checkReads();
	}
	
	/**
	 * If given contig equals the currentContig, do nothing. Else, clear records in queue and set
	 * current position to zero, then search for given contig
	 * @param contig
	 */
	public void advanceToContig(String contig) {
		if (contig.equals(currentContig)) {
			return; //Already there
		}
		
		if (! contigMap.containsKey(contig)) {
			throw new IllegalArgumentException("Unrecognized contig name : "  + contig);
		}
		
		if (DEBUG)
			System.err.println("Advancing to contig : " + contig);
		
		recordIt.close();
		currentPos = 0;
		records.clear();
		
		int length = contigMap.get(contig);
		recordIt = samReader.queryOverlapping(contig, 1, length);
		
		//Going to a new contig, clear current queue
		
		nextRecord = recordIt.next();
		
		if (nextRecord != null)
			currentContig = contig;
		else {
			System.err.println("Could not find any reads that mapped to contig : " + contig);
		}
	}
	
	public int getLeadingEdgePos() {
		return records.getFirst().getRecord().getAlignmentEnd();
	}
	
	public int getTrailingEdgePos() {
		return records.getLast().getRecord().getAlignmentStart();
	}
	
	public MappedRead getTrailingRecord() {
		if (records.size()==0)
			return null;
		return records.getLast();
	}
	
	/**
	 * Push new records onto the queue. Unmapped reads and reads with unmapped mates are skipped.
	 */
	private void expand() {
		if (nextRecord == null)
			return;
		
		//System.out.println("Pushing record starting at : " + nextRecord.getAlignmentStart());
		records.push(new MappedRead(nextRecord));
		
		//Find next suitable record
		try {
			nextRecord = recordIt.next();
		}
		catch(NoSuchElementException ex) {
			nextRecord = null;
		}
		
		//Automagically skip unmapped reads and reads with unmapped mates
		while(nextRecord != null && (nextRecord.getMappingQuality()==0 || nextRecord.getMateUnmappedFlag())) {
			//System.out.println("Skipping record with mapping quality: " + nextRecord.getMappingQuality() + " mate mapping quality: " + nextRecord.getMateUnmappedFlag());
			nextRecord = recordIt.next();
		}
	}

	
	/**
	 * Remove from queue those reads whose right edge is less than the current pos
	 */
	private void shrinkTrailingEdge() {		
		Iterator<MappedRead> it = records.iterator();
		try {
			MappedRead read = it.next();
			while(it.hasNext()) {
				if (read.getRecord().getAlignmentEnd() < currentPos) {
					it.remove();
				}
				
				read = it.next();
			}
			
			if (read.getRecord().getAlignmentEnd() < currentPos) {
				it.remove();
			}
		}
		catch(NoSuchElementException ex) {
			//Expected behavior
		}
		

	}
	
}
