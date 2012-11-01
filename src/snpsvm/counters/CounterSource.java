package snpsvm.counters;

import java.util.ArrayList;
import java.util.List;

public class CounterSource {

	/**
	 * Obtain a list of all column computers in use
	 * @return
	 */
	public static List<ColumnComputer> getCounters() {
		List<ColumnComputer> counters = new ArrayList<ColumnComputer>();
		counters.add( new DepthComputer());
		counters.add( new BinomProbComputer());
		counters.add( new QualSumComputer());
		counters.add( new MeanQualityComputer());
		counters.add( new PosDevComputer());
		counters.add( new MQComputer());
		counters.add( new DistroProbComputer());
		counters.add( new NearbyQualComputer());
		counters.add( new StrandBiasComputer());
		counters.add( new MismatchComputer());
		counters.add( new ReadPosCounter());
		counters.add( new HomopolymerRunCounter());
		counters.add( new DinucRepeatCounter());
		counters.add( new NucDiversityCounter());
		return counters;
	}
}
