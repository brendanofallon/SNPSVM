package snpsvm.counters;

import snpsvm.bamreading.AlignmentColumn;
import snpsvm.bamreading.FastaWindow;

public class DinucRepeatCounter implements ColumnComputer {

final Double[] values = new Double[2];
	
	final int maxLength = 10; //dont look beyond this many bases in either direction
	
	@Override
	public String getName() {
		return "dinuc.counter";
	}

	@Override
	public Double[] computeValue(char refBase, FastaWindow window,
			AlignmentColumn col) {
		
		
		//Looking backward
		int refPos = col.getCurrentPosition();
		char base0 = window.getBaseAt(refPos-1);
		char base1 = window.getBaseAt(refPos-2);
		double count = 0;
		if (base0 != base1) {
			for(int i=refPos-3; (i-1)>Math.max(window.indexOfLeftEdge(), refPos-1-maxLength); i-=2) {
				if (base0 == window.getBaseAt(i) && base1 == window.getBaseAt(i-1))
					count++;
				else
					break;
			}
		}
		values[0] = count;
		
		//Looking forward
		base0 = window.getBaseAt(refPos+1);
		base1 = window.getBaseAt(refPos+2);
		
		count = 0;
		if (base0 != base1) {
			for(int i=refPos+3; (i+1)<Math.min(window.indexOfRightEdge(), refPos+1+maxLength); i+=2) {
				if (base0 == window.getBaseAt(i) && base1 == window.getBaseAt(i+1))
					count++;
				else
					break;
			}
		}
		values[1] = count;
		
		return values;
	}
}
