package snpsvm.counters;

import snpsvm.bamreading.AlignmentColumn;
import snpsvm.bamreading.FastaWindow;

public class ContextComputer implements ColumnComputer {

	final int RANGE = 5; //number of bases in either direction to record
	private final double[] values = new double[4*(2*RANGE+1)];
	@Override
	public String getName() {
		return "nuc.context.computer";
	}
	
	@Override
	public double[] computeValue(char refBase, FastaWindow window,
			AlignmentColumn col) {

		for(int i=0; i<values.length; i++) {
			values[i] = 0.0;
		}
		
		int refPos = col.getCurrentPosition();
		
		int offset = 0;
		for(int pos = refPos - RANGE; pos <= (refPos+RANGE); pos++) {
			char base = window.getBaseAt(pos);
		
			if (base != 'N') {
				int index = toInt(base);
				values[offset+index] = 1.0;
			}
			offset += 4;
		}
		
		return values;
	}
	
	private static int toInt(char c) {
		switch(c) {
			case 'A': return 0;
			case 'C': return 1;
			case 'T': return 2;
			case 'G': return 3;
		}
		
		throw new IllegalArgumentException("Unknown base : " + c);
	}
	
	@Override
	public int getColumnCount() {
		return values.length;
	}


	@Override
	public String getColumnDesc(int which) {
		return "Presence of base at site";
	}
	
}
