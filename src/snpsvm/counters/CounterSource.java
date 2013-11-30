package snpsvm.counters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CounterSource {

	private static List<Integer> exclusions = null;
	private static boolean countersObtained = false;
	
	public static void excludeCounter(int col) {
		if (exclusions == null)
			exclusions = new ArrayList<Integer>();
		if (countersObtained) {
			throw new IllegalArgumentException("Counters already computed, can't modify exclusions now");
		}
		exclusions.add(col);
	}
	/**
	 * Obtain a list of all column computers in use. This must return new instances of counters
	 * on each call because counters are not in general thread safe 
	 *  
	 * @return
	 */
	public static List<ColumnComputer> getCounters() {
		List<ColumnComputer> counters = new ArrayList<ColumnComputer>();
		counters.add( new DuplicateCounter()); 
//		/* 0 */ counters.add( new DepthComputer());  // col 1
//		/* 1 */ counters.add( new BinomProbComputer()); // 2
//		/* 2 */ counters.add( new QualSumComputer()); // 3-4
//		/* 3 */ counters.add( new MeanQualityComputer()); // 5-6
//		/* 4 */ counters.add( new PosDevComputer());  // 7-8
//		/* 5 */ counters.add( new VarFracCounter());  // 9
//		/* 6 */ counters.add( new MQComputer()); // 10-11
//		/* 7 */ counters.add( new DistroProbComputer()); // 12
//		/* 8 */ counters.add( new StrandBiasComputer()); // 13
//		/* 9 */ counters.add( new MismatchComputer()); //14 -15
//		/* 10 */ counters.add( new ReadPosCounter()); // 16 - 17
//		/* 11 */ counters.add( new HomopolymerRunCounter()); //18-19
//		/* 12 */ counters.add( new DinucRepeatCounter()); //20-21
//		/* 13 */ counters.add( new NucDiversityCounter()); //22
//		/* 14 */ counters.add( new MismatchTotal()); //23
//		/* 15 */ counters.add( new OverlappingIndelComputer()); //24
//		/* 16 */ counters.add( new TsTvComputer()); //25
		// /* 17 */ counters.add( new MutClassCounter() ); // 26
		// /* 17 */ counters.add( new TGPCounter() ); 
		countersObtained = true;
		if (exclusions != null) {
			//Sort descending
			Collections.sort(exclusions, Collections.reverseOrder());
			for(int which : exclusions) {
				ColumnComputer removed = counters.remove(which);
				System.err.println("Removing counter " + removed.getName());
			}
		}
		return counters;
	}
}
