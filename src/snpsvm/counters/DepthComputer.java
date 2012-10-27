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
