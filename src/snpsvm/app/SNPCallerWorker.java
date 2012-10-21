package snpsvm.app;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.util.List;

import javax.swing.SwingWorker;

import libsvm.LIBSVMModel;
import libsvm.LIBSVMPredictor;
import libsvm.LIBSVMResult;

import snpsvm.bamreading.IntervalList;
import snpsvm.bamreading.ReferenceBAMEmitter;
import snpsvm.bamreading.ResultEmitter;
import snpsvm.bamreading.IntervalList.Interval;
import snpsvm.bamreading.ResultVariantConverter;
import snpsvm.bamreading.Variant;
import snpsvm.counters.ColumnComputer;

/**
 * Reads and emits information from an input BAMWindow, then uses a model to call
 * SNPs. 
 * @author brendanofallon
 *
 */
public class SNPCallerWorker implements Runnable {

	private final File referenceFile;
	private final File bamFile;
	private final File modelFile;
	private final IntervalList intervals;
	private List<ColumnComputer> counters;
	private List<Variant> variants = null;
	
	public SNPCallerWorker(File referenceFile, File bamFile, File modelFile, IntervalList intervals, List<ColumnComputer> counters) {
		this.referenceFile = referenceFile;
		this.bamFile = bamFile;
		this.intervals = intervals;
		this.modelFile = modelFile;
		this.counters = counters;
	}
	
	
	@Override
	public void run()  {
		try {
		ReferenceBAMEmitter emitter = new ReferenceBAMEmitter(referenceFile, bamFile, counters);
		
		//Store intermediate results in a temporary file
		String tmpDataPrefix =  "." + generateRandomString(12);
		
		File data = new File(tmpDataPrefix + ".data");
		File positionsFile = new File(tmpDataPrefix + ".pos");
		emitter.setPositionsFile(positionsFile);

		//Read BAM file, write results to temporary file
		PrintStream dataStream = new PrintStream(new FileOutputStream(data));		

		for(String contig : intervals.getContigs()) {
			System.out.println("Emitting contig :" + contig);
			for(Interval interval : intervals.getIntervalsInContig(contig)) {
				System.out.println("Emitting interval : " + contig + " : " + interval.getFirstPos() + " - " + interval.getLastPos());
				emitter.emitWindow(contig, interval.getFirstPos(), interval.getLastPos(), dataStream);
			}
		}
		dataStream.close();

		System.out.println("Done emitting window...");
		LIBSVMPredictor predictor = new LIBSVMPredictor();
		LIBSVMResult result = predictor.predictData(data, new LIBSVMModel(modelFile));
		result.setPositionsFile(positionsFile);

		ResultVariantConverter converter = new ResultVariantConverter();
		variants = converter.createVariantList(result);
		
		}
		catch (IOException iox) {
			
			iox.printStackTrace();
		}
	}

	/**
	 * A reference to a list containing all called variants in the
	 * @return
	 */
	public List<Variant> getVariantList() {
		return variants;
	}
	
	/**
	 * Returns true if this worker has completed and a non-null variant list is present 
	 * @return
	 */
	public boolean isVariantListCreated() {
		return variants != null;
	}
	
	private static String generateRandomString(int length) {
		StringBuilder strB = new StringBuilder();
		while(strB.length() < length) {
			char c = chars.charAt( (int)(chars.length()*Math.random()) );
			strB.append(c);
		}
		return strB.toString();
	}
	
	private static final String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabscdefhgijklmnopqrstuvwxyz1234567890";
}
