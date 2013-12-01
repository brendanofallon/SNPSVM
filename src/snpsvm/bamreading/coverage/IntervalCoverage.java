package snpsvm.bamreading.coverage;

import java.util.List;

import snpsvm.bamreading.intervalProcessing.IntervalList.Interval;

public class IntervalCoverage implements Comparable<IntervalCoverage> {


	public static final int[] defaultCutoffs = new int[]{0, 10, 20, 50};
	private static int[] cutoffs = defaultCutoffs;
	
	public String contig; 
	public Interval interval;
	public int basesActuallyExamined; //Actual number of bases examined
	public long coverageSum;
	public int[] coverageAboveCutoff; //Absolute number of positions examined covered by more than coverageCutoff[i] reads
	public int[] coverageCutoffs = cutoffs;
	
	public static void setCutoffs(Integer[] cutoffsToUse) {
		cutoffs = new int[cutoffsToUse.length];
		for(int i=0; i<cutoffsToUse.length; i++) {
			cutoffs[i] = cutoffsToUse[i];
		}
	}
	
	public static int getCutoffCount() {
		return cutoffs.length;
	}
	
	public static int[] getCutoffs() {
		return cutoffs;
	}
	
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
