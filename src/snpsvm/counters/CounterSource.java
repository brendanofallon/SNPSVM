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
		/* 0 */ counters.add( new DepthComputer());  // col 1
		/* 1 */ counters.add( new BinomProbComputer()); // 2
		/* 2 */ counters.add( new QualSumComputer()); // 3-4
		/* 3 */ counters.add( new MeanQualityComputer()); // 5-6
		/* 4 */ counters.add( new PosDevComputer());  // 7-8
		/* 5 */ counters.add( new VarFracCounter());  // 9
		/* 6 */ counters.add( new MQComputer()); // 10-11
		/* 7 */ counters.add( new DistroProbComputer()); // 12
		/* 8  counters.add( new NearbyQualComputer()); //13 -15 */
		/* 9 */ counters.add( new StrandBiasComputer()); // 16
		/* 10 */ counters.add( new MismatchComputer()); //17 -18
		/* 11 */ counters.add( new ReadPosCounter()); // 19 - 20
		/* 12 */ counters.add( new HomopolymerRunCounter()); //21-22
		/* 13 */ counters.add( new DinucRepeatCounter()); //23-24
		/* 14 */ counters.add( new NucDiversityCounter()); //25
		/* 15 */ counters.add( new MismatchTotal()); //26
		/* 16 */ counters.add( new OverlappingIndelComputer()); //27
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
