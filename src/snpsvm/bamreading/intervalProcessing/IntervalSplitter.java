package snpsvm.bamreading.intervalProcessing;


public interface IntervalSplitter {

	/**
	 * Take a given list of intervals and turn them into two smaller lists
	 * @param intervals
	 * @return
	 */
	 public IntervalList[] splitIntervals(IntervalList intervals);
}
