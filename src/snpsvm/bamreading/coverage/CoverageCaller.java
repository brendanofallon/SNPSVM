package snpsvm.bamreading.coverage;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

import snpsvm.bamreading.BAMWindowStore;
import snpsvm.bamreading.CallingOptions;
import snpsvm.bamreading.intervalProcessing.AbstractIntervalProcessor;
import snpsvm.bamreading.intervalProcessing.IntervalCaller;
import snpsvm.bamreading.intervalProcessing.IntervalList;

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
		return new CovCalculator(windows, intervals);
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
