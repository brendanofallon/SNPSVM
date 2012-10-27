package snpsvm.bamreading;

import net.sf.samtools.Cigar;
import net.sf.samtools.CigarElement;
import net.sf.samtools.SAMRecord;

public class MappedRead {

    static long count = 0;
        public static final int[] defaultMap = new int[1024];
        private static boolean mapInitialized = false;
        
	SAMRecord read;
	private boolean initialized = false;
        
	int[] refToReadMap = defaultMap; //Map from reference position to read position
	private int mismatchCount = -1; //Number of bases that align to reference but differ from it 
	
	
	final int readBasesCount;	//instant storage for number of bases in read 
	final int readAlignmentStart; //instant storage for start of alignment of read
	final byte[] readBases;		//instant storage for actual bases in read

	public MappedRead(SAMRecord rec) {
		if (! mapInitialized) {
			for(int i=0; i<defaultMap.length; i++) {
				defaultMap[i] = i;
			}
			mapInitialized = true;
		}

		this.read = rec;
		readBasesCount = read.getReadLength();
		readAlignmentStart = read.getAlignmentStart();
		readBases = read.getReadBases();
	}
	
	public SAMRecord getRecord() {
		return read;
	}
	
	public int getMismatchCount(FastaWindow ref) {
		if (mismatchCount == -1) {
			mismatchCount = 0;

			for(int i=Math.max(read.getAlignmentStart(), ref.indexOfLeftEdge()+1); i<Math.min(ref.indexOfRightEdge(), read.getAlignmentEnd()); i++) {
				if (hasBaseAtReferencePos(i)) {		
					if ( ((char)getBaseAtReferencePos(i)) != ref.getBaseAt(i)) {
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
		return refPosToReadPos(refPos) != -1;
	}

	public byte getBaseAtReadPos(int readPos) {
		if (!initialized)
			initialize();
		
		return readBases[readPos];
	}

	/**
	 * Return index of base in this read that maps to the given reference position
	 * @param refPos
	 * @return
	 */
	public int refPosToReadPos(int refPos) {

		int dif = refPos - readAlignmentStart;
		if (dif < 0 || dif >= readBasesCount) {
			return -1;
		}
		return refToReadMap[dif];

	}

	public byte getBaseAtReferencePos(int refPos) {
		if (!initialized)
			initialize();
		
		int pos = refPosToReadPos(refPos);
		if (pos < 0 || pos >= read.getReadLength()) {
			return -1;
		}
		return readBases[ pos ];
	}
	
	public byte getQualityAtReferencePos(int refPos) {
		if (!initialized)
			initialize();
		
		int pos = refPosToReadPos(refPos);
		if (pos < 0 || pos >= read.getReadLength()) {
			return 0;
		}
		return read.getBaseQualities()[ pos ];
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
            refToReadMap = defaultMap;
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
					
				}
				
				
				
			}
		}
		
		initialized = true;
	}
	
}
