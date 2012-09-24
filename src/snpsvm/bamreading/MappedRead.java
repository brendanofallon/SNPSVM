package snpsvm.bamreading;

import net.sf.samtools.Cigar;
import net.sf.samtools.CigarElement;
import net.sf.samtools.SAMRecord;

public class MappedRead {

	SAMRecord read;
	private boolean initialized = false;
	int[] refToReadMap = null; //Map from reference position to read position
	
	public MappedRead(SAMRecord rec) {
		this.read = rec;
	}
	
	public SAMRecord getRecord() {
		return read;
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
		if (dif >= read.getReadBases().length) {
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
	 * Read this record and compute how each base maps to the reference
	 */
	private void initialize() {
		Cigar cig = read.getCigar();
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
