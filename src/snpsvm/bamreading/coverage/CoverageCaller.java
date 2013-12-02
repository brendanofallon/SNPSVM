package snpsvm.bamreading.coverage;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

import snpsvm.bamreading.BAMWindowStore;
import snpsvm.bamreading.BamWindow;
import snpsvm.bamreading.CallingOptions;
import snpsvm.bamreading.intervalProcessing.AbstractIntervalProcessor;
import snpsvm.bamreading.intervalProcessing.IntervalCaller;
import snpsvm.bamreading.intervalProcessing.IntervalList;
import snpsvm.bamreading.intervalProcessing.IntervalList.Interval;

public class CoverageCaller extends AbstractIntervalProcessor<List<IntervalCoverage>> {

	private BAMWindowStore windows;
	////Represents intervals for which an error occurred, for instance, if the user specified a contig
	//that we couldn't read 
	public final IntervalCoverage ERROR_COVERAGE = new IntervalCoverage(); 
	
	
	public CoverageCaller(ThreadPoolExecutor pool, CallingOptions ops, BAMWindowStore bamWindows) {
		super(pool, ops);
		this.windows = bamWindows;
	}

	@Override
	protected IntervalCaller<List<IntervalCoverage>> getIntervalCaller(IntervalList intervals) throws Exception {
		BamWindow bam = windows.getWindow();
		boolean processIntervals = false;
		for(String contig : intervals.getContigs()) {
			for(Interval interval :intervals.getIntervalsInContig(contig)) {
				if (bam.hasReadsInRegion(contig, interval.getFirstPos(), interval.getLastPos())) {
					processIntervals = true;
					break;
				}
			}
			if (processIntervals) {
				break;
			}
		}
		windows.returnToStore(bam); //don't forget to check it back in
		if (processIntervals) {
			return new CovCalculator(windows, intervals);
		}
		else {
			return null;
		}
	}

	@Override
	public List<IntervalCoverage> getResult() {
		super.waitForCompletion();
		List<IntervalCoverage> everyResult = new ArrayList<IntervalCoverage>(1024);
		for(IntervalCaller<List<IntervalCoverage>> caller : callers) {
			everyResult.addAll( caller.getResult());
		}
		return everyResult;
	}

}
