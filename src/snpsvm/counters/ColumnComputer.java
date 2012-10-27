package snpsvm.counters;

import snpsvm.bamreading.AlignmentColumn;
import snpsvm.bamreading.FastaWindow;

public interface ColumnComputer {

	/**
	 * A user-friendly name for this computer
	 * @return
	 */
	public String getName();
	
	/**
	 * The number of statistics or 'columns' produced by this computer
	 * @return
	 */
	public int getColumnCount();
	
	
	/**
	 * User-friendly description of the exact column computed by this computer
	 * @return
	 */
	public String getColumnDesc(int which);
	
	/**
	 * Compute the values for this position
	 * @param col
	 * @return
	 */
	public double[] computeValue(final char refBase, FastaWindow window, AlignmentColumn col);
	
}
