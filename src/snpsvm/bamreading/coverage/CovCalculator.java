package snpsvm.bamreading.coverage;

import java.util.ArrayList;
import java.util.List;

import snpsvm.bamreading.BAMWindowStore;
import snpsvm.bamreading.BamWindow;
import snpsvm.bamreading.intervalProcessing.IntervalCaller;
import snpsvm.bamreading.intervalProcessing.IntervalList;
import snpsvm.bamreading.intervalProcessing.IntervalList.Interval;

public class CovCalculator implements IntervalCaller<List<IntervalCoverage>> {

	public static final int STEP_SIZE = 2; //We only actually look at every STEP_SIZE bases 
	
	private BAMWindowStore bamWindowStore;
	private IntervalList intervals;
	private long basesComputed = 0;
	private boolean running = false;
	private boolean completed = false;
	private List<IntervalCoverage> allResults;
	
	public CovCalculator(BAMWindowStore bamWindows, IntervalList intervals) {
		this.bamWindowStore = bamWindows;
		this.intervals = intervals;
	}
	
	@Override
	public void run() {
		running = true;
		BamWindow window = bamWindowStore.getWindow();
		allResults = new ArrayList<IntervalCoverage>(intervals.getIntervalCount());
		for(String contig : intervals.getContigs()) {
			for(Interval interval : intervals.getIntervalsInContig(contig)) {
				IntervalCoverage coverageResult = computeCoverage(window, contig, interval);
				allResults.add(coverageResult);
				System.out.flush();
				System.out.println(interval + " : " + coverageResult.basesActuallyExamined + " mean: " + (double)coverageResult.coverageSum / (double)coverageResult.basesActuallyExamined);
				basesComputed += interval.getSize();
			}
		}
		
		bamWindowStore.returnToStore(window);
		running = false;
		completed = true;
	}
	
	//Combine results from two intervals into a single result
//	private IntervalCoverage mergeResults(IntervalCoverage covA, IntervalCoverage covB) {
//		IntervalCoverage result = new IntervalCoverage();
//		result.basesActuallyExamined = covA.basesActuallyExamined + covB.basesActuallyExamined;
//		result.coverageSum = covA.coverageSum + covB.coverageSum;
//		//TODO: Merge results for bases covered by > X reads
//		
//		return result;
//	}
	
	private static IntervalCoverage computeCoverage(BamWindow window, String contig, Interval interval) {
		if (! window.containsContig(contig)) {
			throw new IllegalArgumentException("Contig " + contig + " could not be found in the bam file.");
		}
		IntervalCoverage result = new IntervalCoverage();	
		result.interval = interval;
		result.contig = contig;
		window.advanceTo(contig, interval.getFirstPos());
		while(window.getCurrentPosition() < interval.getLastPos()) {
			int size = window.size();
			result.basesActuallyExamined++;
			result.coverageSum += size;
			window.advanceBy(STEP_SIZE);
		}
		
		return result;
	}

	@Override
	public long getBasesCalled() {
		return basesComputed;
	}

	@Override
	public List<IntervalCoverage> getResult() {
		return allResults;
	}

	@Override
	public boolean isResultReady() {
		return completed;
	}

}
