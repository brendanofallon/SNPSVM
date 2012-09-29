package snpsvm.bamreading;

import net.sf.samtools.Cigar;
import net.sf.samtools.CigarElement;
import net.sf.samtools.SAMRecord;

public class MappedRead {

	SAMRecord read;
	private boolean initialized = false;
	int[] refToReadMap = null; //Map from reference position to read position
	private int mismatchCount = -1; //Number of bases that align to reference but differ from it 
	
	
	public MappedRead(SAMRecord rec) {
		this.read = rec;
	}
	
	public SAMRecord getRecord() {
		return read;
	}
	
	public int getMismatchCount(FastaWindow ref) {
		if (mismatchCount == -1) {
			mismatchCount = 0;

			for(int i=read.getAlignmentStart(); i<read.getAlignmentEnd(); i++) {
				if (hasBaseAtReferencePos(i)) {		
					if ( ((char)getBaseAtReferencePos(i)) != ref.getBaseAt(i+1)) {
						mismatchCount++;
					}
				}
			}
		}
		
		return mismatchCount;
	}
	
	/**
	 * Returns true if there's exactly one base in this read that maps to 
	 * the given reference position
	 * @param refPos
	 * @return
	 */
	public boolean hasBaseAtReferencePos(int refPos) {
		int readPos = refPosToReadPos(refPos); 
		return  readPos > -1;
	}
	
	/**
	 * Return index of base in this read that maps to the given reference position
	 * @param refPos
	 * @return
	 */
	public int refPosToReadPos(int refPos) {
		int dif = refPos - read.getAlignmentStart();
		if (dif < 0 || dif >= read.getReadBases().length) {
			//System.out.println("Hmm, asked for dif : " + dif + ", returning -1 instead");
			return -1;
		}
		
		if (refToReadMap == null)
			return dif;
		else {
			return refToReadMap[dif];
		}
	}
	
	public byte getBaseAtReferencePos(int refPos) {
		if (!initialized)
			initialize();
		
		int pos = refPosToReadPos(refPos);
		if (pos < 0 || pos >= read.getReadLength()) {
			System.out.println("Uugh, no read base at ref pos: "+ refPos + ", which maps to read position: " + pos + " CIGAR : " + read.getCigarString());
			//int pos2 = refPosToReadPos(refPos);
			return -1;
		}
		return read.getReadBases()[ pos ];
	}
	
	public byte getQualityAtReferencePos(int refPos) {
		if (!initialized)
			initialize();
		
		return read.getBaseQualities()[ refPosToReadPos(refPos) ];
	}
	
	/**
	 * If this read has any indels then construct a map that relates reference position to read position so
	 * we can quickly look up the read (or quality) that maps to a particular reference pos. If there are no
	 * indels then abort immediately. 
	 */
	private void initialize() {
		Cigar cig = read.getCigar();
		if (cig.getCigarElements().size()==0) {
			System.err.println("No cigar elements for read: " + read.toString() + ", skipping, mq is : " + read.getMappingQuality() );
			initialized = true;
			return;
		}
		CigarElement firstEl = cig.getCigarElement(0);
		
		//If no indels don't do anything
		if (firstEl.getLength() == cig.getReferenceLength()) {
			initialized = true;
			return;
		}
		
		boolean consumingRead = true;
		boolean consumingReference = true;
		
		//56 is maximum deletion length we tolerate. remember we have -1's for every base on the read that missing in the reference,
		//so we need extra space in the map for all those -1's 
		refToReadMap = new int[ cig.getReferenceLength()+101 ];
		int refPos = 0;
		int readPos = 0;
		for(CigarElement el : cig.getCigarElements()) {
			consumingRead = el.getOperator().consumesReadBases();
			consumingReference = el.getOperator().consumesReferenceBases();
			
			for(int j=0; j<el.getLength(); j++) {
				int index = -1;
				
				if (consumingRead) {
					index = readPos;
					readPos++;
				}
				
				if (consumingReference) {
				
					refToReadMap[refPos] = index;
					refPos++;
					
					if (index >= 101) {
						System.out.println("Hmm, we seem to be adding read positions past 101.." + index);
					}
				}
				
				
				
			}
		}
		
		initialized = true;
	}
	
}
