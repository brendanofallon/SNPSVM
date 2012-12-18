package snpsvm.counters;

import snpsvm.bamreading.AlignmentColumn;
import snpsvm.bamreading.FastaWindow;

/**
 * Computes total read depth at the current position
 * @author brendan
 *
 */
public class DepthComputer implements ColumnComputer {

	private final double[] value = new double[1];
	
	@Override
	public String getName() {
		return "total.depth";
	}
	
	@Override
	public double[] computeValue(final char refBase, FastaWindow window, AlignmentColumn col) {
		value[0] = (double)col.getDepth();
		value[0] = Math.min(2000, value[0]); //Cap at 2000, everything above this is treated equally
		value[0] = Math.log(value[0]);
		value[0] = value[0]/7.61*2.0 - 1.0; //Scale to between -1 and 1
		return value;
	}

	@Override
	public int getColumnCount() {
		return value.length;
	}


	@Override
	public String getColumnDesc(int which) {
		return "Total read depth at site";
	}
}
