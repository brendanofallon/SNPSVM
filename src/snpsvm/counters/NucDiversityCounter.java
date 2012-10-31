package snpsvm.counters;

import snpsvm.bamreading.AlignmentColumn;
import snpsvm.bamreading.FastaWindow;

/**
 * A simple take on assessing sequence complexity around a focal site, we simply compute the
 * fraction of A,C,T and G and then figure out how much this differs from the expected fraction
 * Low complexity sequences (homo- or dinucleotide runs, etc) will likely be pretty divergent from
 * the genome-wide expectation 
 * @author brendan
 *
 */
public class NucDiversityCounter implements ColumnComputer {

	public final double expA = 0.3;
	public final double expC = 0.2;
	public final double expG = 0.2;
	public final double expT = 0.3;

	private final double[] val = new double[1];
	private double[] counts = new double[4];
	
	public final int WINDOW_SIZE = 10; //Window in either direction, total size will be 2*W+1
	
	@Override
	public String getName() {
		return "nuc.diversity";
	}

	@Override
	public int getColumnCount() {
		return val.length;
	}


	@Override
	public String getColumnDesc(int which) {
		return "Diversity of reference in window surrounding site";
	}
	
	@Override
	public double[] computeValue(char refBase, FastaWindow window,
			AlignmentColumn col) {

		int startPos = Math.max(window.indexOfLeftEdge(), col.getCurrentPosition()-WINDOW_SIZE);
		int endPos = Math.min(window.indexOfRightEdge(), col.getCurrentPosition()+WINDOW_SIZE);
		counts[0] = 0.0;
		counts[1] = 0.0;
		counts[2] = 0.0;
		counts[3] = 0.0;
		
		for(int i=startPos; i<endPos; i++) {
			char base = window.getBaseAt(i);
			switch(base) {
			case 'A' : counts[0]++; break;
			case 'C' : counts[1]++; break;
			case 'T' : counts[2]++; break;
			case 'G' : counts[3]++; break;
			}
		}
		
		
		double tot = counts[0] + counts[1] + counts[2] + counts[3];
		if (tot > 0) {
			counts[0] /= tot;
			counts[1] /= tot;
			counts[2] /= tot;
			counts[3] /= tot;
		}
		val[0] = (counts[0]-expA)*(counts[0]-expA)
					+ (counts[1]-expC)*(counts[1]-expC)
					+ (counts[2]-expT)*(counts[2]-expT)
					+ (counts[3]-expG)*(counts[3]-expG);
		
		
		val[0] = val[0]*2.0 - 1.0;
		
		return val;
	}

}
