package snpsvm.bamreading.coverage;

import snpsvm.bamreading.intervalProcessing.IntervalList.Interval;

public class IntervalCoverage implements Comparable<IntervalCoverage> {

	public String contig; 
	public Interval interval;
	public int basesActuallyExamined; //Actual number of bases examined
	public long coverageSum;
	public int[] coverageAboveCutoff; //Absolute number of positions examined covered by more than coverageCutoff[i] reads
	public int[] coverageCutoff;
	
	@Override
	public int compareTo(IntervalCoverage o) {
		if (contig.equals(o.contig)) {
			return interval.getFirstPos() - o.interval.getFirstPos();
		}
		else {
			return contig.compareTo(o.contig);
		}
	}
	
	
}
