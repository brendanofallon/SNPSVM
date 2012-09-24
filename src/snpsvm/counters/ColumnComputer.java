package snpsvm.counters;

import snpsvm.bamreading.AlignmentColumn;

public interface ColumnComputer {

	/**
	 * A user-friendly name for this computer
	 * @return
	 */
	public String getName();
	
	/**
	 * Compute the values for this position
	 * @param col
	 * @return
	 */
	public Double[] computeValue(char refBase, AlignmentColumn col);
	
}
