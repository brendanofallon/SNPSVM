package snpsvm.bamreading;

public interface HasBaseProgress {

	/**
	 * Returns number of bases examined
	 * @return
	 */
	public long getBasesCalled();
	
}
