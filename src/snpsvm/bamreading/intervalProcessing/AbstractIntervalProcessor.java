package snpsvm.bamreading.intervalProcessing;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

import snpsvm.bamreading.CallingOptions;
import snpsvm.bamreading.SNPCaller;
import snpsvm.counters.CounterSource;

public abstract class AbstractIntervalProcessor<T> {

	//Interval sets smaller than this size will be computed forthwith, otherwise
	//they'll be split into approximate halves and each side will be processed separately
	private long thresholdExtent = 1000000; 
	
	protected ThreadPoolExecutor pool;
	protected IntervalList intervals;
	protected IntervalSplitter splitter = new IntervalBisector();
	protected CallingOptions options;
	private List<IntervalCaller<T>> callers = new ArrayList<IntervalCaller<T>>();
	
	public AbstractIntervalProcessor(ThreadPoolExecutor pool, 
						IntervalList intervals, 
						CallingOptions ops) {
		this.pool = pool;
		this.intervals = intervals;
		this.options = ops;
	}
	
	/**
	 * Begin actual processing for single subset of intervals
	 * @param intervals
	 * @throws Exception
	 */
	protected abstract IntervalCaller<T> getIntervalCaller(IntervalList intervals) throws Exception;

	
	
	/**
	 * Recursive function that will call all snps in the interval list. If the interval list is
	 * small enough then the snps just be submitted to the thread pool, if not the intervals will be split
	 * in half and each half will be submitted 
	 * @param intervals
	 */
	public void submitAll(IntervalList intervals) {
		
		//adjust threshold to be bigger if we're dealing with genome-sized files,
		//elsewise we may blow the stack
		if (intervals.getExtent() > 1e8) {
			thresholdExtent = (long) 1e7;
		}
		if (intervals.getExtent() > 1e9) {
			thresholdExtent = (long) 1e8;
		}
		
		if (intervals.getExtent() < thresholdExtent) {
			//Intervals size is pretty small, just call 'em
			IntervalCaller<T> caller;
			try {
				caller = getIntervalCaller(intervals);
				if (caller != null) {
					callers.add(caller);
					pool.execute(caller);
				}
			} catch (Exception e) { 
				e.printStackTrace();
				System.err.println("Error processing interval, aborting.");
				System.exit(1);
			}
			
		}
		else {	
			//Intervals cover a lot of ground, so split them in half and submit each half
			IntervalList[] intervalArray = splitter.splitIntervals(intervals);
			for(int i=0; i<intervalArray.length; i++) {
				submitAll(intervalArray[i]);
			}
		}
	}
	
	/**
	 * Total number of bases processed so far... not likely to be super accurate since this will
	 * usually get called whilst lots of intervals are asynchronously getting processed, but
	 * might be nice to get a general handle on how much progress has been made so far. 
	 * @return
	 */
	public long getBasesCalled() {
		long tot = 0;
		for(IntervalCaller<T> caller : callers) {
			tot += caller.getBasesCalled();
		}
		return tot;
	}
}
