package snpsvm.counters;

import java.util.ArrayList;
import java.util.List;

public class CounterSource {

	/**
	 * Obtain a list of all column computers in use. This must return new instances of counters
	 * on each call because counters are not in general thread safe 
	 *  
	 * @return
	 */
	public static List<ColumnComputer> getCounters() {
		List<ColumnComputer> counters = new ArrayList<ColumnComputer>();
		counters.add( new DepthComputer());  // col 1
		counters.add( new BinomProbComputer()); // 2
		counters.add( new QualSumComputer()); // 3-4
		counters.add( new MeanQualityComputer()); // 5-6
		counters.add( new PosDevComputer());  // 7-8
		counters.add( new VarFracCounter());  // 9
		counters.add( new MQComputer()); // 10-11
		counters.add( new DistroProbComputer()); // 12
		counters.add( new NearbyQualComputer()); //13 -15
		counters.add( new StrandBiasComputer()); // 16
		counters.add( new MismatchComputer()); //17 -18
		counters.add( new ReadPosCounter()); // 19 - 20
		counters.add( new HomopolymerRunCounter()); //21-22
		counters.add( new DinucRepeatCounter()); //23-24
		counters.add( new NucDiversityCounter()); //25
		counters.add( new MismatchTotal()); //26
		
		return counters;
	}
}
