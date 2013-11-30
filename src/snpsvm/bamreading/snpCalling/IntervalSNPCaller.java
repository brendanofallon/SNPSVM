package snpsvm.bamreading.snpCalling;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

import snpsvm.bamreading.BAMWindowStore;
import snpsvm.bamreading.CallingOptions;
import snpsvm.bamreading.intervalProcessing.AbstractIntervalProcessor;
import snpsvm.bamreading.intervalProcessing.IntervalCaller;
import snpsvm.bamreading.intervalProcessing.IntervalList;
import snpsvm.bamreading.variant.Variant;
import snpsvm.counters.CounterSource;

/**
 * A type of interval processor that calls snps for each interval processed
 * @author brendanofallon
 *
 */
public class IntervalSNPCaller extends AbstractIntervalProcessor<List<Variant>> {

	private File reference;
	private File model;
	protected BAMWindowStore bamWindows;
	
	public IntervalSNPCaller(ThreadPoolExecutor pool, 
								CallingOptions ops,
								File referenceFile,
								File modelFile,
								BAMWindowStore bamWindows) {
		super(pool, ops);
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

	public List<Variant> getResult() {
		super.waitForCompletion();
		List<Variant> vars = new ArrayList<Variant>();
		for(IntervalCaller<List<Variant>> caller : callers) {
			List<Variant> subVars = caller.getResult();
			if (subVars != null) {
				vars.addAll( subVars );
			}
			
		}
		return vars;
	}
}
