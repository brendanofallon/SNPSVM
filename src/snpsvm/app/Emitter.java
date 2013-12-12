package snpsvm.app;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import snpsvm.app.AbstractModule.MissingArgumentException;
import snpsvm.bamreading.BamWindow;
import snpsvm.bamreading.CallingOptions;
import snpsvm.bamreading.FastaIndex.IndexNotFoundException;
import snpsvm.bamreading.ReferenceBAMEmitter;
import snpsvm.bamreading.intervalProcessing.IntervalList;
import snpsvm.bamreading.intervalProcessing.IntervalList.Interval;
import snpsvm.counters.ColumnComputer;
import snpsvm.counters.CounterSource;

public class Emitter extends AbstractModule {

	@Override
	public void emitUsage() {
		System.out.println(" Emitter: emit training data to terminal (for debugging only) ");
		System.out.println(" -R : Reference File ");
		System.out.println(" -B : BAM File ");
		System.out.println(" -L : intervals ");
	}

	@Override
	public boolean matchesModuleName(String name) {
		return name.startsWith("write");
	}

	@Override
	public void performOperation(String name, ArgParser args) {
		String referencePath = null;
		String inputBAMPath = null;
		try {
			referencePath = getRequiredStringArg(args, "-R", "Missing required argument for reference file, use -R");
			inputBAMPath = getRequiredStringArg(args, "-B", "Missing required argument for input BAM file, use -B");
		} catch (MissingArgumentException e1) {
			System.err.println(e1.getMessage());
			return;
		}
		
		IntervalList intervals = getIntervals(args);
		
		File referenceFile = new File(referencePath);
		File inputBAM = new File(inputBAMPath);
		List<ColumnComputer> counters = CounterSource.getCounters();
		BamWindow window = new BamWindow(inputBAM);
		CallingOptions ops = new CallingOptions();
		
		try {
			ReferenceBAMEmitter emitter = new ReferenceBAMEmitter(referenceFile, counters, window, ops);
			BufferedWriter posWriter = new BufferedWriter(new FileWriter("emitter.positions.txt"));
			emitter.setPositionsWriter( posWriter );
			for(String contig : intervals.getContigs()) {
				for(Interval inter : intervals.getIntervalsInContig(contig)) {
					emitter.emitWindow(contig, inter.getFirstPos(), inter.getLastPos(), System.out);	
				}
			}
			
			posWriter.close();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IndexNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

}
