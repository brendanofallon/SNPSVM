package snpsvm.bamreading.snpCalling;

import java.io.File;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

import snpsvm.bamreading.BAMWindowStore;
import snpsvm.bamreading.CallingOptions;
import snpsvm.bamreading.SNPCaller;
import snpsvm.bamreading.Variant;
import snpsvm.bamreading.intervalProcessing.AbstractIntervalProcessor;
import snpsvm.bamreading.intervalProcessing.IntervalCaller;
import snpsvm.bamreading.intervalProcessing.IntervalList;
import snpsvm.counters.CounterSource;

public class IntervalSNPCaller extends AbstractIntervalProcessor<List<Variant>> {

	private File reference;
	private File model;
	protected BAMWindowStore bamWindows;
	
	public IntervalSNPCaller(ThreadPoolExecutor pool, 
								IntervalList intervals,
								CallingOptions ops,
								File referenceFile,
								File modelFile,
								BAMWindowStore bamWindows) {
		super(pool, intervals, ops);
		this.reference = referenceFile;
		this.model= modelFile;
		this.bamWindows = bamWindows;
	}

	@Override
	protected IntervalCaller<List<Variant>> getIntervalCaller(IntervalList intervals)
			throws Exception {
		
		return new SNPCaller(reference,
				model, 
				intervals,
				CounterSource.getCounters(), 
				bamWindows,
				options);
	}

}
