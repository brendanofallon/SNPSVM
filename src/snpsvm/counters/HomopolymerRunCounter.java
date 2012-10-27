package snpsvm.counters;

import snpsvm.bamreading.AlignmentColumn;
import snpsvm.bamreading.FastaWindow;

/**
 * Computes longest homopolymer run in both directions 
 * @author brendan
 *
 */
public class HomopolymerRunCounter implements ColumnComputer {

	final double[] values = new double[2];
	
	final int maxLength = 10; //dont look beyond this many bases in either direction
	
	@Override
	public String getName() {
		return "hrun.counter";
	}

	@Override
	public int getColumnCount() {
		return values.length;
	}


	@Override
	public String getColumnDesc(int which) {
		if (which == 0)
			return "Length of homopolymer run to left of site";
		else
			return "Length of homopolymer run to right of site";
	}
	
	@Override
	public double[] computeValue(char refBase, FastaWindow window,
			AlignmentColumn col) {
		
		
		//Looking backward
		int refPos = col.getCurrentPosition();
		char base = window.getBaseAt(refPos-1);
		double count = 0;
		for(int i=refPos-2; i>Math.max(window.indexOfLeftEdge(), refPos-1-maxLength); i--) {
			if (base == window.getBaseAt(i))
				count++;
			else
				break;
		}
		values[0] = count;
		
		
		base = window.getBaseAt(refPos+1);
		count = 0;
		for(int i=refPos+2; i<Math.min(window.indexOfRightEdge()-1, refPos+1+maxLength); i++) {
                    if (i < window.indexOfRightEdge()) {
                        //System.out.println("requested pos : " + i + " right edge : " + window.indexOfRightEdge());
			if (base == window.getBaseAt(i))
				count++;
			else
				break;
                    }
                    else
                        break;
		}
		values[1] = count;
		
		return values;
	}

}
