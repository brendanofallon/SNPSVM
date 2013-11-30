package snpsvm.bamreading.intervalProcessing;

import snpsvm.bamreading.HasBaseProgress;

public interface IntervalCaller<T> extends Runnable, HasBaseProgress {

	public T getResult();
	
	public boolean isResultReady();
	
}
